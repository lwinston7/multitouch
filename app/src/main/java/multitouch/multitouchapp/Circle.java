package multitouch.multitouchapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by Lauren on 11/3/2016.
 */

public class Circle extends DrawShape {
    private float radius;
    private float x;
    private float y;
    private static final float TOLERANCE = 5;
    public Circle() {}
    public Circle(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    @Override
    public void update(float x, float y) {
        radius = (float) Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y,2));
    }

    public void updateWithScale(float scaleIndex) {
        radius = radius * scaleIndex;
    }

    @Override
    public void startStroke(float x, float y) {
        this.x = x;
        this.y = y;
        radius = 0;
    }

    @Override
    public void finishStroke(float x, float y) {
        update(x, y);
    }

    @Override
    public Path getDrawPath() {
        Path drawPath = new Path();
        drawPath.moveTo(x, y);
        drawPath.addCircle(x, y, radius, Path.Direction.CW);
        return drawPath;
    }

    @Override
    public void move(float x1, float y1) {
        x = x1;
        y = y1;
    }

    @Override
    public void startMove(float x, float y) {
        this.x = x;
        this.y = y;

        p0Past = null;
        p1Past = null;
    }

    @Override
    public boolean isStrayStroke() {
        return radius < FINGER_PIXELS;
    }

    @Override
    public Stroke clone() {
        Circle c = new Circle(x, y, radius);
        c.set(mColor, mSize, mIsFilled, mTransparency);
        return c;
    }

    public void updateR(float newR) {
        radius = newR;
    }
}
