package multitouch.multitouchapp;

import android.graphics.Point;
import android.util.Log;

/**
 * Created by Lauren on 11/6/2016.
 */

public abstract class DrawShape extends Stroke {
    protected boolean mIsFilled = false;
    private final int MINIMUM_TRANSPARENCY = 20;
    protected int mTransparency = MINIMUM_TRANSPARENCY;

    public boolean getIsFilled() {
        return mIsFilled;
    }

    public int getTransparency() {
        return mTransparency;
    }

    @Override
    public void move(Point p0, Point p1) {
        if (p0Past != null&& p1Past != null) {
            float currDistance = (float) distance(p0, p1);
            float pastDistance = (float) distance(this.p0Past, this.p1Past);
            float deltaDistance = currDistance - pastDistance;
            if (Math.abs(deltaDistance) > .5) {
                mIsFilled = true;
                mTransparency = (int) Math.min(Math.max(mTransparency + deltaDistance, MINIMUM_TRANSPARENCY), 255);
                p0Past = p0;
                p1Past = p1;
                return;
            }
        }

        move((p0.x + p1.x) / 2f, (p1.y + p0.y) / 2f);
        p0Past = p0;
        p1Past = p1;
    }
}
