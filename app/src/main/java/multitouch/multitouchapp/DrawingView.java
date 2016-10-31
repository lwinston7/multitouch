package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Lauren on 10/31/2016.
 */

public class DrawingView extends View{
    private Paint mPaint;
    private ArrayList<Path> paths = new ArrayList<Path>();
    private Path currentPath;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        this.setOnTouchListener(new BasicStrokeListener());
    }


    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), mPaint);
        }

        if (currentPath != null) {
            canvas.drawPath(currentPath, mPaint);
        }
    }

    private class BasicStrokeListener implements OnTouchListener {
        private float startX;
        private float startY;
        @Override
        public boolean onTouch(View v, final MotionEvent event) {
            int action = MotionEventCompat.getActionMasked(event);

            switch(action) {
                case (MotionEvent.ACTION_DOWN) :
                    // Create stroke
                    startX = event.getX();
                    startY =event.getY();
                    currentPath = new Path();
                    currentPath.moveTo(startX, startY);
                    invalidate();
                    return true;
                case (MotionEvent.ACTION_MOVE) :
                    // Update stroke
                    float x = event.getX();
                    float y =event.getY();
                    currentPath.lineTo(x, y);
                    invalidate();
                    return true;
                case (MotionEvent.ACTION_UP) :
                    // Finish stroke
                    currentPath.lineTo(event.getX(), event.getY());
                    paths.add(currentPath);
                    currentPath = null;
                    invalidate();
                    return true;
                case (MotionEvent.ACTION_OUTSIDE) :
                    // Finish stroke
                    currentPath.lineTo(event.getX(), event.getY());
                    paths.add(currentPath);
                    currentPath = null;
                    invalidate();
                    return true;
                default :
                    return false;
            }
        }
    }
}
