package multitouch.multitouchapp;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by Lauren on 11/6/2016.
 */

public class Rectangle extends DrawShape{
    private Rect rect;
    private int left, top;
    private float moveX, moveY;
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
        int actualLeft = left;
        int actualTop = top;
        int actualRight = (int) right;
        int actualBottom = (int) bottom;
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
    public boolean containsTap(float x1, float y1, float x2, float y2) {
        float width = rect.width();
        float height = rect.height();
        float midx = rect.centerX();
        float midy = rect.centerY();
        float midTouchX = (x1 + x2) / 2;
        float midTouchY = (y1 + y2) / 2;
        if (midTouchX > (left + width + TOLERANCE)) {
            return false;
        } else if (midTouchX < (left - TOLERANCE)) {
            return false;
        } else if (midTouchY > (rect.bottom + TOLERANCE)) {
            return false;
        }else if (midTouchY < (rect.top - TOLERANCE)) {
            return false;
        }
        return true;
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        if (containsTap(x1, y1, x2, y2)) {
            return 0;
        } else {
            Point midpoint = new Point((int) (x1 + x2 / 2f), (int) (y1 + y2 / 2f));
            return (float) distance(midpoint);
        }
    }

    private double distance(Point pt) {
        double distance = Integer.MAX_VALUE;
        if (pt.x < rect.left) {
            if (pt.y > rect.bottom) {
                distance = distance(pt, new Point(rect.left, rect.bottom));
            } else if (pt.y < rect.top) {
                distance = distance(pt, new Point(rect.left, rect.top));
            } else {
                distance = rect.left - pt.x;
            }
        } else if (pt.x > rect.right) {
            if (pt.y > rect.bottom) {
                distance = distance(pt, new Point(rect.right, rect.bottom));
            } else if (pt.y < rect.top){
                distance = distance(pt, new Point(rect.right, rect.top));
            } else {
                distance = pt.x - rect.right;
            }
        } else {
            distance = Math.min(rect.top - pt.y, pt.y - rect.bottom);
        }

        return Math.abs(distance);
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
        p0Past = null;
        p1Past = null;
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
