package multitouch.multitouchapp;

import android.annotation.TargetApi;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Lauren on 11/6/2016.
 */

@TargetApi(21)
public class DrawPath extends Stroke {
    // TODO: (Use ArrayList)
    private ArrayList<Path> paths = new ArrayList<Path>();
    protected Path drawPath;
    protected float mX;
    protected float mY;
    protected static final float TOLERANCE = 5;
    protected float moveX = 0;
    protected float moveY = 0;
    protected final float SUBDIVIDE_THRESHOLD = .01f;
    public DrawPath() {}
    public DrawPath(Path drawPath) {
        this.drawPath = drawPath;
    }


    @Override
    public void startStroke(float x, float y) {
        drawPath = new Path();
        drawPath.moveTo(x,y);
        paths.add(drawPath);
        mX = x;
        mY = y;
    }

    @Override
    public void update(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            drawPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }

    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
        //Log.d("convex", drawPath.isConvex() + "");
    }

    public Path getDrawPath() {
        return drawPath;
    }

    @Override
    public boolean containsTap(float x, float y) {
        return false;
    }

    @Override
    public float distanceFromTap(float x, float y) {
        if (containsTap(x, y)) {
            return 0;
        } else {
            float minDistance = Integer.MAX_VALUE;
            PointF midpoint = new PointF(x, y);
            PathMeasure pm = new PathMeasure(drawPath, false);
            float len = 0;
            while (len < pm.getLength()) {
                float[] coordinates = {0f, 0f};
                pm.getPosTan(len, coordinates, null);
                float distance = (float) distance(midpoint, new PointF(coordinates[0], coordinates[1]));
                if (distance < minDistance) {
                    minDistance = distance;
                }
                len += pm.getLength() * SUBDIVIDE_THRESHOLD;
            }
            return minDistance;
        }
    }

    @Override
    public void move(float x, float y) {
        drawPath.offset(x - moveX, y - moveY);
        moveX = x;
        moveY = y;
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
            mColor = (int) Math.min(Math.max(mColor + deltaDistance, mColor), 255);
        }

        p1Past = dragPoint;
    }

    @Override
    public void startMove(float x, float y) {
        moveX = x;
        moveY = y;
        // TODO: (Lauren) This should be the actual points, not the same x, y.
        p0Past = new PointF(x, y);
        p1Past = new PointF(x, y);
    }

    @Override
    public boolean isStrayStroke() {
        PathMeasure pm = new PathMeasure(drawPath, false);
        float[] coordinates = {0f, 0f};
        pm.getPosTan(0, coordinates, null);
        if (pm.getLength() < FINGER_PIXELS) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Stroke clone() {
        return new DrawPath(new Path(drawPath));
    }

    @Override
    public void meteredShift(float x, float y) {

    }
}
