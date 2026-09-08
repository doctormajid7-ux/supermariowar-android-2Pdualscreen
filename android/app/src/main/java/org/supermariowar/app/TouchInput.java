package org.supermariowar.app;

import java.util.HashMap;
import java.util.Map;

/** Multi-pointer key ownership, independent of Android so it can be tested on the host. */
final class TouchInput {
    static final int LEFT = 1;
    static final int RIGHT = 1 << 1;
    static final int UP = 1 << 2;
    static final int DOWN = 1 << 3;
    static final int ACTION = 1 << 4;
    static final int ITEM = 1 << 5;
    static final int CONFIRM = 1 << 6;
    static final int BACK = 1 << 7;
    static final int RANDOM = 1 << 8;
    static final int FAST = 1 << 9;
    static final int ALL = (1 << 10) - 1;

    interface Sink { void keyChanged(int key, boolean down); }
    private final Sink sink;
    private final Map<Integer, Integer> pointers = new HashMap<>();
    private int held;
    private boolean batching;
    private boolean enabled = true;

    TouchInput(Sink sink) { this.sink = sink; }
    int held() { return held; }
    boolean isEnabled() { return enabled; }

    void setEnabled(boolean value) {
        enabled = value;
        if (!value) releaseAll();
    }

    void beginBatch() { batching = true; }
    void endBatch() {
        batching = false;
        update();
    }

    void move(int pointerId, int keys) {
        if (!enabled) return;
        pointers.put(pointerId, keys & ALL);
        update();
    }

    void release(int pointerId) {
        pointers.remove(pointerId);
        update();
    }

    void releaseAll() {
        pointers.clear();
        update();
    }

    private void update() {
        if (batching) return;
        int next = 0;
        for (int keys : pointers.values()) next |= keys;
        int released = held & ~next;
        int pressed = next & ~held;
        held = next;
        // Release first when sliding to a different control. A second finger owning
        // the same key keeps it held, and MOVE never generates repeated key-downs.
        for (int key = 1; key <= FAST; key <<= 1)
            if ((released & key) != 0) sink.keyChanged(key, false);
        for (int key = 1; key <= FAST; key <<= 1)
            if ((pressed & key) != 0) sink.keyChanged(key, true);
    }
}
