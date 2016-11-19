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
    private final int MINIMUM_TRANSPARENCY = 20;
    protected float mRotation = 0;
    protected int mTransparency = MINIMUM_TRANSPARENCY;

    public boolean getIsFilled() {
        return mIsFilled;
    }

    public int getTransparency() {
        return mTransparency;
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
        PointF midpoint = new PointF();
        PointF dragPoint = new PointF();
        if (containsTap(p0.x, p0.y) && containsTap(p1.x, p1.y) && !containsTap(p2.x, p2.y)) {
            midpoint = new PointF((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f);
            dragPoint = new PointF(p2.x, p2.y);
        } else if (!containsTap(p0.x, p0.y) && containsTap(p1.x, p1.y) && containsTap(p2.x, p2.y)) {
            midpoint = new PointF((p2.x + p1.x) / 2f, (p2.y + p1.y) / 2f);
            dragPoint = new PointF(p0.x, p0.y);
        } else if (containsTap(p0.x, p0.y) && !containsTap(p1.x, p1.y) && containsTap(p2.x, p2.y)) {
            midpoint = new PointF((p0.x + p2.x) / 2f, (p0.y + p2.y) / 2f);
            dragPoint = new PointF(p1.x, p1.y);
        }

        float currDistance = (float) distance(midpoint, dragPoint);
        float pastDistance = (float) distance(this.p0Past, this.p1Past);
        float p0Distance = (float) distance(midpoint, this.p0Past);
        float p1Distance = (float) distance(dragPoint, this.p1Past);
        float deltaDistance = currDistance - pastDistance;
        if (Math.abs(currDistance) > 250 || (Math.abs(p0Distance) > MINIMUM_DELTA_FINGER_DISTANCE && Math.abs(p1Distance) < LOCKED_DELTA_FINGER_DISTANCE) ||
                (Math.abs(p1Distance) > MINIMUM_DELTA_FINGER_DISTANCE && Math.abs(p0Distance) < LOCKED_DELTA_FINGER_DISTANCE) ||
                    Math.abs(deltaDistance) > 50) {
                mIsFilled = true;
                mTransparency = (int) Math.min(Math.max(mTransparency + deltaDistance, MINIMUM_TRANSPARENCY), 255);
            }

        p0Past = p0;
        p1Past = p1;
    }

    public void setColorAdjustmentPoints(PointF p0, PointF p1) {
        // TODO: Move this to Stroke.
        this.p0Past = p0;
        this.p1Past = p1;
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
                } else if (pt.y < bounds.top){
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

    protected void set(int color, float size, boolean isFilled, int transparency) {
        super.set(color, size);
        this.mIsFilled = isFilled;
        this.mTransparency = transparency;
    }
}
