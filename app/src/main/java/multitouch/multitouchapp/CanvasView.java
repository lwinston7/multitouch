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

import static android.view.ViewConfiguration.getLongPressTimeout;

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

    private DrawMode currentDrawMode = DrawMode.Line;
    private DrawMode prevDrawMode = DrawMode.Line;

    private enum TouchMode {
        SingleFingerDraw, // 1-Finger Touch
        TwoFingerWait, // 2-Finger Wait for other movement
        Hold // 2-Finger Hold
    }
    private TouchMode currTouchMode = TouchMode.SingleFingerDraw;
    private float mLastFingerDown = 0;


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
                } else if (currentStroke instanceof Circle) {
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
        //Draw normally
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                Log.d("1 pointer", "down");
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currTouchMode == TouchMode.TwoFingerWait) {
                    Log.d("pointers", "multiple pointer move");
                    if (System.currentTimeMillis() - mLastFingerDown > getLongPressTimeout()) {
                        currTouchMode = TouchMode.Hold;
                        currentStroke = findNearestStroke(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        currentStroke.startMove(x, y);
                        drawPaint.setStrokeWidth(60);
                        setErase(true);
                        drawCanvas.drawPath(((DrawPath) currentStroke).getDrawPath(), drawPaint);
                        setErase(false);
                        // TODO: Remove stroke from arraylist as well.
                        Log.d("pointers", "HOLDING!!!!");
                    }
                } else if (currTouchMode == TouchMode.SingleFingerDraw){
                    moveTouch(x, y);
                    Log.d("1 pointer", "move");
                } else if (currTouchMode == TouchMode.Hold) {
                    Log.d("pointers", "MOVING");
                    currentStroke.move(x, y);
                    invalidate();
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    upTouch(x, y);
                    Log.d("1 pointer", "up");
                } else {
                    currTouchMode = TouchMode.SingleFingerDraw;
                    drawPaint.setStrokeWidth(brushSize);
                    drawCanvas.drawPath(((DrawPath)currentStroke).getDrawPath(), drawPaint);
                    strokes.add(currentStroke);
                    currentStroke = null;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                upTouch(x, y);
                Log.d("1 pointer", "pointer outside");
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    currTouchMode = TouchMode.TwoFingerWait;
                    mLastFingerDown = System.currentTimeMillis();
                    currentStroke = null;
                }
                Log.d("pointers", "pointer down");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d("pointers", "pointer up");
                break;
            default:
                Log.d("1 pointer", event.toString());
                break;
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
        Log.d("pointer eat", "" + EAT_COUNT);
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
            return strokes.remove(nearestStrokeIndex);
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
