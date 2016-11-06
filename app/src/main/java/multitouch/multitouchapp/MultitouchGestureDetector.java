package multitouch.multitouchapp;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import static android.view.ViewConfiguration.getLongPressTimeout;

/**
 * Created by Lauren on 11/5/2016.
 */

public class MultitouchGestureDetector extends GestureDetector{
    private float mTwoFingerTimeDown = 0;
    public MultitouchGestureDetector(Context context, OnGestureListener listener) {
        super(context, listener);
    }

    public boolean onDoubleHoldEvent(MotionEvent motionEvent) {
        if(motionEvent.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && motionEvent.getActionIndex()==0){
            mTwoFingerTimeDown = System.currentTimeMillis();
        }

        if(motionEvent.getActionMasked() == MotionEvent.ACTION_POINTER_UP && motionEvent.getActionIndex()==0 ){
            if ((System.currentTimeMillis()-mTwoFingerTimeDown) >= getLongPressTimeout()) {
                //long double-press action
                Log.d("mask", "two finger tap");
                return true;
            } else {
                //short double-press action
            }
        }

        return false;
    }

    public boolean onResizeEvent(MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() > 4) {
            // TODO: Check to see if pointers are growing or shrinking.
            return true;
        } else {
            return false;
        }
    }
}
