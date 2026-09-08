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
        public void keyChanged(int key, boolean down) {
            check(((keys & key) != 0) != down, "Unbalanced or duplicate key event");
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
        System.out.println("Touch controls: " + checks + " checks passed");
    }
}
