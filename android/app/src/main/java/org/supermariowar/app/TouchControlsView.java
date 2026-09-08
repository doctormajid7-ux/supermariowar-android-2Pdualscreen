package org.supermariowar.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.DisplayCutout;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import org.libsdl.app.SDLActivity;

/** Transparent controller above SDL's surface. It consumes touches, never physical keys. */
final class TouchControlsView extends View {
    private static final int[] PAD_KEYS = {TouchInput.UP, TouchInput.RIGHT, TouchInput.DOWN, TouchInput.LEFT};
    private static final int[] PAD_X = {0, 56, 0, -56};
    private static final int[] PAD_Y = {-56, 0, 56, 0};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();
    private final TouchKeys keys = new TouchKeys(this::mapKey, (key, down) -> {
        if (down) SDLActivity.onNativeKeyDown(key);
        else SDLActivity.onNativeKeyUp(key);
    });
    private final TouchInput input = new TouchInput((key, down) -> keys.change(0, key, down));
    private final TouchInput input2 = new TouchInput((key, down) -> keys.change(1, key, down));
    private final TouchRouter router = new TouchRouter(input, input2);
    private TouchLayout layout, layout2;
    private boolean twoPlayerMode;
    private boolean gameplay;
    private boolean dpadUpAllowed = true;
    private final Handler inputHeartbeat = new Handler(Looper.getMainLooper());
    private boolean inputActive;
    private final Runnable reassertHeldKeys = new Runnable() {
        @Override public void run() {
            if (!inputActive || !isAttachedToWindow()) return;
            keys.resendHeld();
            inputHeartbeat.postDelayed(this, 100);
        }
    };

    private float controlScale() {
        return getHeight() > getWidth() ? 1.25f : 1.0f;
    }
    private int insetLeft, insetTop, insetRight, insetBottom;

    TouchControlsView(Context context) {
        super(context);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setOnApplyWindowInsetsListener((view, insets) -> {
            insetLeft = insets.getSystemWindowInsetLeft();
            insetTop = insets.getSystemWindowInsetTop();
            insetRight = insets.getSystemWindowInsetRight();
            insetBottom = insets.getSystemWindowInsetBottom();
            if (Build.VERSION.SDK_INT >= 28) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    insetLeft = Math.max(insetLeft, cutout.getSafeInsetLeft());
                    insetTop = Math.max(insetTop, cutout.getSafeInsetTop());
                    insetRight = Math.max(insetRight, cutout.getSafeInsetRight());
                    insetBottom = Math.max(insetBottom, cutout.getSafeInsetBottom());
                }
            }
            rebuildLayout();
            return insets;
        });
    }

    private int mapKey(int player, int key) {
        final int androidKey;
        if (player == 0) {
            switch (key) {
                case TouchInput.LEFT: androidKey = KeyEvent.KEYCODE_DPAD_LEFT; break;
                case TouchInput.RIGHT: androidKey = KeyEvent.KEYCODE_DPAD_RIGHT; break;
                case TouchInput.UP: androidKey = KeyEvent.KEYCODE_DPAD_UP; break;
                case TouchInput.DOWN: androidKey = KeyEvent.KEYCODE_DPAD_DOWN; break;
                case TouchInput.ACTION: androidKey = KeyEvent.KEYCODE_CTRL_RIGHT; break;
                case TouchInput.ITEM: androidKey = KeyEvent.KEYCODE_SHIFT_RIGHT; break;
                case TouchInput.CONFIRM: androidKey = KeyEvent.KEYCODE_ENTER; break;
                case TouchInput.BACK: androidKey = KeyEvent.KEYCODE_ESCAPE; break;
                case TouchInput.RANDOM: androidKey = KeyEvent.KEYCODE_SPACE; break;
                case TouchInput.FAST: androidKey = KeyEvent.KEYCODE_SHIFT_LEFT; break;
                default: throw new IllegalArgumentException("Unknown touch key");
            }
        } else {
            // Player 2 follows the default keyboard layout: W/A/S/D, E and Q.
            switch (key) {
                case TouchInput.LEFT: androidKey = KeyEvent.KEYCODE_A; break;
                case TouchInput.RIGHT: androidKey = KeyEvent.KEYCODE_D; break;
                case TouchInput.UP: androidKey = KeyEvent.KEYCODE_W; break;
                case TouchInput.DOWN: androidKey = KeyEvent.KEYCODE_S; break;
                case TouchInput.ACTION: androidKey = KeyEvent.KEYCODE_E; break;
                case TouchInput.ITEM: androidKey = KeyEvent.KEYCODE_Q; break;
                case TouchInput.CONFIRM:
                    androidKey = ((GameActivity) getContext()).isGameplay()
                            ? KeyEvent.KEYCODE_F : KeyEvent.KEYCODE_E;
                    break;
                case TouchInput.BACK:
                    androidKey = ((GameActivity) getContext()).isGameplay()
                            ? KeyEvent.KEYCODE_R : KeyEvent.KEYCODE_Q;
                    break;
                case TouchInput.RANDOM: androidKey = KeyEvent.KEYCODE_SPACE; break;
                case TouchInput.FAST: androidKey = KeyEvent.KEYCODE_SHIFT_LEFT; break;
                default: throw new IllegalArgumentException("Unknown touch key");
            }
        }
        return androidKey;
    }

    void releaseAll() {
        router.releaseAll();
        invalidate();
    }

    void setInputActive(boolean active) {
        inputActive = active;
        router.setEnabled(active);
        inputHeartbeat.removeCallbacks(reassertHeldKeys);
        if (active) inputHeartbeat.post(reassertHeldKeys);
        invalidate();
    }

    void refreshMenuState(boolean isGameplay, boolean allowUp) {
        gameplay = isGameplay;
        dpadUpAllowed = allowUp;
        if (router.allowsPadUp() != allowUp) {
            router.setAllowPadUp(allowUp);
            invalidate();
        }
    }

    void setTwoPlayerMode(boolean enabled) {
        if (twoPlayerMode == enabled) return;
        twoPlayerMode = enabled;
        rebuildLayout();
    }

    private void rebuildLayout() {
        releaseAll();
        float left = insetLeft, top = insetTop, right = getWidth() - insetRight;
        float bottom = getHeight() - insetBottom;
        float scale = controlScale();
        if (twoPlayerMode && bottom - top > 1) {
            float middle = top + (bottom - top) / 2f;
            layout2 = new TouchLayout(left, top, right, middle,
                    getResources().getDisplayMetrics().density, scale);
            layout = new TouchLayout(left, middle, right, bottom,
                    getResources().getDisplayMetrics().density, scale);
        } else {
            layout2 = null;
            layout = new TouchLayout(left, top, right, bottom,
                    getResources().getDisplayMetrics().density, scale);
        }
        router.configure(layout, layout2, scale);
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rebuildLayout();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
    }

    @Override protected void onDetachedFromWindow() {
        inputActive = false;
        inputHeartbeat.removeCallbacks(reassertHeldKeys);
        releaseAll();
        super.onDetachedFromWindow();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (layout == null || !input.isEnabled()) return true;
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL) {
            releaseAll();
            return true;
        }
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN
                && action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_POINTER_UP) return true;
        final int changed = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN) releaseAll();
        // Pointer IDs are stable; indices change as other fingers lift.
        keys.beginBatch();
        router.beginBatch();
        for (int i = 0; i < event.getPointerCount(); i++) {
            int id = event.getPointerId(i);
            if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) && i == changed)
                router.up(id);
            else if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && i == changed)
                router.down(id, event.getX(i), event.getY(i));
            else
                router.move(id, event.getX(i), event.getY(i));
        }
        router.endBatch();
        keys.endBatch();
        if (action == MotionEvent.ACTION_UP) {
            releaseAll();
            performClick();
        }
        invalidate();
        return true; // Do not also send these touches to SDL as mouse clicks.
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        if (layout == null) return;
        TouchLayout l = layout;
        drawControls(canvas, l, input);
        if (layout2 != null) {
            canvas.save();
            canvas.clipRect(0, layout2.top, getWidth(), layout2.bottom);
            canvas.rotate(180, getWidth() / 2f, (layout2.top + layout2.bottom) / 2f);
            drawControls(canvas, layout2, input2);
            canvas.restore();
        }
    }

    private void drawControls(Canvas canvas, TouchLayout l, TouchInput source) {
        drawPad(canvas, l, source);
        drawButton(canvas, l, source, l.jump, R.string.touch_jump, Color.rgb(77, 160, 255));
        drawButton(canvas, l, source, l.action, R.string.touch_action, Color.rgb(255, 174, 64));
        drawButton(canvas, l, source, l.item, R.string.touch_item, Color.rgb(174, 119, 237));
        drawTopButton(canvas, l, source, l.backX, TouchInput.BACK, R.string.touch_back);
        drawTopButton(canvas, l, source, l.confirmX, TouchInput.CONFIRM, R.string.touch_confirm);
    }

    private void fill(boolean down, int accent) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(down ? accent : Color.rgb(18, 24, 34));
        paint.setAlpha(down ? 210 : 115);
    }

    private void outline(boolean down) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * layout.unit);
        paint.setColor(Color.WHITE);
        paint.setAlpha(down ? 245 : 150);
    }

    private void label(Canvas canvas, String text, float x, float y, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(235);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(size * layout.unit);
        canvas.drawText(text, x, y - (paint.ascent() + paint.descent()) / 2, paint);
    }

    private void drawButton(Canvas canvas, TouchLayout l, TouchInput source, TouchLayout.Button b, int text, int accent) {
        boolean down = (source.held() & b.keys) != 0;
        fill(down, accent);
        canvas.drawCircle(b.x, b.y, b.radius, paint);
        outline(down);
        canvas.drawCircle(b.x, b.y, b.radius, paint);
        label(canvas, getResources().getString(text), b.x, b.y, 13);
    }

    private void drawTopButton(Canvas canvas, TouchLayout l, TouchInput source, float x, int key, int text) {
        boolean down = (source.held() & key) != 0;
        fill(down, Color.rgb(77, 160, 255));
        canvas.drawRoundRect(x, l.topY, x + l.topWidth, l.topY + l.topHeight, 12*l.unit, 12*l.unit, paint);
        outline(down);
        canvas.drawRoundRect(x, l.topY, x + l.topWidth, l.topY + l.topHeight, 12*l.unit, 12*l.unit, paint);
        label(canvas, getResources().getString(text), x + l.topWidth/2, l.topY + l.topHeight/2, 12);
    }

    private void drawPad(Canvas canvas, TouchLayout l, TouchInput source) {
        float u = l.unit, scale = controlScale();
        float cell = 54*u*scale, corner = 9*u*scale;
        for (int i = 0; i < 4; i++) {
            if (PAD_KEYS[i] == TouchInput.UP && !router.allowsPadUp()) continue;
            float x = l.padX + PAD_X[i]*u*scale, y = l.padY + PAD_Y[i]*u*scale;
            boolean down = (source.held() & PAD_KEYS[i]) != 0;
            fill(down, Color.rgb(77, 160, 255));
            canvas.drawRoundRect(x-cell/2, y-cell/2, x+cell/2, y+cell/2, corner, corner, paint);
            outline(down);
            canvas.drawRoundRect(x-cell/2, y-cell/2, x+cell/2, y+cell/2, corner, corner, paint);
            canvas.save();
            canvas.rotate(i*90, x, y);
            arrow.reset();
            arrow.moveTo(x, y-9*u*scale); arrow.lineTo(x+11*u*scale, y+7*u*scale);
            arrow.lineTo(x-11*u*scale, y+7*u*scale); arrow.close();
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); paint.setAlpha(235);
            canvas.drawPath(arrow, paint);
            canvas.restore();
        }
    }
}
