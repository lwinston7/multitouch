package multitouch.multitouchapp;

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
}
