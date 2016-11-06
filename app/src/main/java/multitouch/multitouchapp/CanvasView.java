package multitouch.multitouchapp;

/**
 * Created by michelleliu on 10/26/16.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.TypedValue;
import java.util.ArrayList;

public class CanvasView extends View{

    private Bitmap canvasBitmap;
    private Canvas drawCanvas;
    private ArrayList<Stroke> strokes = new ArrayList<Stroke>();
    private Stroke currentStroke;
    private Path drawPath;
    private Paint canvasPaint;
    private Paint drawPaint;
    Context context;
    private int paintColor = 0xFF660000;
    private float mX, mY;
    private float brushSize, lastBrushSize;
    private int bgColor = Color.WHITE;
    private MultitouchGestureDetector mGestureDetector;
    private static final float TOLERANCE = 5;
    private boolean EAT_POINTER_INPUT = false;
    private int EAT_COUNT = 0;


    private enum DrawMode {
        Line,
        Circle,
        Rectangle,
        Erase
    }
    
    private enum TouchMode {
        Single,
        Select
    }

    private DrawMode currentDrawMode = DrawMode.Line;
    private DrawMode prevDrawMode = DrawMode.Line;



    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;
        setupDrawing();
        mGestureDetector = new MultitouchGestureDetector(context, new DrawingGestureListener());
    }

    public void setupDrawing(){
        brushSize = context.getResources().getInteger(R.integer.medium_size);
        lastBrushSize = brushSize;
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

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

        if (currentStroke != null && currentDrawMode != DrawMode.Erase) {
            if (currentStroke instanceof DrawPath) {
                canvas.drawPath(((DrawPath) currentStroke).getDrawPath(), drawPaint);
            } else if (currentStroke instanceof Circle){
                // draw shape.
                Circle c = (Circle) currentStroke;
                canvas.drawCircle(c.getX(), c.getY(), c.getRadius(), drawPaint);
            } else if (currentStroke instanceof Rectangle) {
                Rectangle r = (Rectangle) currentStroke;
                canvas.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), drawPaint);
            }
        }
    }

    private void startTouch(float x, float y) {
        switch (currentDrawMode) {
            case Line:
                currentStroke = new DrawPath();
                currentStroke.startStroke(x, y);
                break;
            case Circle:
                currentStroke = new Circle();
                currentStroke.startStroke(x, y);
                break;
            case Erase:
                startPath(x, y);
                break;
            case Rectangle:
                currentStroke = new Rectangle();
                currentStroke.startStroke(x, y);
                break;
        }
    }

    private void startPath(float x, float y) {
        drawPath = new Path();
        drawPath.moveTo(x,y);
        mX = x;
        mY = y;
    }

    private void moveTouch(float x, float y) {
        if (currentDrawMode == DrawMode.Erase) {
            movePath(x, y);
            // TODO: Does drawPath need to be cleared every time?
            drawCanvas.drawPath(drawPath, drawPaint);
        }
        if (currentStroke != null) {
            currentStroke.update(x, y);
        }
    }

    private void movePath(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            drawPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    public void clearCanvas() {
        if (drawPath != null) {
            drawPath.reset();
        }
        drawCanvas.drawColor(Color.WHITE);
        strokes = new ArrayList<Stroke>();
        invalidate();
    }

    private void upTouch(float x, float y) {
        if (currentDrawMode == DrawMode.Erase) {
            upPath(x, y);
        } else if (currentStroke != null){
            currentStroke.finishStroke(x, y);

            switch (currentDrawMode) {
                case Line:
                    drawCanvas.drawPath(((DrawPath) currentStroke).getDrawPath(), drawPaint);
                    break;
                case Circle:
                    Circle c = (Circle) currentStroke;
                    drawCanvas.drawCircle(c.getX(), c.getY(), c.getRadius(), drawPaint);
                    break;
                case Rectangle:
                    Rectangle r = (Rectangle) currentStroke;
                    drawCanvas.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), drawPaint);
                    break;

            }

            strokes.add(currentStroke);
            currentStroke = null;
        }
    }

    private void upPath(float x, float y) {
        movePath(x, y);
        drawCanvas.drawPath(drawPath, drawPaint);
        drawPath = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mGestureDetector.onTouchEvent(event) || event.getPointerCount() > 1) {
            // We've detected one of our gestures or a multitouch gesture!
            if (mGestureDetector.onDoubleHoldEvent(event)) {
                Stroke selectedStroke = findNearestStroke(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                // handle two finger press here.

            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    Log.d("pointers", "pointer down");
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    Log.d("pointers", "pointer up");
                    EatInput();
                    break;
                case MotionEvent.ACTION_DOWN:
                    Log.d("pointers", "down");
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d("pointers", "up");
                    EatInput();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("pointers", "move");
                    break;
            }

        } else if (EAT_COUNT > 0) {
            Log.d("pointers", "eat line input " + EAT_COUNT);
            EAT_COUNT--;
        } else {
            //Draw normally
            Log.d("pointers", "draw normally " + EAT_COUNT);
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
        if (isErase) {
            prevDrawMode = currentDrawMode;
            currentDrawMode = DrawMode.Erase;
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            drawPaint.setColor(bgColor);
        } else {
            //TODO: Change
            currentDrawMode = prevDrawMode;
            drawPaint.setXfermode(null);
            drawPaint.setColor(paintColor);
        }
    }

    public void EatInput() {
        EAT_POINTER_INPUT = true;
        EAT_COUNT++;
        currentStroke = null;
    }

    public Stroke findNearestStroke(float x1, float y1, float x2, float y2) {
        float nearestDistance = Integer.MAX_VALUE;
        int nearestStrokeIndex = -1;
        // Give priority to strokes drawn most recently.
        for (int i = strokes.size() - 1; i >= 0; i--) {
            float currDistance = strokes.get(i).distanceFromTap(x1, y1, x2, y2);
            if (currDistance < nearestDistance) {
                nearestDistance = currDistance;
                nearestStrokeIndex = i;
            };
        }

        if (nearestStrokeIndex > -1) {
            Log.d("stroke", nearestStrokeIndex + "");
            return strokes.get(nearestStrokeIndex);
        } else {
            return null;
        }
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
