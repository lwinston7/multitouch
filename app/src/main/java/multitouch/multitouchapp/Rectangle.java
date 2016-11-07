package multitouch.multitouchapp;

import android.graphics.Rect;

/**
 * Created by Lauren on 11/6/2016.
 */

public class Rectangle extends DrawShape{
    float x, y;
    float width = 100.0f;
    float height = 50.0f;
    @Override
    public void startStroke(float x, float y) {
        this.x = x;
        this.y = y;
        width = 0;
        height = 0;
    }

    @Override
    public void update(float x, float y) {
        // TODO: Adjust for negative numbers.
        width = x;
        height = y;
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public boolean containsTap(float x1, float y1, float x2, float y2) {
        return false;
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        return 0;
    }

    @Override
    public void move(float x, float y) {

    }

    @Override
    public void startMove(float x, float y) {

    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
