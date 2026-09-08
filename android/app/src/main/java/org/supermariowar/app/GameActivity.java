package org.supermariowar.app;

import org.libsdl.app.SDLActivity;
import java.io.File;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.ActivityInfo;
import android.view.ViewGroup;

public class GameActivity extends SDLActivity {
    private TouchControlsView touchControls;
    private final Handler modeHandler = new Handler(Looper.getMainLooper());
    private boolean portraitMode;
    // Read on the UI thread at a low rate. TouchEvent handling must never call JNI
    // for every finger or every movement: on some devices that stalls the input
    // queue long enough to look like a one-second loss of controls.
    private volatile boolean gameplay;
    private volatile boolean dpadUpAllowed = true;
    private final Runnable modePoll = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed()) return;
            boolean requested = nativeIsTwoPlayerPortrait();
            gameplay = nativeIsGameplay();
            dpadUpAllowed = nativeAllowsDpadUp();
            // During a match player-control values can briefly be rebuilt while
            // the native state changes. Do not let that transient false value
            // rotate the Activity: orientation changes drop window focus and can
            // stop both players' touch input for roughly a second.
            if (portraitMode && gameplay && !requested) requested = true;
            if (requested != portraitMode) {
                portraitMode = requested;
                setRequestedOrientation(requested ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                if (touchControls != null) touchControls.setTwoPlayerMode(requested);
            }
            if (touchControls != null) touchControls.refreshMenuState(gameplay, dpadUpAllowed);
            modeHandler.postDelayed(this, 250);
        }
    };

    private native boolean nativeIsTwoPlayerPortrait();
    private native boolean nativeIsGameplay();
    private native boolean nativeAllowsDpadUp();

    boolean allowsDpadUp() {
        return dpadUpAllowed;
    }

    boolean isGameplay() {
        return gameplay;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // SDL can return early if native library loading fails.
        if (mLayout != null) {
            touchControls = new TouchControlsView(this);
            mLayout.addView(touchControls, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            modeHandler.post(modePoll);
        }
    }

    @Override protected void onPause() {
        if (touchControls != null) touchControls.setInputActive(false);
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        if (touchControls != null) touchControls.setInputActive(focused);
        super.onWindowFocusChanged(focused);
    }

    @Override protected void onDestroy() {
        modeHandler.removeCallbacks(modePoll);
        if (touchControls != null) touchControls.setInputActive(false);
        super.onDestroy();
    }

    // SDL2's USB permission receiver uses the older overload. Target API 35
    // requires an explicit export policy for its custom permission action.
    @Override public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 33) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
        return super.registerReceiver(receiver, filter);
    }
    @Override protected String[] getLibraries() {
        return new String[] {"SDL2", "SDL2_image", "SDL2_mixer", "main"};
    }
    @Override protected String[] getArguments() {
        return new String[] {"--datadir", new File(getFilesDir(), "game/data").getAbsolutePath()};
    }
}
