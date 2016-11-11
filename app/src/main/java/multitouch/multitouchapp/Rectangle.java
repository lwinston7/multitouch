package multitouch.multitouchapp;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by Lauren on 11/6/2016.
 */

public class Rectangle extends DrawShape{
    Rect rect;
    int left, top;
    float moveX, moveY;
    private static final float TOLERANCE = 1;

    @Override
    public void startStroke(float x, float y) {
        rect = new Rect((int)x,(int) y,(int) x,(int) y);
        this.left = (int) x;
        this.top = (int) y;
    }

    @Override
    public void update(float right, float bottom) {
        // TODO: Adjust for negative numbers.
        rect.set(left, top, (int) right,(int) bottom);
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public boolean containsTap(float x1, float y1, float x2, float y2) {
        float width = rect.width();
        float height = rect.height();
        float midx = rect.centerX();
        float midy = rect.centerY();
        if (midx > (left + width + TOLERANCE)) {
            return false;
        } else if (midx < (left - TOLERANCE)) {
            return false;
        } else if (midy > (rect.bottom + height + TOLERANCE)) {
            return false;
        }else if (midy < (rect.bottom - TOLERANCE)) {
            return false;
        }
        return true;
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        if (containsTap(x1, y1, x2, y2)) {
            Log.d("test", "contains tap");
            return 0;
        } else {
            Point midpoint = new Point((int) (x1 + x2 / 2f), (int) (y1 + y2 / 2f));
            return distance(midpoint);
        }
    }

    private float distance(Point pt) {
        float dx = Math.max(Math.abs(pt.x - rect.centerX()) - rect.width() / 2, 0);
        float dy = Math.max(Math.abs(pt.y - rect.centerY()) - rect.height()/ 2, 0);
        return dx * dx + dy * dy;
    }

    @Override
    //move- track
    public void move(float x1, float y1) {
        rect.offset((int)(x1 - moveX), (int)(y1 - moveY));
        moveX = x1;
        moveY = y1;
    }

    @Override
    //start move - starting point
    public void startMove(float x1, float y1) {
        moveX = x1;
        moveY = y1;
    }

    public Rect getRect() {
        return rect;
    }

    public void updateHeightWidth(float h, float w) {
        float right = left + w;
        float bottom = top + h;
        // TODO: Adjust for negative numbers.
        rect.set(left, top, (int) right,(int) bottom);
    }

}
