package multitouch.multitouchapp;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Any type of object that can be drawn on the screen: a path or a shape.
 * Created by Lauren on 11/6/2016.
 */

public abstract class Stroke {
    protected int mColor;
    protected boolean mIsFilled = false;
    protected float mSize;
    protected PointF p0Past, p1Past;
    protected static final float TOLERANCE = 1;
    protected final int MINIMUM_TRANSPARENCY = 0;
    protected int mTransparency = MINIMUM_TRANSPARENCY;

    public int getTransparency() {
        return mTransparency;
    }

    public boolean getIsFilled() {
        return mIsFilled;
    }

    public void setTransparency(int transparency) {
        mTransparency = transparency;
    }

    protected void set(int color, float size) {
        mColor = color;
        mSize = size;
    }

    // The minimum change in distance between two fingers that will register a change.
    protected final float MINIMUM_DELTA_FINGER_DISTANCE = 10f;
    protected final float LOCKED_DELTA_FINGER_DISTANCE = 5f;
    protected final float MINIMUM_FINGER_DISTANCE = 200f;

    protected final float FINGER_PIXELS = 100f;

    // You can't grab objects that you aren't at least somewhat near.
    protected final float MINIMUM_MOVE_DISTANCE = 200;

    public void setColor(int c) {
        mColor = c;
    }

    public void setSize(float s) {
        mSize = s;
    }

    public int getColor() {
        return mColor;
    }

    public float getSize() {
        return mSize;
    }

    public abstract void startStroke(float x, float y);
    public abstract void update(float x, float y);
    public abstract void finishStroke(float x, float y);
    public abstract Path getDrawPath();

    public abstract boolean containsTap(float x, float y);

    public abstract float distanceFromTap(float x, float y);

    // Translate the stroke by dx and dy.
    public abstract void move(float dx, float dy);

    // Translate, scale, or rotate the stroke by two control points.
    public abstract void adjustColor(PointF p0, PointF p1, PointF p2);
    public abstract void startMove(float x, float y);

    protected double distance(PointF start, PointF end) {
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
    }

    public abstract boolean isStrayStroke();
    public abstract Stroke clone();

    public void setColorAdjustmentPoints(PointF p0) {
        this.p0Past = p0;
    }

    public void setDragPoint(PointF p1) {
        this.p1Past = p1;
    }

    public abstract void shiftBy(float shiftXAmount, float shiftYAmount);

}
