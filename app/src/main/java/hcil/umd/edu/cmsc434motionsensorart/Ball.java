package hcil.umd.edu.cmsc434motionsensorart;

import android.graphics.Paint;
import android.os.SystemClock;

/**
 * Created by jonf on 12/8/2016.
 */

public class Ball{
    public static float DEFAULT_BALL_RADIUS = 15;

    public float radius = DEFAULT_BALL_RADIUS;
    public float xLocation = 0; // center x
    public float yLocation = 0; // center y
    public float xVelocity = 10;
    public float yVelocity = 10;
    public Paint paint = new Paint();
    private long _creationTimestampMs = -1;

    public Ball(float radius, float xLoc, float yLoc, float xVel, float yVel, int color){
        this.radius = radius;
        this.xLocation = xLoc;
        this.yLocation = yLoc;
        this.xVelocity = xVel;
        this.yVelocity = yVel;
        this.paint.setColor(color);
        _creationTimestampMs = SystemClock.elapsedRealtime();
    }

    public float getRight() {
        return this.xLocation + this.radius;
    }

    public float getLeft(){
        return this.xLocation - this.radius;
    }

    public float getTop(){
        return this.yLocation - this.radius;
    }

    public float getBottom(){
        return this.yLocation + this.radius;
    }

    public long getAge(){ return SystemClock.elapsedRealtime() - _creationTimestampMs; }
}
