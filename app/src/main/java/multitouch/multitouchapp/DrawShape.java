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
    protected int mTransparency = MINIMUM_TRANSPARENCY;

    public boolean getIsFilled() {
        return mIsFilled;
    }

    public int getTransparency() {
        return mTransparency;
    }

    @Override
    public void move(PointF p0, PointF p1) {
        if (p0Past != null&& p1Past != null) {
            float currDistance = (float) distance(p0, p1);
            float pastDistance = (float) distance(this.p0Past, this.p1Past);
            float p0Distance = (float) distance(p0, this.p0Past);
            float p1Distance = (float) distance(p1, this.p1Past);
            float deltaDistance = currDistance - pastDistance;
            if (Math.abs(currDistance) > 250 || (Math.abs(p0Distance) > MINIMUM_DELTA_FINGER_DISTANCE && Math.abs(p1Distance) < LOCKED_DELTA_FINGER_DISTANCE) ||
                (Math.abs(p1Distance) > MINIMUM_DELTA_FINGER_DISTANCE && Math.abs(p0Distance) < LOCKED_DELTA_FINGER_DISTANCE) ||
                    Math.abs(deltaDistance) > 50) {
                mIsFilled = true;
                mTransparency = (int) Math.min(Math.max(mTransparency + deltaDistance, MINIMUM_TRANSPARENCY), 255);
                p0Past = p0;
                p1Past = p1;
                return;
            }
        }

        move((p0.x + p1.x) / 2f, (p1.y + p0.y) / 2f);
        p0Past = p0;
        p1Past = p1;
    }

    @Override
    protected boolean containsTap(float x1, float y1, float x2, float y2) {
        float x = (x1 + x2) / 2f;
        float y = (y1 + y2) / 2f;
        Path drawPath = getDrawPath();
        RectF bounds = new RectF();
        drawPath.computeBounds(bounds, false);
        return bounds.contains(x - TOLERANCE, y - TOLERANCE) ||
                bounds.contains(x - TOLERANCE, y + TOLERANCE) ||
                bounds.contains(x + TOLERANCE, y - TOLERANCE) ||
                bounds.contains(x + TOLERANCE, y + TOLERANCE);
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        float midX = (x1 + x2) / 2;
        float midY = (y1 + y2) / 2;
        PointF pt = new PointF(midX, midY);
        if (containsTap(x1, y1, x2, y2)) {
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
}
