package multitouch.multitouchapp;

/**
 * Created by michelleliu on 10/26/16.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.TypedValue;

import java.util.ArrayList;

public class CanvasView extends View{

    public int width;
    public int height;
    private Bitmap canvasBitmap;
    private Canvas drawCanvas;
    private ArrayList<Path> paths = new ArrayList<Path>();
    private Path drawPath;
    private Paint canvasPaint;
    private Paint drawPaint;
    Context context;
    private boolean erase=false;
    private int paintColor = 0xFF660000;
    private float mX, mY;
    private float brushSize, lastBrushSize;

    private static final float TOLERANCE = 5;
    private GestureDetector mGestureDetector;


    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;
        setupDrawing();
        mGestureDetector = new GestureDetector(context, new DrawingGestureListener());
    }

    public void setupDrawing(){
       // brushSize = getResources().getInteger(R.integer.medium_size);
        brushSize = 20;
        lastBrushSize = brushSize;
        drawPath = new Path();
        // and we set a new Paint with the desired attributes
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setColor(paintColor);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeWidth(4f);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }
    @Override
    public void onSizeChanged(int w,int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /*
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), drawPaint);
        }
        */

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

        if (drawPath != null) {
            canvas.drawPath(drawPath, drawPaint);
        }
    }

    private void startTouch(float x, float y) {
        drawPath = new Path();
        drawPath.moveTo(x,y);
        mX = x;
        mY = y;
    }

    private void moveTouch(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            drawPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }

    public void clearCanvas() {
        if (drawPath != null) {
            drawPath.reset();
        }
        drawCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    private void upTouch(float x, float y) {
        moveTouch(x, y);
        //drawPath.lineTo(mX, mY);
        drawCanvas.drawPath(drawPath,drawPaint);
        paths.add(drawPath);
        drawPath = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mGestureDetector.onTouchEvent(event)) {
            // We've detected one of our gestures!

        } else {
            //Draw normally
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTouch(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveTouch(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    upTouch(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    upTouch(x, y);
                    invalidate();
                    break;
            }
        }

        return true;
    }

    public void setColor(String newColor) {
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);
        invalidate();
    }

    public void setBrushSize(float newSize){
    //update size
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize=pixelAmount;
        System.out.println("brush size: " + brushSize + " =? " + newSize);
        drawPaint.setStrokeWidth(brushSize);
    }

    public void setLastBrushSize(float lastSize){
        lastBrushSize=lastSize;
    }
    public float getLastBrushSize(){
        return lastBrushSize;
    }

    public void setErase(boolean isErase){
    //set erase true or false
        erase=isErase;
        if(erase) drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        else drawPaint.setXfermode(null);
    }

    private class DrawingGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }
}
