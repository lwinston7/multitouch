package multitouch.multitouchapp;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Lauren on 11/6/2016.
 */

public class DrawPath extends Stroke {
    // TODO: (Make ArrayList)
    private Path drawPath;
    private float mX;
    private float mY;
    private static final float TOLERANCE = 5;
    private float moveX = 0;
    private float moveY = 0;
    private final float SUBDIVIDE_THRESHOLD = .01f;

    @Override
    public void startStroke(float x, float y) {
        drawPath = new Path();
        drawPath.moveTo(x,y);
        // drawPaths = new ArrayList<Path>();
        //drawPaths.add(path;
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
    }

    public Path getDrawPath() {
        return drawPath;
    }

    @Override
    public boolean containsTap(float x1, float y1, float x2, float y2) {
        return false;
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        Point midpoint = new Point((int) (x1 + x2 / 2f), (int) (y1 + y2 / 2f));
        float minDistance = Integer.MAX_VALUE;
        PathMeasure pm = new PathMeasure(drawPath, false);
        float len = 0;
        while (len < pm.getLength()) {
            float[] coordinates = {0f, 0f};
            pm.getPosTan(len, coordinates, null);
            float distance = (float) distance(midpoint, new Point((int)coordinates[0], (int)coordinates[1]));
            if (distance < minDistance) {
                minDistance = distance;
            }
            len += pm.getLength() * SUBDIVIDE_THRESHOLD;
        }
        return minDistance;
    }

    @Override
    public void move(float x, float y) {
        drawPath.offset(x - moveX, y - moveY);
        moveX = x;
        moveY = y;
    }

    @Override
    public void move(Point p0, Point p1) {
        float currDistance = (float) distance(p0, p1);
        float pastDistance = (float) distance(this.p0Past, this.p1Past);
        float deltaDistance = currDistance - pastDistance;

        // If delta distance is significant, move with p0. Otherwise, move with the midpoint.
        if (Math.abs(deltaDistance) > MINIMUM_DELTA_FINGER_DISTANCE) {
            // TODO: Add line manipulation here.
            this.p0Past = p0;
            this.p1Past = p1;
            return;
        }

        move((p0.x + p1.x) / 2f, (p1.y + p0.y) / 2f);
        this.p0Past = p0;
        this.p1Past = p1;
    }

    @Override
    public void startMove(float x, float y) {
        moveX = x;
        moveY = y;
        // TODO: (Lauren) This should be the actual points, not the same x, y.
        p0Past = new Point((int)x, (int)y);
        p1Past = new Point((int)x, (int)y);
    }
}
