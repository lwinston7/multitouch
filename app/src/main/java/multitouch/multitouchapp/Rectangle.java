package multitouch.multitouchapp;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.ArrayList;

/**
 * Created by Lauren on 11/6/2016.
 */

public class Rectangle extends DrawShape{
    float x, y;
    float width = 100.0f;
    float height = 50.0f;
    private Path drawPath;
    private static final float TOLERANCE = 5;
    private float moveX = 0;
    private float moveY = 0;

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
        float midx = (x1 + x2) / 2f;
        float midy = (y1 + y2) / 2f;
        if (midx > (x + width + TOLERANCE)) {
            return false;
        } else if (midx < (x - TOLERANCE)) {
            return false;
        } else if (midy > (y + height + TOLERANCE)) {
            return false;
        }else if (midy < (y - TOLERANCE)) {
            return false;
        }
        return true;
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
