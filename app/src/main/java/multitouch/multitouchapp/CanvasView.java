package multitouch.multitouchapp;

/**
 * Created by michelleliu on 10/26/16.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.TypedValue;
import java.util.ArrayList;
import android.graphics.PointF;
import android.util.Log;

import static android.view.ViewConfiguration.getLongPressTimeout;

public class CanvasView extends View{

    private final int MAXIMUM_DRAG_DISTANCE = 300;
    private final int MINIMUM_MOVE_DISTANCE = 10;
    private Bitmap canvasBitmap;
    private Canvas drawCanvas;
    private ArrayList<Stroke> strokes = new ArrayList<Stroke>();
    private Stroke currentStroke;
    private Path erasePath;
    private Paint canvasPaint;
    private Paint drawPaint;
    Context context;
    private int paintColor = 0xFF660000;
    private float mX, mY;
    private float brushSize, lastBrushSize;
    private int bgColor = Color.WHITE;
    private MultitouchGestureDetector mGestureDetector;
    private static final float TOLERANCE = 5;
    private float prevScaleDist;
    private float currScaleDist;
    private Stroke tappedStroke;
    private Circle clonedCircle;
    private Rectangle clonedRect;
    private float prevSwipeY1, prevSwipeY2, prevSwipeY3, prevSwipeY4;

    private float mScrollX = 0;
    private float mScrollY = 0;

    private float mLastX = 0;
    private float mLastY = 0;

    private int width, height;
    private boolean isCloned;

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
        OneFingerWait,
        OneFingerHold,
        Perfection,
        PerfectionWait,
        TwoFingerWait, // 2-Finger Wait for other movement
        Hold // 2-Finger Hold
    }

    private TouchMode currTouchMode = TouchMode.SingleFingerDraw;
    private float mLastFingerDown = 0;

    private enum GestureMode {
        Drag,
        Rotate,
        Scale,
        Clone
    }

    private GestureMode currGestureMode = null;


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
        width = w * 2;
        height = h * 2;
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(canvasBitmap, mScrollX * -1, mScrollY * -1, canvasPaint);

        //TODO: Offset by mScrollX and mScrollY if needed.
            if (currentStroke != null && currentDrawMode != DrawMode.Erase) {
                canvas.drawPath(currentStroke.getDrawPath(), drawPaint);
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
        if (currentStroke != null) {
            currentStroke.setColor(paintColor);
            currentStroke.setSize(brushSize);
        }
    }

    private void startPath(float x, float y) {
        erasePath = new Path();
        erasePath.moveTo(x,y);
        mX = x;
        mY = y;
    }

    private void moveTouch(float x, float y) {
        if (currentDrawMode == DrawMode.Erase) {
            movePath(x, y);
            // TODO: Does erasePath need to be cleared every time?
            drawCanvas.drawPath(erasePath, drawPaint);
        }
        if (currentStroke != null) {
            currentStroke.update(x, y);
        }
    }

    private void movePath(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            erasePath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    public void clearCanvas() {
        erasePath = null;
        mScrollX = 0;
        mScrollY = 0;
        currentStroke = null;
        if (currentDrawMode == DrawMode.Erase) {
            setErase(false);
        }
        strokes = new ArrayList<Stroke>();
        resetCanvas();
        invalidate();
    }

    private void upTouch(float x, float y) {
        if (currentDrawMode == DrawMode.Erase) {
            upPath(x, y);
        } else if (currentStroke != null){
            currentStroke.finishStroke(x, y);
            drawCanvas.drawPath(currentStroke.getDrawPath(), drawPaint);
            if (currentStroke instanceof PerfectStroke) {
                strokes.add(((PerfectStroke) currentStroke).getPerfectStroke());
            } else {
                strokes.add(currentStroke);
                currentStroke = null;
            }
        }
    }

    private void upPath(float x, float y) {
        movePath(x, y);
        drawCanvas.drawPath(erasePath, drawPaint);
        erasePath = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Draw normally
        float x = event.getX();
        float y = event.getY();
        isCloned = false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currTouchMode == TouchMode.OneFingerWait && distanceFromLastPoint(x, y) > MINIMUM_MOVE_DISTANCE ) {
                    currTouchMode = TouchMode.SingleFingerDraw;
                }

                if (currTouchMode == TouchMode.TwoFingerWait) {
                    if (System.currentTimeMillis() - mLastFingerDown > getLongPressTimeout()) {
                        // Make sure that two pointers are down.
                        // TODO: Add a check to make sure that they haven't been moved for too long?
                        if (event.getPointerCount() >= 2) {
                            currentStroke = popNearestStroke(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        }
                        if (currentStroke != null) {
                            currTouchMode = TouchMode.Hold;
                            currentStroke.startMove(x, y);
                            invalidate();
                        } else {
                            mLastFingerDown = System.currentTimeMillis();
                        }
                    }
                } else if (currTouchMode == TouchMode.SingleFingerDraw) {
                    if (currentStroke != null) {
                        moveTouch(x, y);
                    }
                } else if (currTouchMode == TouchMode.Perfection) {
                    moveTouch(event.getX(1), event.getY(1));
                } else if (currTouchMode == TouchMode.Hold) {
                    Log.d("inside move hold 1", currGestureMode.toString());
                    //add a condition here
                    if (currentStroke != null && currGestureMode == GestureMode.Drag) {
                        Log.d("drag", event.getPointerCount() + "");
                        if (event.getPointerCount() >= 2) {
                            Point p0 = new Point((int) event.getX(0), (int) event.getY(0));
                            Point p1 = new Point((int) event.getX(1), (int) event.getY(1));
                            //change here
                            currentStroke.move((p0.x + p1.x)/2, (p0.y + p1.y)/2);
                            if (currentStroke instanceof DrawShape && ((DrawShape) currentStroke).getIsFilled()) {
                                drawPaint.setStyle(Paint.Style.FILL);
                                drawPaint.setAlpha(((DrawShape) currentStroke).getTransparency());
                            }
                        } else {
                            currentStroke.move(x, y);
                        }
                    }
                    if (currentStroke != null && currGestureMode == GestureMode.Clone) {
                        Log.d("inside move 22 clone", currGestureMode.toString() + event.getPointerCount());
                        if (event.getPointerCount() == 1 ) {
                            float cloneX = event.getX(0);
                            float cloneY = event.getY(0);
                            if (currentStroke instanceof Circle) {
                                clonedCircle = (Circle) currentStroke;
                                if (!isCloned) {
                                    Log.d("## inside move 22 clone", currentStroke.toString());
                                    strokes.add(currentStroke);
                                    isCloned = true;
                                }
                                clonedCircle.move(cloneX, cloneY);
                                clonedCircle = null;
                                //drawCanvas.drawCircle(clonedCircle.getX(), clonedCircle.getY(), clonedCircle.getRadius(), drawPaint);
                            } else if (currentStroke instanceof Rectangle) {
                                clonedRect = (Rectangle) currentStroke;
                                if (!isCloned){
                                    Log.d("inside clone 22 rect", clonedRect.toString());
                                    //drawCanvas.drawRect(clonedRect.getRect(), drawPaint);
                                    clonedRect.move(cloneX, cloneY);
                                    strokes.add(currentStroke);
                                    isCloned = true;
                                }
                            }
                        }
                        invalidate();
                    }
                    if (currentStroke != null && currGestureMode == GestureMode.Rotate) {
                        Log.d("inside move 33 rorate", currGestureMode.toString());
                        if (event.getPointerCount() == 2) {
                            float rotateDegree = rotation(event);
                            if (currentStroke instanceof DrawShape) {
                                drawCanvas.save();
                                drawCanvas.rotate(rotateDegree);
                                drawCanvas.drawPath(currentStroke.getDrawPath(),drawPaint);
                                drawCanvas.restore();
                                break;
                            }
                        }
                        invalidate();
                    }
                    if (currentStroke != null && currGestureMode == GestureMode.Scale) {
                        Log.d("inside move 44 scale", currGestureMode.toString());
                        //scale shape
                        currScaleDist = spacingScale(event);
                        float scaleIndex = currScaleDist/prevScaleDist;
                        if (currentStroke instanceof Circle) {
                            Log.d("inside scale 44 Circle"," " + scaleIndex);
                            ((Circle) currentStroke).updateWithScale(scaleIndex);
                        } else if (currentStroke instanceof Rectangle) {
                            Log.d("inside scale 44 Circle"," " + scaleIndex);
                            float updatedH = ((Rectangle) currentStroke).getRect().height() * scaleIndex;
                            float updatedW = ((Rectangle) currentStroke).getRect().width() * scaleIndex;
                            //TODO: This isn't going to work. Call an update method that updates the height
                            // and width.
                            ((Rectangle)currentStroke).updateHeightWidth(updatedH, updatedW);
                        }invalidate();
                    }

                }

                if (event.getPointerCount() >= 2) {
                    tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                    if (tappedStroke != null) {
                        Log.d("inside Hold Touch Mode", tappedStroke.toString());
                    }
                }
                if (currGestureMode == GestureMode.Drag) {
                }
                if (event.getPointerCount() == 4) {
                    currGestureMode = GestureMode.Rotate;
                    Log.d("inside move 3 rorate", currGestureMode.toString());
                }
                if (event.getPointerCount() == 5) {
                    currGestureMode = GestureMode.Scale;
                    Log.d("inside move 5 scale", currGestureMode.toString());
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    upTouch(x, y);
                } else if (currTouchMode == TouchMode.PerfectionWait) {
                    upTouch(x, y);
                    currTouchMode = TouchMode.SingleFingerDraw;
                } else {
                    currTouchMode = TouchMode.SingleFingerDraw;
                    if (currentStroke != null) {
                        drawCanvas.drawPath(currentStroke.getDrawPath(), drawPaint);
                        strokes.add(currentStroke);
                    }
                    currentStroke = null;
                }
                resetPaint();
                invalidate();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                upTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (currGestureMode != null) {
                    Log.d("number of Pointer Down", currGestureMode.toString() + event.getPointerCount());
                }
                if (currTouchMode != null) {
                    Log.d("number of Pointer Down", currTouchMode.toString() + event.getPointerCount());
                }
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    currTouchMode = TouchMode.TwoFingerWait;
                    mLastFingerDown = System.currentTimeMillis();
                    currentStroke = null;
                } else if (currTouchMode == TouchMode.OneFingerWait) {
                    // Attempt to make perfect stroke
                    currentStroke = new PerfectStroke();
                    currentStroke.startStroke(mLastX, mLastY);
                    currTouchMode = TouchMode.Perfection;
                    invalidate();
                } else if (currTouchMode == TouchMode.PerfectionWait) {
                    ((PerfectStroke)currentStroke).newStroke();
                    currTouchMode = TouchMode.Perfection;
                    invalidate();
                }
                // TODO: (Also, adjust these based off of their states? Instead of just pointers.)
                //TODO: (Lauren) Add in change color / brush size here.
                if (event.getPointerCount() == 2) {
                    currGestureMode = GestureMode.Drag;
                }
                else if (event.getPointerCount() == 3) {
                    currGestureMode = GestureMode.Clone;
                }
                else if (event.getPointerCount() == 4) {
                    currGestureMode = GestureMode.Rotate;
                    prevSwipeY1 = event.getY(0);
                    prevSwipeY2 = event.getY(1);
                    prevSwipeY3 = event.getY(2);
                    prevSwipeY4 = event.getY(3);
                }
                else if (event.getPointerCount() == 5) {
                    currGestureMode = GestureMode.Scale;
                    prevScaleDist = spacingScale(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (currTouchMode == TouchMode.Perfection) {
                    currTouchMode = TouchMode.PerfectionWait;
                }
                if (currGestureMode == GestureMode.Clone) {
                    Log.d("action up", "inside clone");
                    if (clonedCircle != null) {
                        drawCanvas.drawPath(clonedCircle.getDrawPath(),drawPaint);
                        strokes.add(clonedCircle);
                        clonedCircle = null;
                    } else if (clonedRect != null) {
                        drawCanvas.drawPath(clonedRect.getDrawPath(), drawPaint);
                        strokes.add(clonedRect);
                        clonedRect = null;
                    }
                    invalidate();
                }
                break;
            default:
                break;
        }

        mGestureDetector.onTouchEvent(event);
        mLastX = x;
        mLastY = y;
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
            currentDrawMode = prevDrawMode;
            drawPaint.setXfermode(null);
            drawPaint.setColor(paintColor);
        }
    }

    public void setRect(boolean isRect) {
        if (isRect) {
            prevDrawMode = currentDrawMode;
            currentDrawMode = DrawMode.Rectangle;

        } else {
            currentDrawMode = prevDrawMode;
        }
    }

    public void setCircle(boolean isCircle) {
        if (isCircle) {
            prevDrawMode = currentDrawMode;
            currentDrawMode = DrawMode.Circle;

        } else {
            currentDrawMode = prevDrawMode;
        }
    }

    public void setLine(boolean isLine) {
        if (isLine) {
            prevDrawMode = currentDrawMode;
            currentDrawMode = DrawMode.Line;

        } else {
            currentDrawMode = prevDrawMode;
        }
    }

    //calculate the degree to be rotated by
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_x, delta_y);
        return (float) Math.toDegrees(radians);
    }

    private float spacingScale(MotionEvent event) {
        if (event.getPointerCount() >= 5) {
            float x1 = event.getX(2);
            float y1 = event.getY(2);
            float x2 = event.getX(3);
            float y2 = event.getY(3);
            float x3 = event.getX(4);
            float y3 = event.getY(4);
            float offsetx1 = x1 - x2;
            float offsety1 = y1 - y2;
            float offsetx2 = x2 - x3;
            float offsety2 = y2 - y3;
            float offsetx3 = x3 - x1;
            float offsety3 = y3 - y1;
            float currDist = (float) Math.sqrt(offsetx1 * offsetx1 + offsety1 * offsety1 +
            offsetx2 * offsetx2 + offsety2 * offsety2 + offsetx3 * offsetx3 + offsety3 * offsety3);
            return currDist;
        }
        return (float)1.0;
    }

    //determine the space between the first two fingers
    private float spacingOneTwo(MotionEvent event) {
        float x = event.getX(0) - event.getY(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    //determine the midPoint of the the 3, 4 fingers
    private void midPointThirdFlour(PointF point, MotionEvent event) {
        float x = event.getX(2) + event.getY(3);
        float y = event.getY(2) + event.getY(3);
        point.set(x/2, y/2);
    }

    private void midPointOneTwo(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getY(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x/2, y/2);
    }

    private int findNearestStrokeIndex(float x1, float y1, float x2, float y2) {
        float nearestDistance = MAXIMUM_DRAG_DISTANCE;
        int nearestStrokeIndex = -1;
        // Give priority to strokes drawn most recently.
        for (int i = strokes.size() - 1; i >= 0; i--) {
            float currDistance = strokes.get(i).distanceFromTap(x1, y1, x2, y2);
            if (currDistance < nearestDistance) {
                nearestDistance = currDistance;
                nearestStrokeIndex = i;
            };
        }

        return nearestStrokeIndex;
    }

    private Stroke popNearestStroke(float x1, float y1, float x2, float y2) {
        int index = findNearestStrokeIndex(x1, y1, x2, y2);
        if (index < 0) {
            return null;
        }

        Stroke str = strokes.remove(index);
        resetCanvas();
        drawPaint.setColor(str.getColor());
        drawPaint.setStrokeWidth(str.getSize());
        if (str instanceof DrawShape && ((DrawShape)str).getIsFilled()) {
            drawPaint.setStyle(Paint.Style.FILL);
            drawPaint.setAlpha(((DrawShape) str).getTransparency());
        }
        return str;
    }

    private void resetCanvas() {
        drawCanvas.drawColor(bgColor);
        for (Stroke str : strokes) {
            drawPaint.setColor(str.getColor());
            drawPaint.setStrokeWidth(str.getSize());
            if (str instanceof DrawShape && ((DrawShape)str).getIsFilled()) {
                drawPaint.setStyle(Paint.Style.FILL);
                drawPaint.setAlpha(((DrawShape) str).getTransparency());
            } else {
                drawPaint.setStyle(Paint.Style.STROKE);
                drawPaint.setAlpha(255);
            }
            drawStrokeOnCanvas(str);
        }

        drawPaint.setColor(paintColor);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setAlpha(255);
    }

    private void drawStrokeOnCanvas(Stroke str) {
        drawCanvas.drawPath(str.getDrawPath(), drawPaint);
    }

    public Stroke getTappedShape(float x1, float y1, float x2, float y2) {
        for (int i = strokes.size() - 1; i >= 0; i--) {
            Stroke thisStroke = strokes.get(i);
            if (thisStroke.containsTap(x1, y1, x2, y2)) {
                Log.d("inside getTapped",thisStroke.toString());
                return thisStroke;
            };
        }
        return null;
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
            if (currTouchMode == TouchMode.TwoFingerWait
                    && e2.getPointerCount() == 2) {
                // Clamp values between 0 -> width / height.
              /*  mScrollX = Math.min(Math.max(mScrollX + distanceX, 0), width - getWidth());
                mScrollY = Math.min(Math.max(mScrollY + distanceY, 0), height - getHeight());
                invalidate();*/
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (e.getPointerCount() == 1) {
                currTouchMode = TouchMode.OneFingerWait;
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (currTouchMode == TouchMode.PerfectionWait) {
            }
            return false;
        }
    }

    private double distanceFromLastPoint(float x, float y) {
        return Math.sqrt(Math.pow(x - mLastX, 2) + Math.pow(y - mLastY, 2));
    }

    private void resetPaint() {
        drawPaint.setColor(paintColor);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle((Paint.Style.STROKE));
        drawPaint.setAlpha(255);
    }
}
