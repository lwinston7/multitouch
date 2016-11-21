package multitouch.multitouchapp;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

/**
 * Created by Lauren on 11/6/2016.
 */

public abstract class DrawShape extends Stroke {
    protected boolean mIsFilled = false;
    protected boolean mIsPerfect = false;
    protected float mRotation = 0;

    public boolean getIsFilled() {
        return mIsFilled;
    }



    public void setRotation(float rotation) {
        mRotation = rotation;
    }

    public float getRotation() {
        return mRotation;
    }


    @Override
    public void adjustColor(PointF p0, PointF p1, PointF p2) {
        // TODO: Manage these points better.
        PointF dragPoint = p0;
        if (p1 != null && distance(p0Past, dragPoint) < distance(p0Past, p1)) {
            dragPoint = p1;
        }

        if (p2 != null && distance(p0Past, dragPoint) < distance(p0Past, p2)) {
            dragPoint = p2;
        }

        float dragDistance = (float) distance(dragPoint, this.p1Past);
        float deltaDistance = p1Past.y - dragPoint.y;
        if ((Math.abs(dragDistance) < LOCKED_DELTA_FINGER_DISTANCE) ||
                (Math.abs(dragDistance) > MINIMUM_DELTA_FINGER_DISTANCE) ||
                Math.abs(deltaDistance) > 10) {
            mIsFilled = true;
            mTransparency = (int) Math.min(Math.max(mTransparency + deltaDistance, MINIMUM_TRANSPARENCY), 255);
        }

        p1Past = dragPoint;
    }


    @Override
    public boolean containsTap(float x, float y) {
        Path drawPath = getDrawPath();
        RectF bounds = new RectF();
        drawPath.computeBounds(bounds, false);
        return bounds.contains(x - TOLERANCE, y - TOLERANCE) ||
                bounds.contains(x - TOLERANCE, y + TOLERANCE) ||
                bounds.contains(x + TOLERANCE, y - TOLERANCE) ||
                bounds.contains(x + TOLERANCE, y + TOLERANCE);
    }

    @Override
    public float distanceFromTap(float x, float y) {
        PointF pt = new PointF(x, y);
        if (containsTap(x, y)) {
            return 0;
        } else {
            Path drawPath = getDrawPath();
            RectF bounds = new RectF();
            //TODO: Should be uncommented without breaking double-tap to erase.
            //drawPath.computeBounds(bounds, false);
            double distance = Integer.MAX_VALUE;
            if (pt.x < bounds.left) {
                if (pt.y > bounds.bottom) {
                    distance = distance(pt, new PointF(bounds.left, bounds.bottom));
                } else if (pt.y < bounds.top) {
                    distance = distance(pt, new PointF(bounds.left, bounds.top));
                } else {
                    distance = bounds.left - pt.x;
                }
            } else if (pt.x > bounds.right) {
                if (pt.y > bounds.bottom) {
                    distance = distance(pt, new PointF(bounds.right, bounds.bottom));
                } else if (pt.y < bounds.top) {
                    distance = distance(pt, new PointF(bounds.right, bounds.top));
                } else {
                    distance = pt.x - bounds.right;
                }
            } else {
                distance = Math.min(bounds.top - pt.y, pt.y - bounds.bottom);
            }

            return (float) Math.abs(distance);
        }
    }

    protected void set(int color, float size, boolean isFilled, int transparency, boolean isPerfect) {
        super.set(color, size);
        this.mIsFilled = isFilled;
        this.mTransparency = transparency;
        this.mIsPerfect = isPerfect;
    }

    public abstract void updateWithScale(float scaleIndex);
}
