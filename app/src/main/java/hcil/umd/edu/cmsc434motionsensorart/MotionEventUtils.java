package hcil.umd.edu.cmsc434motionsensorart;

import android.view.MotionEvent;

/**
 * Created by jonf on 12/8/2016.
 */

public final class MotionEventUtils {

    // should not ever be constructed, purely a utility class
    private MotionEventUtils(){}

    // Given an action int, returns a string description
    public static String actionToString(int action) {
        switch (action) {

            case MotionEvent.ACTION_DOWN: return "Down";
            case MotionEvent.ACTION_MOVE: return "Move";
            case MotionEvent.ACTION_POINTER_DOWN: return "Pointer Down";
            case MotionEvent.ACTION_UP: return "Up";
            case MotionEvent.ACTION_POINTER_UP: return "Pointer Up";
            case MotionEvent.ACTION_OUTSIDE: return "Outside";
            case MotionEvent.ACTION_CANCEL: return "Cancel";
        }
        return "";
    }
}
