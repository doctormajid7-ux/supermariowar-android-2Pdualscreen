package org.supermariowar.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Host-side behavioral tests: run ./test-touch.sh (no emulator or Android runtime). */
public final class TouchControlsTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Recorder implements TouchInput.Sink {
        final List<String> events = new ArrayList<>();
        int keys;
        boolean allowRepeat;
        public void keyChanged(int key, boolean down) {
            if (!allowRepeat) check(((keys & key) != 0) != down, "Unbalanced or duplicate key event");
            keys = down ? keys | key : keys & ~key;
            events.add(key + (down ? "+" : "-"));
        }
        void expect(String... expected) {
            check(events.equals(Arrays.asList(expected)), "Expected " + Arrays.toString(expected) + ", got " + events);
            events.clear();
        }
    }

    public static void main(String[] args) {
        Recorder sink = new Recorder();
        TouchInput input = new TouchInput(sink);
        // Move, run and jump can be held by three independently numbered fingers.
        input.move(17, TouchInput.RIGHT);
        input.move(42, TouchInput.ACTION | TouchInput.RANDOM);
        input.move(9, TouchInput.UP);
        sink.expect("2+", "16+", "256+", "4+");
        input.move(17, TouchInput.RIGHT);
        input.move(9, TouchInput.UP);
        sink.expect(); // MOVE must not repeat presses.
        input.release(42);
        sink.expect("16-", "256-");
        check(input.held() == (TouchInput.RIGHT | TouchInput.UP), "Lifting action retains movement and jump");
        input.releaseAll();
        sink.expect("2-", "4-");
        check(sink.keys == 0, "Cancel/pause releases everything");
        input.releaseAll();
        sink.expect();

        // Jump button and the up direction are two owners of the same SDL key.
        input.move(3, TouchInput.UP);
        input.move(71, TouchInput.UP);
        sink.expect("4+");
        input.release(3);
        sink.expect();
        check(input.held() == TouchInput.UP, "Second jump finger retains key");
        input.release(71);
        sink.expect("4-");

        // Sliding changes direction, allows diagonals and releases on leaving a control.
        input.move(5, TouchInput.LEFT);
        input.move(5, TouchInput.RIGHT | TouchInput.DOWN);
        sink.expect("1+", "1-", "2+", "8+");
        input.move(5, 0);
        sink.expect("2-", "8-");
        input.release(5);
        sink.expect();

        // Two fingers transfer ownership in the same MotionEvent: no artificial re-press.
        input.move(11, TouchInput.UP);
        sink.expect("4+");
        input.beginBatch();
        input.move(11, 0);
        input.move(64, TouchInput.UP);
        input.endBatch();
        sink.expect();
        input.releaseAll();
        sink.expect("4-");

        // Tap within one frame still produces both edges; stale pointer releases are harmless.
        input.move(99, TouchInput.CONFIRM);
        input.release(99);
        sink.expect("64+", "64-");
        input.release(99);
        sink.expect();
        input.move(8, TouchInput.BACK);
        input.releaseAll();
        sink.expect("128+", "128-");

        // Focus loss/pause releases keys and ignores late MOVE events until re-enabled.
        input.move(25, TouchInput.LEFT | TouchInput.ACTION);
        sink.expect("1+", "16+");
        input.setEnabled(false);
        sink.expect("1-", "16-");
        input.move(25, TouchInput.LEFT);
        input.move(61, TouchInput.UP);
        sink.expect();
        check(input.held() == 0, "No keys reappear while unfocused");
        input.setEnabled(true);
        sink.expect();
        input.move(61, TouchInput.UP);
        input.releaseAll();
        sink.expect("4+", "4-");

        // Phone/tablet sizes, density, and cutout/system-bar offsets.
        float[][] screens = {{640,360,1}, {800,360,1}, {2400,1080,3}, {2560,1600,2}, {1280,720,2}, {480,320,1}};
        for (float[] screen : screens) {
            TouchLayout l = new TouchLayout(24, 12, screen[0]-16, screen[1]-8, screen[2]);
            check(l.hit(l.jump.x, l.jump.y) == TouchInput.UP, "Jump target");
            check(l.hit(l.action.x, l.action.y) == (TouchInput.ACTION | TouchInput.RANDOM), "Action and menu random target");
            check(l.hit(l.item.x, l.item.y) == (TouchInput.ITEM | TouchInput.FAST), "Item and menu fast target");
            check(l.hit(l.backX+l.topWidth/2, l.topY+l.topHeight/2) == TouchInput.BACK, "Back target");
            check(l.hit(l.confirmX+l.topWidth/2, l.topY+l.topHeight/2) == TouchInput.CONFIRM, "Confirm/pause target");
            check(l.hit(l.padX, l.padY) == 0, "D-pad neutral zone");
            check(l.hit(l.padX-56*l.unit, l.padY) == TouchInput.LEFT, "D-pad left");
            check(l.hit(l.padX+56*l.unit, l.padY) == TouchInput.RIGHT, "D-pad right");
            check(l.hit(l.padX, l.padY-56*l.unit) == TouchInput.UP, "D-pad up");
            check(l.hit(l.padX, l.padY+56*l.unit) == TouchInput.DOWN, "D-pad down");
            check(l.hit(l.padX+50*l.unit, l.padY+50*l.unit) == (TouchInput.RIGHT|TouchInput.DOWN), "D-pad diagonal");
            check(l.hit(screen[0]/2, screen[1]/2) == 0, "Centre does not activate controls");
            check(l.padX-l.padRadius >= 24 && l.padY+l.padRadius <= screen[1]-8, "D-pad respects safe bounds");
            check(l.jump.x+l.jump.radius <= screen[0]-16, "Jump respects safe bounds");
            check(l.jump.y+l.jump.radius <= screen[1]-8, "Jump bottom bound");
            check(Math.hypot(l.jump.x-l.action.x,l.jump.y-l.action.y) > l.jump.radius+l.action.radius, "Action and jump do not overlap");
            check(Math.hypot(l.item.x-l.action.x,l.item.y-l.action.y) > l.item.radius+l.action.radius, "Item and action do not overlap");
        }
        testGameplayPad();
        testTwoPlayerContacts();
        testSharedKeys();
        System.out.println("Touch controls: " + checks + " checks passed");
    }

    private static void testGameplayPad() {
        for (float scale : new float[] {1, 1.25f}) {
            TouchLayout l = new TouchLayout(24, 12, 1056, 1180, 3, scale);
            float offset = 56 * l.unit * scale;
            check(l.hit(l.padX, l.padY-offset, scale, false) == 0, "Gameplay up target is inactive");
            check(l.hit(l.padX+offset, l.padY-offset, scale, false) == TouchInput.RIGHT, "Upper diagonal keeps right without jump");
            check(l.hit(l.padX-offset, l.padY-offset, scale, false) == TouchInput.LEFT, "Upper diagonal keeps left without jump");
            check(l.hit(l.jump.x, l.jump.y, scale, false) == TouchInput.UP, "Dedicated jump stays active in game");
            check(l.hit(l.padX, l.padY-offset, scale, true) == TouchInput.UP, "Menu up stays active");
            check(l.hit(l.padX, l.padY+offset, scale, false) == TouchInput.DOWN, "Gameplay down stays active");
        }
    }

    private static final class TwoPlayers {
        final Recorder output = new Recorder();
        final TouchKeys keys = new TouchKeys((player, key) ->
                player == 0 || key == TouchInput.RANDOM || key == TouchInput.FAST ? key : key << 10, output);
        final TouchInput first = new TouchInput((key, down) -> keys.change(0, key, down));
        final TouchInput second = new TouchInput((key, down) -> keys.change(1, key, down));
        final TouchRouter router = new TouchRouter(first, second);
        final TouchLayout bottom = new TouchLayout(24, 1220, 1056, 2370, 3, 1.25f);
        final TouchLayout top = new TouchLayout(24, 20, 1056, 1220, 3, 1.25f);
        TwoPlayers() {
            router.configure(bottom, top, 1.25f);
            router.setAllowPadUp(false);
        }
        float[] point(int player, int control) {
            TouchLayout l = player == 0 ? bottom : top;
            float x, y;
            switch (control) {
                case 0: x=l.padX+56*l.unit*1.25f; y=l.padY; break;
                case 1: x=l.jump.x; y=l.jump.y; break;
                case 2: x=l.action.x; y=l.action.y; break;
                case 3: x=l.item.x; y=l.item.y; break;
                default: x=l.padX; y=l.padY-56*l.unit*1.25f; break;
            }
            return player == 0 ? new float[] {x,y} : new float[] {l.left+l.right-x,l.top+l.bottom-y};
        }
        void down(int id, int player, int control) {
            float[] p=point(player, control);
            router.down(id,p[0],p[1]);
        }
        void begin() { keys.beginBatch(); router.beginBatch(); }
        void end() { router.endBatch(); keys.endBatch(); }
    }

    private static void testTwoPlayerContacts() {
        int[] ids = {3, 27, 8, 22, 17, 2, 31, 14};
        for (int count : new int[] {4,6,8}) {
            TwoPlayers t = new TwoPlayers();
            // Contacts alternate between players; pointer IDs are not array indices.
            for (int i=0;i<count;i++) t.down(ids[i],i%2,i/2);
            int expected = TouchInput.RIGHT | TouchInput.UP;
            if (count>=6) expected |= TouchInput.ACTION | TouchInput.RANDOM;
            if (count>=8) expected |= TouchInput.ITEM | TouchInput.FAST;
            check(t.first.held()==expected && t.second.held()==expected, count+" contacts independently control both players");
            int events=t.output.events.size();
            for (int frame=0;frame<100;frame++) {
                t.begin();
                // Android can reorder indices; movement follows IDs in any order.
                for (int i=count-1;i>=0;i--) {
                    float[] p=t.point(i%2,i/2);
                    t.router.move(ids[i],p[0],p[1]);
                }
                t.end();
            }
            check(t.output.events.size()==events, "Repeated multi-contact moves never repeat key-down");
            // Lift P1's fingers first; P2 must keep all keys, including shared keys.
            for (int i=0;i<count;i+=2) t.router.up(ids[i]);
            check(t.first.held()==0 && t.second.held()==expected, "Lifting P1 retains all P2 controls");
            if (count>=6) check((t.output.keys & TouchInput.RANDOM)!=0, "Shared random retained by P2");
            if (count>=8) check((t.output.keys & TouchInput.FAST)!=0, "Shared fast retained by P2");
            for (int i=count-1;i>=0;i-=2) t.router.up(ids[i]);
            check(t.output.keys==0, "Arbitrary lift order releases all physical keys");

            // All contacts sharing jump: only the last contact per player releases it.
            for (int i=0;i<count;i++) t.down(ids[i],i%2,1);
            for (int i=0;i<count-2;i++) t.router.up(ids[i]);
            check(t.first.held()==TouchInput.UP && t.second.held()==TouchInput.UP, "Shared jump survives partial release");
            t.router.releaseAll();
            check(t.output.keys==0, "CANCEL clears every contact");
            for (int i=0;i<count;i++) {
                float[] p=t.point(i%2,1);
                t.router.move(ids[i],p[0],p[1]);
            }
            check(t.output.keys==0, "Stale MOVE after cancellation cannot reactivate keys");

            for (int i=0;i<count;i++) t.down(ids[i],i%2,i/2);
            t.router.setEnabled(false);
            check(t.output.keys==0, "Focus loss releases both players with many contacts");
            t.down(19,0,1);
            check(t.output.keys==0, "Focus loss ignores new contacts");
            t.router.setEnabled(true);
            t.down(19,1,1);
            check(t.second.held()==TouchInput.UP, "Fresh contact works after focus returns");
            t.router.configure(t.bottom,t.top,1.25f);
            check(t.output.keys==0, "Layout change releases all contacts");
        }

        TwoPlayers t=new TwoPlayers();
        for (int player=0;player<2;player++) {
            t.down(player,player,4);
            check(t.first.held()==0 && t.second.held()==0, "Up is inactive on both rotated gameplay pads");
        }
        t.router.setAllowPadUp(true);
        t.down(7,0,4); t.down(23,1,4);
        check(t.first.held()==TouchInput.UP && t.second.held()==TouchInput.UP, "Menus enable up for both players");
        t.router.setAllowPadUp(false);
        check(t.output.keys==0, "Entering gameplay releases menu up");
        float[] p=t.point(0,4);
        t.router.move(7,p[0],p[1]);
        check(t.output.keys==0, "Held menu contact cannot trigger gameplay jump");
        t.down(7,0,1);
        float[] other=t.point(1,1);
        t.router.move(7,other[0],other[1]);
        check(t.output.keys==0, "Crossing halves cannot steal the other player's controls");
        p=t.point(0,1);
        t.router.move(7,p[0],p[1]);
        check(t.first.held()==TouchInput.UP && t.second.held()==0, "Contact keeps its original owner on return");
        t.router.releaseAll();
    }

    private static void testSharedKeys() {
        Recorder output=new Recorder();
        int[] mapping={TouchInput.ACTION};
        TouchKeys keys=new TouchKeys((player,key)->mapping[0],output);
        keys.change(0,TouchInput.CONFIRM,true);
        output.expect("16+");
        mapping[0]=TouchInput.ITEM;
        keys.change(0,TouchInput.CONFIRM,false);
        output.expect("16-"); // Menu/game mapping changes must release the original key.
        keys.change(0,TouchInput.ACTION,true);
        output.expect("32+");
        keys.beginBatch();
        keys.change(0,TouchInput.ACTION,false);
        keys.change(1,TouchInput.ACTION,true);
        keys.endBatch();
        output.expect(); // Cross-player handoff in one event cannot retrigger a shared key.
        keys.change(1,TouchInput.ACTION,false);
        output.expect("32-");

        // A native ResetKeys can clear SDL's state while fingers remain down.
        keys.change(0, TouchInput.ACTION, true);
        output.expect("32+");
        output.allowRepeat = true;
        keys.resendHeld();
        output.expect("32+");
        output.allowRepeat = false;
        keys.change(0, TouchInput.ACTION, false);
        output.expect("32-");
    }
}
