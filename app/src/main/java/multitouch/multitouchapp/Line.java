package multitouch.multitouchapp;

/**
 * Stroke of a perfect line.
 * Created by Lauren on 11/15/2016.
 */

public class Line extends DrawPath {
    private float mStartX, mStartY;

    @Override
    public void startStroke(float x, float y) {
        super.startStroke(x, y);
        mStartX = x;
        mStartY = y;
    }

    @Override
    public void update(float x, float y) {
        if (drawPath == null) {
            startStroke(x ,y);
        } else {
            drawPath.reset();
            drawPath.moveTo(mStartX, mStartY);
            drawPath.lineTo(x, y);
        }
    }

}
