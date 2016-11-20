package multitouch.multitouchapp;

import android.annotation.TargetApi;
import android.graphics.Path;
import android.graphics.PointF;

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
    private float mDragX, mDragY;
    private Stroke mPerfectStroke;
    private Path mSecondPath;
    private enum StrokeType {
        Line,
        Circle,
        Rectangle
    }

    public PerfectStroke() {
        currStrokeType = StrokeType.Line;
        mPerfectStroke = new Line();
    }
    private StrokeType currStrokeType;

    @Override
    public void startStroke(float x, float y) {
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
        if (mPerfectStroke != null) {
            return mPerfectStroke.getDrawPath();
        } else {
            return null;
        }
    }

    @Override
    public boolean containsTap(float x, float y) {
        return mPerfectStroke.containsTap(x, y);
    }

    public Stroke getPerfectStroke() {
        return mPerfectStroke;
    }

    public void newStroke() {
        if (currStrokeType == StrokeType.Line) {
            currStrokeType = StrokeType.Circle;
            mPerfectStroke = new Circle(true);

        } else if (currStrokeType == StrokeType.Circle) {
            currStrokeType = StrokeType.Rectangle;
            mPerfectStroke = new Rectangle(true);

        } else if (currStrokeType == StrokeType.Rectangle) {
            currStrokeType = StrokeType.Line;
            mPerfectStroke = new Line();
        }
        mPerfectStroke.startStroke(mStartX, mStartY);
    }

    @Override
    public float distanceFromTap(float x, float y) {
        return mPerfectStroke.distanceFromTap(x, y);
    }

    public  void move(float dx, float dy) {
        mPerfectStroke.move(dx, dy);
    }

    @Override
    public void adjustColor(PointF p0, PointF p1, PointF p2) {
        mPerfectStroke.adjustColor(p0, p1, p2);
    }

    public void setDragPoint(float x, float y) {
        this.mDragX = x;
        this.mDragY = y;
    }

    @Override
    public void startMove(float x, float y) {
        mPerfectStroke.startMove(x, y);
    }

    @Override
    public boolean isStrayStroke() {
        return false;
    }

    @Override
    public Stroke clone() {
        return new PerfectStroke();
    }

    @Override
    public void meteredShift(float x, float y) {

    }
}
