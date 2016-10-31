package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Lauren on 10/31/2016.
 */

public class DrawingView extends View{
    private Paint mPaint;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
    }


    @Override
    public void onDraw(Canvas canvas) {

    }
}
