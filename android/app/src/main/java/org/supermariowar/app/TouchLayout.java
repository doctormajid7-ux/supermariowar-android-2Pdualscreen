package org.supermariowar.app;

/** Geometry shared by drawing and hit testing; all coordinates are view pixels. */
final class TouchLayout {
    static final class Button {
        final float x, y, radius;
        final int keys;
        Button(float x, float y, float radius, int keys) {
            this.x = x; this.y = y; this.radius = radius; this.keys = keys;
        }
        boolean contains(float px, float py) {
            return Math.hypot(px - x, py - y) <= radius;
        }
    }

    final float unit, padX, padY, padRadius;
    final float left, top, right, bottom;
    final float topY, topHeight, backX, confirmX, topWidth;
    final Button jump, action, item;

    TouchLayout(float left, float top, float right, float bottom, float density) {
        this(left, top, right, bottom, density, 1.0f);
    }

    TouchLayout(float left, float top, float right, float bottom, float density, float controlScale) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        float width = Math.max(1, right - left), height = Math.max(1, bottom - top);
        unit = Math.min(density, Math.min(width / 640f, height / 360f));
        padRadius = 84 * unit;
        padX = left + 104 * unit;
        padY = bottom - 106 * unit;
        jump = new Button(right - 66 * unit * controlScale, bottom - 83 * unit * controlScale,
                44 * unit * controlScale, TouchInput.UP);
        action = new Button(right - 166 * unit * controlScale, bottom - 64 * unit * controlScale,
                39 * unit * controlScale,
                TouchInput.ACTION | TouchInput.RANDOM);
        item = new Button(right - 147 * unit * controlScale, bottom - 156 * unit * controlScale,
                32 * unit * controlScale,
                TouchInput.ITEM | TouchInput.FAST);
        topY = top + 14 * unit;
        topHeight = 48 * unit;
        topWidth = 112 * unit;
        backX = left + 20 * unit;
        confirmX = right - 20 * unit - topWidth;
    }

    int hit(float x, float y) {
        return hit(x, y, 1.0f);
    }

    int hit(float x, float y, float padScale) {
        return hit(x, y, padScale, true);
    }

    int hit(float x, float y, float padScale, boolean allowPadUp) {
        if (y >= topY && y <= topY + topHeight) {
            if (x >= backX && x <= backX + topWidth) return TouchInput.BACK;
            if (x >= confirmX && x <= confirmX + topWidth) return TouchInput.CONFIRM;
        }
        if (jump.contains(x, y)) return jump.keys;
        if (action.contains(x, y)) return action.keys;
        if (item.contains(x, y)) return item.keys;
        float dx = x - padX, dy = y - padY;
        // Square bounds give the cross generous targets, including diagonals.
        if (Math.abs(dx) > padRadius * padScale || Math.abs(dy) > padRadius * padScale) return 0;
        float dead = 19 * unit * padScale;
        int keys = 0;
        if (dx < -dead) keys |= TouchInput.LEFT;
        if (dx > dead) keys |= TouchInput.RIGHT;
        if (allowPadUp && dy < -dead) keys |= TouchInput.UP;
        if (dy > dead) keys |= TouchInput.DOWN;
        return keys;
    }
}
