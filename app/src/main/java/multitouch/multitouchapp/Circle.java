package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

/**
 * Created by Lauren on 11/3/2016.
 */

public class Circle {
    private float radius;
    private float x;
    private float y;

    public Circle (float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void updateRadius(float x, float y) {
        radius = (float) Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y,2));
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRadius() {
        return radius;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
