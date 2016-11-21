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
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.PointF;
import android.util.Log;

import static android.view.ViewConfiguration.getLongPressTimeout;

public class CanvasView extends View {

    private final int MAXIMUM_DRAG_DISTANCE = 1000;
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
    private int selectionColor = Color.BLACK;
    private int secondarySelectionColor = Color.GRAY;
    private float mX, mY;
    private float brushSize, lastBrushSize;
    private int bgColor = Color.WHITE;
    private MultitouchGestureDetector mGestureDetector;
    private MultitouchGestureDetector tapGestureDetector;
    private static final float TOLERANCE = 5;
    private float prevScaleDist;
    private float currScaleDist;
    private float prevRotateDegree;
    private float currRotateDegree;
    private Stroke tappedStroke;
    private Stroke clonedStroke;
    private float prevSwipeY1, prevSwipeY2, prevSwipeY3, prevSwipeY4;
    private float lastSwipeY1, lastSwipeY2, lastSwipeY3, lastSwipeY4;

    private float mScrollX = 0;
    private float mScrollY = 0;

    private float mLastX = 0;
    private float mLastY = 0;

    private int mLastTransparency = 0;

    private int width, height;
    private boolean isCloned;
    private int mActivePointer1Id = -10;
    private int tapClickCount;
    private long tapStartTime, tapDuration;
    static final long HALF_SECOND = 500;

    private static final float SWIPE_TOLERANCE = 50;

    private Timer longPressTimer;

    private boolean isRotated;

    private enum DrawMode {
        Line,
        Circle,
        Rectangle,
        Erase
    }

    private DrawMode currentDrawMode = DrawMode.Line;
    private DrawMode prevDrawMode = DrawMode.Line;


    /**
     * SingleFingerDraw -> _pointer_down_ -> TwoFingerWait -> _enough_time_ -> PerfectionWait -> _pointer_down_ -> Perfection
     * SingleFingerDraw -> _minimal_movement_ -> OneFingerWait -> _enough_time_ -> Drag
     * OneFingerWait / Drag -> _pointer_down_on_dragged_object -> Cloning
     * OneFingerWait / Drag -> _pointer_down_outside_of_dragged_object -> RotateResize
     */
    private enum TouchMode {
        SingleFingerDraw, // 1-Finger Touch
        OneFingerWait,
        Drag, // 1-Finger Drag
        TwoFingerWait, // 2-Finger Wait for long press
        PerfectionWait, // Wait for the 3rd pointer down
        Perfection, // Creating a new perfect stroke
        TwoFingerUp, // Not scrolling. Scroll again when we put pointers back down.
        TwoFingerReady, // Ready for scrolling
        PerfectionToggle,
        Cloning,
        RotateResize,
        MiniShift,
        FinishedGesture //Don't allow anymore gestures until we remove all fingers from the screen.
    }

    private TouchMode currTouchMode = TouchMode.SingleFingerDraw;
    private float mLastFingerDown = 0;

    private enum GestureMode {
        Swipe,
        DoubleTap;
    }

    private GestureMode currGestureMode = null;


    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;
        setupDrawing();
        mGestureDetector = new MultitouchGestureDetector(context, new DrawingGestureListener());
        //tapGestureDetector = new GestureDetector(context,new DoubleTapGestureListener());
    }

    public void setupDrawing() {
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
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
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
        if (currentStroke != null && currentDrawMode != DrawMode.Erase && currentStroke.getDrawPath() != null) {
            if (currTouchMode != TouchMode.SingleFingerDraw && currTouchMode != TouchMode.Perfection && currTouchMode != TouchMode.PerfectionToggle) {
                drawPaint.setStyle(Paint.Style.STROKE);
                if (paintColor != selectionColor) {
                    drawPaint.setColor(selectionColor);
                } else {
                    drawPaint.setColor(secondarySelectionColor);
                }
                drawPaint.setStrokeWidth(brushSize * 1.3f);
                canvas.drawPath(currentStroke.getDrawPath(), drawPaint);
                drawPaint.setColor(paintColor);
                drawPaint.setStrokeWidth(brushSize);
            }
            drawPaint.setColor(currentStroke.getColor());
            drawPaint.setStrokeWidth(currentStroke.getSize());
            // TODO: Highlight the currently selected or tapped stroke.
            if (currentStroke instanceof DrawShape && ((DrawShape) currentStroke).getRotation() > 0) {
                canvas.save();
                canvas.rotate(((DrawShape) currentStroke).getRotation());
                canvas.drawPath(currentStroke.getDrawPath(), drawPaint);
                canvas.restore();
            } else {
                if (currentStroke instanceof DrawShape && ((DrawShape) currentStroke).getIsFilled()) {
                    drawPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(currentStroke.getDrawPath(), drawPaint);
                    drawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    drawPaint.setAlpha(((DrawShape) currentStroke).getTransparency());
                }
                canvas.drawPath(currentStroke.getDrawPath(), drawPaint);
            }
        }

        if (clonedStroke != null && currentDrawMode != DrawMode.Erase) {
            canvas.drawPath(clonedStroke.getDrawPath(), drawPaint);
        }
    }

    private void startTouch(float x, float y) {
        switch (currentDrawMode) {
            case Line:
                currentStroke = new DrawPath();
                currentStroke.startStroke(x, y);
                break;
            case Circle:
                currentStroke = new Circle(false);
                currentStroke.startStroke(x, y);
                break;
            case Erase:
                startPath(x, y);
                break;
            case Rectangle:
                currentStroke = new Rectangle(false);
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
        erasePath.moveTo(x, y);
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
        } else if (currentStroke != null) {
            currentStroke.finishStroke(x, y);
            Path p = currentStroke.getDrawPath();
            if (p != null) {
                drawCanvas.drawPath(p, drawPaint);
                if (currentStroke instanceof PerfectStroke) {
                    Stroke newStroke = ((PerfectStroke) currentStroke).getPerfectStroke();
                    newStroke.setSize(brushSize);
                    newStroke.setColor(paintColor);
                    strokes.add(newStroke);
                } else {
                    strokes.add(currentStroke);
                    currentStroke = null;
                }
            }
        }
    }

    private void upTouch() {
        if (currentStroke != null) {
            if (currentStroke instanceof DrawShape && ((DrawShape) currentStroke).getIsFilled()) {
                drawPaint.setStyle(Paint.Style.STROKE);
                drawPaint.setAlpha(255);
                drawCanvas.drawPath(currentStroke.getDrawPath(), drawPaint);
                drawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                drawPaint.setAlpha(((DrawShape) currentStroke).getTransparency());
            }
            drawCanvas.drawPath(currentStroke.getDrawPath(), drawPaint);
            if (currentStroke instanceof PerfectStroke) {
                Stroke newStroke = ((PerfectStroke) currentStroke).getPerfectStroke();
                newStroke.setSize(brushSize);
                newStroke.setColor(paintColor);
                strokes.add(newStroke);
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
    public boolean onTouchEvent(final MotionEvent event) {
        //Draw normally
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                mActivePointer1Id = event.getPointerId(0);
                isRotated = false;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currTouchMode == TouchMode.OneFingerWait) {
                    ((MainActivity) context).updateGestureText("Move finger to drag shape. Tap outside to rotate or size. Tap inside to clone.");
                    currTouchMode = TouchMode.Drag;
                }

                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    if (currentStroke != null) {
                        moveTouch(x, y);
                        invalidate();
                    }
                } else if (currTouchMode == TouchMode.Perfection) {
                    int actionIndex = event.getPointerCount() - 1;
                    moveTouch(event.getX(actionIndex), event.getY(actionIndex));
                    invalidate();
                } else if (currTouchMode == TouchMode.Cloning) {
                    if (!isCloned && currentStroke != null) {
                        clonedStroke = currentStroke.clone();
                        upTouch();
                        isCloned = true;
                    } else {
                        float cloneX = event.getX(event.getPointerCount() - 1);
                        float cloneY = event.getY(event.getPointerCount() - 1);
                        clonedStroke.move(cloneX, cloneY);
                    }
                    invalidate();
                } else if (currTouchMode == TouchMode.Drag) {
                    if (currentStroke != null) {
                        currentStroke.move(x, y);
                        invalidate();
                    }
                } else if (currTouchMode == TouchMode.RotateResize) {
                    currScaleDist = spacingScale(event);
                    float scaleIndex = currScaleDist / prevScaleDist;
                    if (currentStroke instanceof DrawShape) {
                        ((DrawShape) currentStroke).updateWithScale(scaleIndex);
                    }
                    prevScaleDist = currScaleDist;
                    if (currentStroke instanceof DrawShape) {
                        if (event.getPointerCount() == 3) {
                            float rectcx = ((Rectangle) currentStroke).getCenterX();
                            float rectcy = ((Rectangle) currentStroke).getCenterY();
                            currRotateDegree = rotation(event);
                            float rotateDegree = currRotateDegree - prevRotateDegree;
                            //((Rectangle) currentStroke).updateWithRotation(rotateDegree,rectcx,rectcy);
                            prevRotateDegree = currRotateDegree;
                            invalidate();
                        }
                        // ((DrawShape) currentStroke).setRotation(rotateDegree);
                    }

                    invalidate();
                } else if (currTouchMode == TouchMode.TwoFingerReady) {
                    if (event.getPointerCount() <= 3) {
                        PointF p1 = null;
                        PointF p2 = null;
                        PointF p0 = new PointF(event.getX(0), event.getY(0));
                        if (event.getPointerCount() > 1) {
                            p1 = new PointF(event.getX(1), event.getY(1));
                        }

                        if (event.getPointerCount() > 2) {
                            p2 = new PointF(event.getX(2), event.getY(2));
                        }
                        currentStroke.adjustColor(p0, p1, p2);

                        invalidate();
                    } else {
                        currentStroke.setTransparency(mLastTransparency);
                        invalidate();
                    }
                }

                if (event.getPointerCount() == 4) {
                    lastSwipeY1 = event.getY(0);
                    lastSwipeY2 = event.getY(1);
                    lastSwipeY3 = event.getY(2);
                    lastSwipeY4 = event.getY(3);
                    if ((lastSwipeY1 > prevSwipeY1) && (lastSwipeY2 > prevSwipeY2) &&
                            (lastSwipeY3 > prevSwipeY3) && (lastSwipeY4 > prevSwipeY4)) {
                        currGestureMode = GestureMode.Swipe;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    upTouch(x, y);
                } else {
                    upTouch();
                    currTouchMode = TouchMode.SingleFingerDraw;
                    currentStroke = null;
                }
                ((MainActivity) context).updateGestureText("Touch to draw. Double tap stroke to delete or hold with one or two fingers to modify. Swipe down with four fingers to clear entire canvas.");
                tapClickCount++;
                if (tapClickCount == 1) {
                    tapStartTime = System.currentTimeMillis();
                } else if (tapClickCount == 2) {
                    tapDuration = System.currentTimeMillis() - tapStartTime;
                    if (tapDuration <= HALF_SECOND) {
                        float tapX = event.getX(0);
                        float tapY = event.getY(0);
                        tappedStroke = popNearestStroke(tapX, tapY);
                        tapClickCount = 0;
                        tapDuration = 0;
                    } else {
                        tapClickCount = 1;
                        tapStartTime = System.currentTimeMillis();
                    }
                }
                if (currGestureMode == GestureMode.Swipe) {
                    Log.d("inside Swipe", " " + event.getPointerCount());
                    lastSwipeY1 = event.getY(0);
                    if ((lastSwipeY1 > prevSwipeY1 + SWIPE_TOLERANCE) &&
                            (lastSwipeY2 > prevSwipeY2 + SWIPE_TOLERANCE)
                            && (lastSwipeY1 > prevSwipeY2 + SWIPE_TOLERANCE) &&
                            (lastSwipeY3 > prevSwipeY3 + SWIPE_TOLERANCE)) {
                        clearCanvas();
                        currGestureMode = null;
                    }
                }
                resetPaint();
                invalidate();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                if (currTouchMode == TouchMode.SingleFingerDraw || currTouchMode == TouchMode.Drag) {
                    upTouch(x, y);
                    invalidate();
                } else {
                    upTouch();
                    currTouchMode = TouchMode.FinishedGesture;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // TODO: Check to see if other finger has been held for a long enough time.
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    currTouchMode = TouchMode.TwoFingerWait;
                    mLastFingerDown = System.currentTimeMillis();
                    longPressTimer = new Timer();
                    longPressTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MainActivity mainActivity = (MainActivity) context;
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    checkForTwoFingerLongPress(event);
                                }
                            });
                        }
                    }, getLongPressTimeout());
                } else if (currTouchMode == TouchMode.PerfectionWait) {
                    // Attempt to make perfect stroke
                    ((PerfectStroke) currentStroke).setDragPoint(event.getX(2), event.getY(2));
                    currentStroke.startStroke((event.getX(0) + event.getX(1)) / 2f,
                            (event.getY(0) + event.getY(1)) / 2f);
                    currTouchMode = TouchMode.Perfection;
                } else if (currTouchMode == TouchMode.PerfectionToggle) {
                    ((PerfectStroke) currentStroke).newStroke();
                    currTouchMode = TouchMode.Perfection;
                    invalidate();
                } else if (currTouchMode == TouchMode.Drag) {
                    int index = event.getActionIndex();
                    PointF tapPoint = new PointF(event.getX(index), event.getY(index));
                    //currentStroke.distanceFromTap(tapPoint.x, tapPoint.y) <= MAXIMUM_DRAG_DISTANCE;
                    if (currentStroke.containsTap(tapPoint.x, tapPoint.y)) {
                        currTouchMode = TouchMode.Cloning;
                        ((MainActivity) context).updateGestureText("Drag finger to adjust position of cloned object.");
                    } else {
                        currTouchMode = TouchMode.RotateResize;
                        ((MainActivity) context).updateGestureText("Use another finger to rotate or resize.");
                        prevSwipeY1 = event.getY(0);
                        prevSwipeY2 = event.getY(1);
                        prevScaleDist = spacingScale(event);
                        if (event.getPointerCount() == 3) {
                            if (currentStroke instanceof Rectangle) {
                                float rectcx = ((Rectangle) currentStroke).getCenterX();
                                float rectcy = ((Rectangle) currentStroke).getCenterY();
                                prevRotateDegree = rotation(event);
                            }
                        }
                    }

                } else if (currTouchMode == TouchMode.RotateResize) {

                    prevScaleDist = spacingScale(event);
                    if (event.getPointerCount() == 3) {
                        //isRotated = false;
                        if (currentStroke instanceof Rectangle) {
                            float rectcx = ((Rectangle) currentStroke).getCenterX();
                            float rectcy = ((Rectangle) currentStroke).getCenterY();
                            prevRotateDegree = rotation(event);
                        }
                    }
                } else if (currTouchMode == TouchMode.TwoFingerUp) {
                    if (event.getPointerCount() >= 3) {
                        int actionIndex = event.getActionIndex();
                        PointF dragPoint = new PointF(event.getX(actionIndex), event.getY(actionIndex));
                        currentStroke.setDragPoint(dragPoint);
                        currTouchMode = TouchMode.TwoFingerReady;
                        mLastTransparency = currentStroke.getTransparency();
                        invalidate();
                    }
                } else if (currTouchMode == TouchMode.TwoFingerReady) {
                    if (event.getPointerCount() > 3) {
                        currentStroke.setTransparency(mLastTransparency);
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (currTouchMode == TouchMode.Perfection) {
                    if (event.getActionIndex() == event.getPointerCount() - 1) {
                        currTouchMode = TouchMode.PerfectionToggle;
                    }
                } else if (currTouchMode == TouchMode.PerfectionToggle) {
                    upTouch();
                    currentStroke = null;
                    currTouchMode = TouchMode.FinishedGesture;
                } else if (currTouchMode == TouchMode.Cloning) {
                    if (clonedStroke != null) {
                        drawCanvas.drawPath(clonedStroke.getDrawPath(), drawPaint);
                        strokes.add(clonedStroke);
                        clonedStroke = null;
                        isCloned = false;
                        currTouchMode = TouchMode.FinishedGesture;
                    }
                    invalidate();
                } else if (currTouchMode == TouchMode.TwoFingerUp) {
                } else if (currTouchMode == TouchMode.TwoFingerReady) {
                } else if (currTouchMode == TouchMode.RotateResize) {
                    //to do:
                    //
                    if (currentStroke != null && event.getPointerCount() == 2) {
                        upTouch();
                        currTouchMode = TouchMode.FinishedGesture;
                    }
                    if (event.getPointerCount() == 3) {
                        float rectcx = ((Rectangle) currentStroke).getCenterX();
                        float rectcy = ((Rectangle) currentStroke).getCenterY();
                        currRotateDegree = rotation(event);
                        if (!isRotated) {
                            //Log.d("inside poinger up", "rotate!!!" + currRotateDegree );
                            drawCanvas.save();
                            drawCanvas.rotate(currRotateDegree, rectcx, rectcy);
                            Stroke rotatedStroke = currentStroke;
                            drawCanvas.drawPath(rotatedStroke.getDrawPath(), drawPaint);
                            strokes.add(rotatedStroke);
                            ((Rectangle) currentStroke).updateHeightWidth(0, 0);
                            currentStroke.setColor(bgColor);
                            //strokes.remove(currentStroke);
                            drawCanvas.restore();
                            isRotated = true;
                            invalidate();
                        }
                    }
                } else {
                    currTouchMode = TouchMode.SingleFingerDraw;
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
        if (currentStroke != null) {
            currentStroke.setColor(paintColor);
        }

        invalidate();
    }

    public void setBrushSize(float newSize) {
        //update size
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize = pixelAmount;
        drawPaint.setStrokeWidth(brushSize);
        if (currentStroke != null) {
            currentStroke.setSize(brushSize);
        }
    }

    public void setLastBrushSize(float lastSize) {
        lastBrushSize = lastSize;
    }

    public float getLastBrushSize() {
        return lastBrushSize;
    }

    public void setErase(boolean isErase) {
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
        if (event.getPointerCount() >= 2) {
            double delta_x = (event.getX(1) - event.getX(0));
            double delta_y = (event.getY(1) - event.getY(0));
            double radians = Math.atan2(delta_x, delta_y);
            float degree = (float) Math.toDegrees(radians);
            //Log.d("radians",  " : " + radians);
            //Log.d("degree",  " : " + degree);
            return degree;
        }

        return 0;
    }

    private float rotation(MotionEvent event, float centerX, float centerY) {
        if (event.getPointerCount() >= 2) {
            double delta_x = (event.getX(1) - centerX);
            double delta_y = (event.getY(1) - centerY);
            double radians = Math.atan2(delta_x, delta_y);
            float degree = (float) Math.toDegrees(radians);
            Log.d("radians", " : " + radians);
            Log.d("degree", " : " + degree);
            return degree;
        }

        return 0;
    }

    private float spacingScale(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            float x1 = event.getX(0);
            float y1 = event.getY(0);
            float x2 = event.getX(1);
            float y2 = event.getY(1);
            float offsetx1 = x1 - x2;
            float offsety1 = y1 - y2;
            float currDist = (float) Math.sqrt(offsetx1 * offsetx1 + offsety1 * offsety1);
            return currDist;
        }
        return (float) 1.0;
    }

    private void checkForTwoFingerLongPress(MotionEvent event) {
        if (currTouchMode == TouchMode.TwoFingerWait) {
            longPressTimer.cancel();
            // TODO: Only work if you're not touching another stroke.
            PointF midpoint = new PointF((event.getX(0) + event.getX(1)) / 2f,
                    (event.getY() + event.getY()) / 2f);
            int pressedIndex = findNearestStrokeIndex(midpoint.x, midpoint.y);
            if (pressedIndex >= 0 &&
                    strokes.get(pressedIndex).containsTap(midpoint.x, midpoint.y)) {
                currentStroke = popNearestStroke(midpoint.x, midpoint.y);
                currTouchMode = TouchMode.TwoFingerUp;
                currentStroke.setColorAdjustmentPoints(midpoint);
                ((MainActivity) context).updateGestureText("Use another finger to adjust color.");
                invalidate();
            } else {
                currTouchMode = TouchMode.PerfectionWait;
                currentStroke = new PerfectStroke();
                currentStroke.setColor(paintColor);
                currentStroke.setSize(brushSize);
                ((MainActivity) context).updateGestureText("Use another finger to draw a perfect stroke. Tap to toggle strokes.");
            }
        }
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
        point.set(x / 2, y / 2);
    }

    private void midPointOneTwo(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getY(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private int findNearestStrokeIndex(float x, float y) {
        float nearestDistance = MAXIMUM_DRAG_DISTANCE;
        int nearestStrokeIndex = -1;
        // Give priority to strokes drawn most recently.
        for (int i = strokes.size() - 1; i >= 0; i--) {
            float currDistance = strokes.get(i).distanceFromTap(x, y);
            if (currDistance < nearestDistance) {
                nearestDistance = currDistance;
                nearestStrokeIndex = i;
            }
            ;
        }

        return nearestStrokeIndex;
    }

    private Stroke popNearestStroke(float x, float y) {
        int index = findNearestStrokeIndex(x, y);
        if (index < 0) {
            return null;
        }
        Stroke str = strokes.remove(index);
        resetCanvas();
        drawPaint.setColor(str.getColor());
        drawPaint.setStrokeWidth(str.getSize());
        if (str instanceof DrawShape && ((DrawShape) str).getIsFilled()) {
            drawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            drawPaint.setAlpha(((DrawShape) str).getTransparency());
        }
        return str;
    }

    private void resetCanvas() {
        drawCanvas.drawColor(bgColor);
        for (Stroke str : strokes) {
            drawPaint.setColor(str.getColor());
            drawPaint.setStrokeWidth(str.getSize());
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setAlpha(255);
            drawStrokeOnCanvas(str);
            if (str instanceof DrawShape && ((DrawShape) str).getIsFilled()) {
                drawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                drawPaint.setAlpha(((DrawShape) str).getTransparency());
                drawStrokeOnCanvas(str);
            }
        }

        drawPaint.setColor(paintColor);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setAlpha(255);
    }

    private void drawStrokeOnCanvas(Stroke str) {
        drawCanvas.drawPath(str.getDrawPath(), drawPaint);
    }

    public Stroke getTappedShape(float x, float y) {
        for (int i = strokes.size() - 1; i >= 0; i--) {
            Stroke thisStroke = strokes.get(i);
            // TODO: This isn't going to work if strokes are on top of one another.
            if (thisStroke.containsTap(x, y)) {
                //Log.d("inside getTapped",thisStroke.toString());
                return thisStroke;
            }
            ;
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
            int pointerShiftScale = e2.getPointerCount() - 3;
            if ((currTouchMode == TouchMode.TwoFingerReady)
                    && e2.getPointerCount() >= 4) {
                boolean isDistanceXGreater = Math.abs(distanceX) > Math.abs(distanceY);
                if (distanceX > 0 && isDistanceXGreater) {
                    currentStroke.shiftBy(-10 * pointerShiftScale, 0);
                } else if (distanceX < 0 && isDistanceXGreater) {
                    currentStroke.shiftBy(10 * pointerShiftScale, 0);
                } else if (distanceY < 0) {
                    currentStroke.shiftBy(0, 10 * pointerShiftScale);
                } else if (distanceY > 0) {
                    currentStroke.shiftBy(0, -10 * pointerShiftScale);
                }
                currTouchMode = TouchMode.TwoFingerUp;
                currentStroke.setTransparency(mLastTransparency);
                invalidate();
                return true;
            } else if (currTouchMode == TouchMode.TwoFingerReady && e2.getPointerCount() <= 3) {

                return true;
            }

            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (e.getPointerCount() == 1 && currTouchMode == TouchMode.SingleFingerDraw) {
                currTouchMode = TouchMode.OneFingerWait;
                if (e.getPointerCount() == 1) {
                    if (currentStroke != null && !currentStroke.isStrayStroke()) {
                        upTouch(e.getX(), e.getY());
                    }
                    currentStroke = popNearestStroke(e.getX(), e.getY());
                    if (currentStroke != null) {
                        currentStroke.startMove(e.getX(), e.getY());
                    } else {
                        currTouchMode = TouchMode.SingleFingerDraw;
                    }
                }

                invalidate();
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
