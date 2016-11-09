package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

/**
 * Created by Lauren on 11/3/2016.
 */

public class Circle extends DrawShape {
    private float radius;
    private float x;
    private float y;
    private static final float TOLERANCE = 5;


    @Override
    public void update(float x, float y) {
        radius = (float) Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y,2));
    }

    public float getRadius() {
        return radius;
    }
    public void updateWithScale(float scaleIndex) {
        radius = radius * scaleIndex;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public void startStroke(float x, float y) {
        this.x = x;
        this.y = y;
        radius = 10;
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public boolean containsTap(float x1, float y1, float x2, float y2) {
        float midx = (x1 + x2) / 2f;
        float midy = (y1 + y2) / 2f;
        if (midx < (x - radius - TOLERANCE)) {
            return false;
        } else if (midx > (x + radius + TOLERANCE)) {
            return false;
        } else if (midy < (y - radius - TOLERANCE)) {
            return false;
        } else if (midy > (y + radius + TOLERANCE)) {
            return false;
        }
        return true;
    }

    @Override
    public float distanceFromTap(float x1, float y1, float x2, float y2) {
        return 0;
    }

    @Override
    public void move(float x1, float y1) {
        x = x1;
        y = y1;
    }

    @Override
    public void startMove(float x1, float y1) {
        x = x1;
        y = y1;

    }
}
