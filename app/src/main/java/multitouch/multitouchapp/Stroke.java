package multitouch.multitouchapp;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;

/**
 * Any type of object that can be drawn on the screen: a path or a shape.
 * Created by Lauren on 11/6/2016.
 */

public abstract class Stroke {
    protected int mColor;
    protected float mSize;
    protected Point p0Past, p1Past;

    // The minimum change in distance between two fingers that will register a change.
    protected float MINIMUM_DELTA_FINGER_DISTANCE = .5f;

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
    public abstract boolean containsTap(float x1, float y1, float x2, float y2);
    public abstract float distanceFromTap(float x1, float y1, float x2, float y2);

    // Translate the stroke by dx and dy.
    public abstract void move(float dx, float dy);

    // Translate, scale, or rotate the stroke by two control points.
    public abstract void move(Point p0, Point p1);
    public abstract void startMove(float x, float y);

    protected double distance(Point start, Point end) {
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
    }
}
