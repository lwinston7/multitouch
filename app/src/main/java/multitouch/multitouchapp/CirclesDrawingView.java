package multitouch.multitouchapp;

/**
 * Created by michelleliu on 10/31/16.
 */

import android.graphics.Bitmap;
import android.graphics.Rect;


public class CirclesDrawingView {
    private static final String TAG = "CirclesDrawingView";

    /** Main bitmap */
    private Bitmap mBitmap = null;

    private Rect mMeasuredRect;

    /** Stores data about single circle */
    private static class CircleArea {
        int radius;
        int centerX;
        int centerY;

        CircleArea(int centerX, int centerY, int radius) {
            this.radius = radius;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public String toString() {
            return "Circle[" + centerX + ", " + centerY + ", " + radius + "]";
        }
    }
}