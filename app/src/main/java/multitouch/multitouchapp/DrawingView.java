package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Lauren on 10/31/2016.
 */

public class DrawingView extends View{
    private Paint mPaint;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        this.setOnTouchListener(new BasicStrokeListener());
    }


    @Override
    public void onDraw(Canvas canvas) {

    }

    private class BasicStrokeListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);

            switch(action) {
                case (MotionEvent.ACTION_DOWN) :
                    // Create stroke
                    return true;
                case (MotionEvent.ACTION_MOVE) :
                    // Update stroke
                    return true;
                case (MotionEvent.ACTION_UP) :
                    // Finish stroke
                    return true;
                case (MotionEvent.ACTION_OUTSIDE) :
                    // Finish stroke
                    return true;
                default :
                    return false;
            }
        }
    }
}
