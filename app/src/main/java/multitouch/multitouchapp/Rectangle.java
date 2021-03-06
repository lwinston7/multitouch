package multitouch.multitouchapp;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

/**
 * Created by Lauren on 11/6/2016.
 */

public class Rectangle extends DrawShape{
    private RectF rect;
    private float left, top;
    private float moveX, moveY;
    private float width;

    public Rectangle(boolean perfect) {
        mIsPerfect = perfect;
    }

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
        width = 0;
    }

    @Override
    public void update(float right, float bottom) {
        if (mIsPerfect) {
            width = (float) distance(new PointF(right, bottom), new PointF(left, top));
            rect.set(left - width /2f, top - width /2f, left + width / 2f, top + width /2f);
        } else {
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

            rect.set(actualLeft, actualTop, actualRight, actualBottom);
        }
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public Path getDrawPath() {
        Path drawPath = new Path();
        if (mIsPerfect) {
            drawPath.moveTo(left, top);
        } else {
            drawPath.moveTo(rect.centerX(), rect.centerY());
        }
        drawPath.addRect(rect, Path.Direction.CW);
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
        Rectangle r = new Rectangle(mIsPerfect);
        r.startStroke(left, top);
        r.update(left + rect.width(), top + rect.height());
        r.startMove(rect.centerX(), rect.centerY());
        r.move(moveX, moveY);
        r.set(mColor, mSize, mIsFilled, mTransparency, mIsPerfect);
        return r;
    }

    @Override
    public void shiftBy(float shiftXAmount, float shiftYAmount) {
        rect.offset(shiftXAmount, shiftYAmount);
    }

    public RectF getRect() {
        return rect;
    }

    @Override
    public void updateWithScale(float scaleIndex) {
        if (mIsPerfect) {
            width = width * scaleIndex;
            rect.set(left - width /2f, top - width /2f, left + width / 2f, top + width /2f);
        } else {
            float h = rect.height() * scaleIndex;
            float w = rect.width() * scaleIndex;
            float right = left + w;
            float bottom = top + h;
            update(right, bottom);
        }
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

    /*
     public void animate(double fromDegrees, double toDegrees, long durationMillis) {
        final RotateAnimation rotate = new RotateAnimation((float)fromDegrees,(float) toDegrees,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(durationMillis);
        rotate.setFillEnabled(true);
        rotate.setFillAfter(true);
        rect.startAnimation(rotate);
    }
     */

}
