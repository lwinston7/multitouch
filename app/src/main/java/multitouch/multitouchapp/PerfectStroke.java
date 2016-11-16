package multitouch.multitouchapp;

import android.annotation.TargetApi;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

/**
 * Defines a "perfect" path or shape.
 * Detects whether a user is drawing a line, circle, or rectangle and draws a "perfected" version
 * of that shape.
 * Created by Lauren on 11/15/2016.
 */

@TargetApi(21)
public class PerfectStroke extends Stroke{
    private float TOLERANCE_RANGE = 10;
    private float mStartX, mStartY;
    private Stroke mPerfectStroke;
    private Path mSecondPath;
    private enum StrokeType {
        Line,
        Circle,
        Rectangle
    }

    private StrokeType currStrokeType;

    @Override
    public void startStroke(float x, float y) {
        currStrokeType = StrokeType.Line;
        mPerfectStroke = new Line();
        mPerfectStroke.startStroke(x, y);
        mStartX = x;
        mStartY = y;
    }

    @Override
    public void update(float x, float y) {
        // TODO: Detect how it has moved.
        mPerfectStroke.update(x, y);
    }

    @Override
    public void finishStroke(float x, float y) {
    }

    @Override
    public Path getDrawPath() {
        return mPerfectStroke.getDrawPath();
    }

    public Stroke getPerfectStroke() {
        return mPerfectStroke;
    }

    public void newStroke() {
        if (currStrokeType == StrokeType.Line) {
            currStrokeType = StrokeType.Circle;
            mPerfectStroke = new Circle();

        } else if (currStrokeType == StrokeType.Circle) {
            currStrokeType = StrokeType.Rectangle;
            mPerfectStroke = new Rectangle();

        } else if (currStrokeType == StrokeType.Rectangle) {
            currStrokeType = StrokeType.Line;
            mPerfectStroke = new Line();
        }
        mPerfectStroke.startStroke(mStartX, mStartY);
    }

    @Override
    public boolean containsTap(float x1, float y1, float x2, float y2) {
        return mPerfectStroke.containsTap(x1, y1, x2, y2);
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        return mPerfectStroke.distanceFromTap(x1, y1, x2, y2);
    }

    public  void move(float dx, float dy) {
        mPerfectStroke.move(dx, dy);
    }

    @Override
    public void move(Point p0, Point p1) {
        mPerfectStroke.move(p0, p1);
    }

    @Override
    public void startMove(float x, float y) {
        mPerfectStroke.startMove(x, y);
    }

    @Override
    public boolean isStrayStroke() {
        return false;
    }
}
