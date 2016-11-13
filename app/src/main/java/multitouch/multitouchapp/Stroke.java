package multitouch.multitouchapp;

import android.graphics.Point;

/**
 * Any type of object that can be drawn on the screen: a path or a shape.
 * Created by Lauren on 11/6/2016.
 */

public abstract class Stroke {
    public abstract void startStroke(float x, float y);
    public abstract void update(float x, float y);
    public abstract void finishStroke(float x, float y);
    public abstract boolean containsTap(float x1, float y1, float x2, float y2);
    public abstract float distanceFromTap(float x1, float y1, float x2, float y2);
    public abstract void move(float x, float y);
    public abstract void startMove(float x, float y);

    protected double distance(Point start, Point end) {
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
    }
}
