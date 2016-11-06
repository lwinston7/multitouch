package multitouch.multitouchapp;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;

import java.util.ArrayList;

/**
 * Created by Lauren on 11/6/2016.
 */

public class DrawPath extends Stroke {
    private Path drawPath;
    private ArrayList<Point> points = new ArrayList<Point>();
    private float mX;
    private float mY;
    private static final float TOLERANCE = 5;

    @Override
    public void startStroke(float x, float y) {
        drawPath = new Path();
        drawPath.moveTo(x,y);
        mX = x;
        mY = y;
        points.add(new Point((int) x, (int) y));
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

        points.add(new Point((int) x, (int) y));
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
        for (int i = 0; i < points.size(); i++) {
            float distance = (float) distance(midpoint, points.get(i));
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private double distance(Point start, Point end) {
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
    }
}
