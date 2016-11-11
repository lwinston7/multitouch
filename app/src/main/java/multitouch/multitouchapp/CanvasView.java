package multitouch.multitouchapp;

/**
 * Created by michelleliu on 10/26/16.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.graphics.PointF;

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
    private float prevScaleDist;
    private float currScaleDist;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();


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
        drawPath = null;
        currentStroke = null;
        currentDrawMode = DrawMode.Line;
        setErase(false);
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
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (currTouchMode == TouchMode.TwoFingerWait) {
                    if (System.currentTimeMillis() - mLastFingerDown > getLongPressTimeout()) {
                        currTouchMode = TouchMode.Hold;
                        //java.lang.IllegalArgumentException: pointerIndex out of range
                        if (event.getPointerCount() >= 2) {
                            currentStroke = findNearestStroke(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                            currentStroke.startMove(x, y);
                        }
                        //drawPaint.setStrokeWidth(60);
                        setErase(true);
                        //java.lang.ClassCastException: multitouch.multitouchapp.Rectangle cannot be cast to multitouch.multitouchapp.DrawPath
                        if (currentStroke instanceof DrawPath) {
                            drawCanvas.drawPath(((DrawPath) currentStroke).getDrawPath(), drawPaint);
                        } else if (currentStroke instanceof Circle) {
                            Circle c = (Circle) currentStroke;
                            drawCanvas.drawCircle(c.getX(),c.getY(),c.getRadius(), drawPaint);
                        } else if (currentStroke instanceof Rectangle) {
                            Rectangle r = (Rectangle) currentStroke;
                            drawCanvas.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), drawPaint);
                        }
                        setErase(false);
                        // TODO: Remove stroke from arraylist as well.
                    }

                    if (currGestureMode == GestureMode.Drag) {
                        if (event.getPointerCount() == 2) {
                            float dx = (event.getX(0) + event.getX(1))/2;
                            float dy = (event.getY(0) + event.getY(1))/2;
                            Stroke tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                            if (tappedStroke != null) {
                                tappedStroke.startMove(dx, dy);
                                if (tappedStroke instanceof Circle) {
                                    Circle tappedCicle = (Circle) tappedStroke;
                                    drawCanvas.drawCircle(tappedCicle.getX(), tappedCicle.getY(),
                                            tappedCicle.getRadius(), drawPaint);
                                    break;
                                } else if (tappedStroke instanceof Rectangle) {
                                    Rectangle tappedR = (Rectangle) tappedStroke;
                                    drawCanvas.drawRect(tappedR.getX(), tappedR.getY(),
                                            tappedR.getWidth(), tappedR.getWidth(),drawPaint);
                                    break;
                                }
                            }
                        }
                    } else if (currGestureMode == GestureMode.Clone) {
                        if (event.getPointerCount() == 3 ) {
                            float cloneX = event.getX(2);
                            float cloneY = event.getY(2);

                            Stroke tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                            if (tappedStroke != null) {
                                if (tappedStroke instanceof Circle) {
                                    Circle tappedCircle = (Circle) tappedStroke;
                                    Circle clonedCircle = tappedCircle;
                                    clonedCircle.move(cloneX, cloneY);
                                    strokes.add(clonedCircle);
                                    drawCanvas.drawCircle(clonedCircle.getX(), clonedCircle.getY(), clonedCircle.getRadius(), drawPaint);
                                    break;
                                } else if (tappedStroke instanceof Rectangle) {
                                    Rectangle tappedRect = (Rectangle) tappedStroke;
                                    Rectangle clonedRect = tappedRect;
                                    clonedRect.move(cloneX, cloneY);
                                    strokes.add(clonedRect);
                                    drawCanvas.drawRect(clonedRect.getX(), clonedRect.getY(),
                                            clonedRect.getWidth(),clonedRect.getHeight(),drawPaint);
                                    break;
                                }
                            }
                        }
                    } else if (currGestureMode == GestureMode.Rotate) {
                        if (event.getPointerCount() == 4) {
                            Stroke tappedStroke = getTappedShape(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
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
                                    drawCanvas.drawRect(tappedRect.getX(),tappedRect.getY(),
                                            tappedRect.getWidth(), tappedRect.getHeight(),drawPaint);
                                    drawCanvas.restore();
                                }

                            }
                        }

                    } else if (currGestureMode == GestureMode.Scale) {
                        //scale shape
                        currScaleDist = spacingScale(event);
                        float scaleIndex = currScaleDist/prevScaleDist;
                        if (currentStroke instanceof Circle) {
                            ((Circle) currentStroke).updateWithScale(scaleIndex);
                        } else if (currentStroke instanceof Rectangle) {
                            float updatedH = ((Rectangle) currentStroke).getHeight() * scaleIndex;
                            float updatedW = ((Rectangle) currentStroke).getWidth() * scaleIndex;
                            currentStroke.update(updatedW, updatedH);
                        }
                    }

                } else if (currTouchMode == TouchMode.SingleFingerDraw){
                    moveTouch(x, y);
                } else if (currTouchMode == TouchMode.Hold) {
                    currentStroke.move(x, y);
                    invalidate();
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
                            drawCanvas.drawRect(r.getX(),r.getY(),r.getWidth(),r.getHeight(), drawPaint);
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
            return strokes.remove(nearestStrokeIndex);
        } else {
            return null;
        }
    }

    public Stroke getTappedShape(float x1, float y1, float x2, float y2) {
        for (int i = strokes.size() - 1; i >= 0; i--) {
            Stroke thisStroke = strokes.get(i);
            if (thisStroke.containsTap(x1, y1, x2, y2)) {
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
