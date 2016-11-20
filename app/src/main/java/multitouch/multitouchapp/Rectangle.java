package multitouch.multitouchapp;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Created by Lauren on 11/6/2016.
 */

public class Rectangle extends DrawShape{
    private RectF rect;
    private float left, top;
    private float moveX, moveY;

    public Rectangle() {}
    public Rectangle(RectF rect, float left, float top) {
        this.rect = rect;
        this.left = left;
        this.top = top;
    }

    @Override
    public void startStroke(float x, float y) {
        rect = new RectF(x,y,x,y);
        this.left = x;
        this.top = y;
    }

    @Override
    public void update(float right, float bottom) {
        float actualLeft = left;
        float actualTop = top;
        float actualRight = right;
        float actualBottom = bottom;
        if (right < left) {
            actualLeft = actualRight;
            actualRight = left;
        }

        if (bottom < top) {
            actualTop = actualBottom;
            actualBottom = top;
        }

        rect.set(actualLeft, actualTop, actualRight,actualBottom);
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public Path getDrawPath() {
        Path drawPath = new Path();
        drawPath.moveTo(rect.centerX(), rect.centerY());
        drawPath.addRect(rect.left,rect.top,rect.right,rect.bottom, Path.Direction.CW);
        return drawPath;
    }

    @Override
    //move- track
    public void move(float x1, float y1) {
        rect.offset((x1 - moveX), (y1 - moveY));
        moveX = x1;
        moveY = y1;
    }

    @Override
    //start move - starting point
    public void startMove(float x1, float y1) {
        moveX = x1;
        moveY = y1;
        p0Past = null;
        p1Past = null;
    }

    @Override
    public boolean isStrayStroke() {
        return rect.width() < FINGER_PIXELS || rect.height() < FINGER_PIXELS;
    }

    @Override
    public Stroke clone() {
        Rectangle r = new Rectangle();
        r.startStroke(left, top);
        r.update(left + rect.width(), top + rect.height());
        r.startMove(rect.centerX(), rect.centerY());
        r.move(moveX, moveY);
        r.set(mColor, mSize, mIsFilled, mTransparency);
        return r;
    }

    @Override
    public void meteredShift(float x, float y) {

    }

    public RectF getRect() {
        return rect;
    }

    @Override
    public void updateWithScale(float scaleIndex) {
        float h = rect.height() * scaleIndex;
        float w = rect.width() * scaleIndex;
        float right = left + w;
        float bottom = top + h;
        update(right, bottom);
    }

    public void updateHeightWidth(float h, float w) {
        float right = left + w;
        float bottom = top + h;
        update(right, bottom);
    }

    public void updateWithRotation(float degrees, float px, float py) {
        RectF rectF = new RectF(rect);
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(degrees, px, py);
        float[] points = new float[4];
        points[0] = rectF.left;
        points[1] = rectF.top;
        points[2] = rectF.right;
        points[3] = rectF.bottom;
        //rotationMatrix.mapRect(rectF);
        rotationMatrix.mapPoints(points);
        rect.set(points[0],points[1],points[2],points[3]);
    }

    public float getCenterX() {
        return rect.centerX();
    }
    public float getCenterY() {
        return rect.centerY();
    }

}
