package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by Lauren on 11/3/2016.
 */

public class Circle extends DrawShape {
    private RectF ovalRect;
    private float radius;
    private float x;
    private float y;
    private float moveX, moveY;
    private static final float TOLERANCE = 5;
    public Circle(boolean perfect) {
        mIsPerfect = perfect;
    }
    public Circle(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    public void update(float x, float y) {
        if (mIsPerfect) {
            radius = (float) Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2));
        } else {
            ovalRect.set(this.x, this.y, x, y);
        }
    }

    @Override
    public void updateWithScale(float scaleIndex) {
        if (mIsPerfect) {
            radius = radius * scaleIndex;
        } else {
            float h = ovalRect.height() * scaleIndex;
            float w = ovalRect.width() * scaleIndex;
            float right = x + w;
            float bottom = y + h;
            update(right, bottom);
        }
    }

    @Override
    public void startStroke(float x, float y) {
        this.x = x;
        this.y = y;
        radius = 0;
        ovalRect = new RectF(x, y, x, y);
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public Path getDrawPath() {
        Path drawPath = new Path();
        if (mIsPerfect) {
            drawPath.moveTo(x, y);
            drawPath.addCircle(x, y, radius, Path.Direction.CW);
        } else {
            drawPath.moveTo(ovalRect.centerX(), ovalRect.centerY());
            drawPath.addOval(ovalRect, Path.Direction.CW);
        }
        return drawPath;
    }

    @Override
    public void move(float x1, float y1) {
        if (mIsPerfect) {
            x = x1;
            y = y1;
        } else {
            ovalRect.offset((x1 - moveX), (y1 - moveY));
            moveX = x1;
            moveY = y1;
        }
    }

    @Override
    public void startMove(float x, float y) {
        if (mIsPerfect) {
            this.x = x;
            this.y = y;
        } else {
            moveX = x;
            moveY = y;
        }

        p0Past = null;
        p1Past = null;
    }

    @Override
    public boolean isStrayStroke() {
        return radius < FINGER_PIXELS;
    }

    @Override
    public Stroke clone() {
        Circle c = new Circle(x, y, radius);
        if (!mIsPerfect) {
            c.startStroke(x, y);
            c.update(x + ovalRect.width(), y + ovalRect.height());
            c.startMove(ovalRect.centerX(), ovalRect.centerY());
            c.move(moveX, moveY);
        }
        c.set(mColor, mSize, mIsFilled, mTransparency, mIsPerfect);
        return c;
    }

    @Override
    public void meteredShift(float pointX, float pointY) {
        float deltaX = pointX - mLastMeteredShiftPoint.x;
        float deltaY = pointY - mLastMeteredShiftPoint.y;
        if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            if (deltaX > 0) {
                x += 20;
            } else {
                x -= 20;
            }
        } else if (Math.abs(deltaY) > Math.abs(deltaX)) {
            if (deltaY > 0) {
                y += 20;
            } else {
                y-= 20;
            }
        }

        mLastMeteredShiftPoint = new PointF(x, y);
    }
}
