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
    private boolean EAT_POINTER_INPUT = false;
    private int EAT_COUNT = 0;
    private float prevScaleDist;
    private float currScaleDist;
    private Stroke tappedStroke;
    private Circle clonedCircle;
    private Rectangle clonedRect;


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
                    canvas.drawRect(r.getRect(), drawPaint);
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
                    drawCanvas.drawRect(r.getRect(), drawPaint);
                    break;

            }

            strokes.add(currentStroke);
            currentStroke = null;
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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currTouchMode == TouchMode.TwoFingerWait) {
                    if (System.currentTimeMillis() - mLastFingerDown > getLongPressTimeout()) {
                        currTouchMode = TouchMode.Hold;
                        //java.lang.IllegalArgumentException: pointerIndex out of range
                        if (event.getPointerCount() >= 2) {
                            currentStroke = popNearestStroke(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        }
                        if (currentStroke != null) {
                            currentStroke.startMove(x, y);
                            invalidate();
                        }
                    }
                } else if (currTouchMode == TouchMode.SingleFingerDraw){
                    moveTouch(x, y);
                } else if (currTouchMode == TouchMode.Hold) {
                    Log.d("inside move hold 1", currGestureMode.toString());
                    //add a condition here
                    if (currentStroke != null && currGestureMode == GestureMode.Drag) {
                        currentStroke.move(x, y);
                    }
                    Log.d(currTouchMode.toString(),currGestureMode.toString());
                    invalidate();
                }
                if (event.getPointerCount() >= 2) {
                    tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                    if (tappedStroke != null) {
                        Log.d("inside Hold Touch Mode", tappedStroke.toString());
                    }
                }
                if (false && currGestureMode == GestureMode.Drag) {
                    Log.d("inside move 2 drag", currGestureMode.toString());
                    if (event.getPointerCount() == 2) {
                        float dx = (event.getX(0) + event.getX(1))/2;
                        float dy = (event.getY(0) + event.getY(1))/2;
                        tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        if (tappedStroke != null) {
                            tappedStroke.move(dx, dy);
                            if (tappedStroke instanceof Circle) {
                                Circle tappedCircle = (Circle) tappedStroke;
                                drawCanvas.drawCircle(tappedCircle.getX(), tappedCircle.getY(),
                                        tappedCircle.getRadius(), drawPaint);
                                invalidate();
                            } else if (tappedStroke instanceof Rectangle) {
                                Rectangle tappedR = (Rectangle) tappedStroke;
                                drawCanvas.drawRect(tappedR.getRect(),drawPaint);
                                invalidate();
                            }
                        }
                    }
                } else if (currGestureMode == GestureMode.Clone) {
                    Log.d("inside move 3 clone", currGestureMode.toString());
                    if (event.getPointerCount() == 3 ) {
                        float cloneX = event.getX(2);
                        float cloneY = event.getY(2);
                        tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        if (tappedStroke != null) {
                            if (tappedStroke instanceof Circle) {
                                Circle tappedCircle = (Circle) tappedStroke;
                                clonedCircle = tappedCircle;
                                drawCanvas.drawCircle(clonedCircle.getX(), clonedCircle.getY(), clonedCircle.getRadius(), drawPaint);
                                clonedCircle.move(cloneX, cloneY);
                                invalidate();
                            } else if (tappedStroke instanceof Rectangle) {
                                Rectangle tappedRect = (Rectangle) tappedStroke;
                                clonedRect = tappedRect;
                                drawCanvas.drawRect(clonedRect.getRect(), drawPaint);
                                clonedRect.move(cloneX, cloneY);
                                invalidate();
                            }
                        }
                    }
                } else if (currGestureMode == GestureMode.Rotate) {
                    Log.d("inside move 3 rorate", currGestureMode.toString());
                    if (event.getPointerCount() == 4) {
                        tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        if (tappedStroke != null) {
                            float rotateDegree = rotation(event);
                            if (tappedStroke instanceof Circle) {
                                Circle tappedCircle = (Circle) tappedStroke;
                                drawCanvas.save();
                                drawCanvas.rotate(rotateDegree);
                                drawCanvas.drawCircle(tappedCircle.getX(), tappedCircle.getY(),
                                        tappedCircle.getRadius(),drawPaint);
                                drawCanvas.restore();
                                break;
                            } else if (tappedStroke instanceof Rectangle) {
                                Rectangle tappedRect = (Rectangle) tappedStroke;
                                drawCanvas.save();
                                drawCanvas.rotate(rotateDegree);
                                drawCanvas.drawRect(tappedRect.getRect(), drawPaint);
                                drawCanvas.restore();
                            }

                        }
                    }

                } else if (currGestureMode == GestureMode.Scale) {
                    Log.d("inside move 4 scale", currGestureMode.toString());
                    //scale shape
                    currScaleDist = spacingScale(event);
                    float scaleIndex = currScaleDist/prevScaleDist;
                    if (currentStroke instanceof Circle) {
                        ((Circle) currentStroke).updateWithScale(scaleIndex);
                    } else if (currentStroke instanceof Rectangle) {
                        float updatedH = ((Rectangle) currentStroke).getRect().height() * scaleIndex;
                        float updatedW = ((Rectangle) currentStroke).getRect().width() * scaleIndex;
                        //TODO: This isn't going to work. Call an update method that updates the height
                        // and width.
                        ((Rectangle)currentStroke).updateHeightWidth(updatedH, updatedW);
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    upTouch(x, y);
                } else {
                    currTouchMode = TouchMode.SingleFingerDraw;
                    drawPaint.setStrokeWidth(brushSize);
                    if (currentStroke != null) {
                        if (currentStroke instanceof  DrawPath) {
                            drawCanvas.drawPath(((DrawPath) currentStroke).getDrawPath(), drawPaint);
                        } else if (currentStroke instanceof Circle) {
                            Circle c = (Circle) currentStroke;
                            drawCanvas.drawCircle(c.getX(),c.getY(),c.getRadius(),drawPaint);
                        } else if (currentStroke instanceof Rectangle) {
                            Rectangle r = (Rectangle) currentStroke;
                            drawCanvas.drawRect(r.getRect(), drawPaint);
                        }
                        strokes.add(currentStroke);
                    }
                    currentStroke = null;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_OUTSIDE:
                upTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (currTouchMode == TouchMode.SingleFingerDraw) {
                    currTouchMode = TouchMode.TwoFingerWait;
                    mLastFingerDown = System.currentTimeMillis();
                    currentStroke = null;
                }
                if (event.getPointerCount() == 2) {
                    currGestureMode = GestureMode.Drag;
                }
                if (event.getPointerCount() == 3) {
                    currGestureMode = GestureMode.Clone;
                }
                if (event.getPointerCount() == 4) {
                    currGestureMode = GestureMode.Rotate;
                }
                if (event.getPointerCount() == 5) {
                    currGestureMode = GestureMode.Scale;
                    prevScaleDist = spacingScale(event);
                    //###

                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (currGestureMode == GestureMode.Clone) {
                    Log.d("action up", "inside clone");
                    if (clonedCircle != null) {
                        drawCanvas.drawCircle(clonedCircle.getX(),clonedCircle.getY(),clonedCircle.getRadius(),drawPaint);
                        strokes.add(clonedCircle);
                        clonedCircle = null;
                    } else if (clonedRect != null) {
                        drawCanvas.drawRect(clonedRect.getRect(), drawPaint);
                        strokes.add(clonedRect);
                        clonedRect = null;
                    }
                    invalidate();
                    break;
                }


                break;
            default:
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
        double delta_x = (event.getX(2) - event.getX(3));
        double delta_y = (event.getY(2) - event.getY(3));
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



    public void EatInput() {
        EAT_POINTER_INPUT = true;
        EAT_COUNT++;
        currentStroke = null;
    }

    private int findNearestStrokeIndex(float x1, float y1, float x2, float y2) {
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

        return nearestStrokeIndex;
    }

    private Stroke popNearestStroke(float x1, float y1, float x2, float y2) {
        int index = findNearestStrokeIndex(x1, y1, x2, y2);
        if (index < 0) {
            return null;
        }

        Stroke str = strokes.remove(index);
        resetCanvas();
        return str;
    }

    private void resetCanvas() {
        drawCanvas.drawColor(bgColor);
        for (Stroke str : strokes) {
            drawStrokeOnCanvas(str);
        }
    }

    private void drawStrokeOnCanvas(Stroke str) {
        if (str instanceof DrawPath) {
            drawCanvas.drawPath(((DrawPath) str).getDrawPath(), drawPaint);
        } else if (str instanceof Circle) {
            Circle c = (Circle) str;
            drawCanvas.drawCircle(c.getX(), c.getY(), c.getRadius(), drawPaint);
        } else if (str instanceof Rectangle) {
            Rectangle r = (Rectangle) str;
            drawCanvas.drawRect(r.getRect(), drawPaint);
        }
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
