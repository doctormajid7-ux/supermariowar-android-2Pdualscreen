package org.supermariowar.app;

import java.util.HashMap;
import java.util.Map;

/** Stable ownership of contacts across both halves, shared by the view and host tests. */
final class TouchRouter {
    private final TouchInput[] inputs;
    private final Map<Integer, Integer> owners = new HashMap<>();
    private final TouchLayout[] layouts = new TouchLayout[2];
    private float scale = 1;
    private boolean allowPadUp = true;

    TouchRouter(TouchInput first, TouchInput second) {
        inputs = new TouchInput[] {first, second};
    }

    void configure(TouchLayout first, TouchLayout second, float controlScale) {
        releaseAll();
        layouts[0] = first;
        layouts[1] = second;
        scale = controlScale;
    }

    boolean allowsPadUp() { return allowPadUp; }

    void setAllowPadUp(boolean enabled) {
        if (allowPadUp == enabled) return;
        releaseAll();
        allowPadUp = enabled;
    }

    void setEnabled(boolean enabled) {
        if (!enabled) releaseAll();
        for (TouchInput input : inputs) input.setEnabled(enabled);
    }

    void beginBatch() { for (TouchInput input : inputs) input.beginBatch(); }
    void endBatch() { for (TouchInput input : inputs) input.endBatch(); }

    void down(int id, float x, float y) {
        if (!inputs[0].isEnabled()) return;
        owners.put(id, layouts[1] != null && y < layouts[1].bottom ? 1 : 0);
        move(id, x, y);
    }

    void move(int id, float x, float y) {
        Integer owner = owners.get(id);
        // Ignore stale MOVE events after pause, layout changes, or menu transitions.
        if (owner == null) return;
        TouchLayout layout = layouts[owner];
        if (layout == null) return;
        int keys = 0;
        if (x >= layout.left && x <= layout.right && y >= layout.top && y <= layout.bottom) {
            if (owner == 1) {
                x = layout.left + layout.right - x;
                y = layout.top + layout.bottom - y;
            }
            keys = layout.hit(x, y, scale, allowPadUp);
        }
        inputs[owner].move(id, keys);
    }

    void up(int id) {
        Integer owner = owners.remove(id);
        if (owner != null) inputs[owner].release(id);
    }

    void releaseAll() {
        owners.clear();
        for (TouchInput input : inputs) input.releaseAll();
    }
}
