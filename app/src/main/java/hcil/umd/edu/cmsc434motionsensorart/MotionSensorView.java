package hcil.umd.edu.cmsc434motionsensorart;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

/**
 * Created by jonf on 12/8/2016.
 * This is the basic implementation of using the accelerometer to control the balls that we went over in class
 */

public class MotionSensorView extends View implements SensorEventListener{
    public static float MIN_BALL_RADIUS = 8;
    public static float MAX_BALL_RADIUS = 35;
    public static int DEFAULT_PAINT_TRAIL_ALPHA = 128;
    public static int DEFAULT_ERASING_OVERLAY_ALPHA = 10;
    public static long DEFAULT_DEATH_THRESHOLD_MILLISEC = 20 * 1000;
    public static int DEFAULT_BACKGROUND_COLOR = Color.WHITE;

    private static final Random _random = new Random();

    private Paint _paintText = new Paint();

    public ArrayList<Ball> balls = new ArrayList<Ball>();

    // Sensor stuff
    private SensorManager _sensorManager;
    private Sensor _accelerometerSensor;
    private float _gravity[] = new float[3];
    private float _processedAcceleration[] = new float[3];
    private float _linearAcceleration[] = new float[3];
    private float _rawAccelerometerValues[] = new float[3];
    private long _lastTimeAccelSensorChangedInMs = -1;

    // For handling different screen rotations
    private WindowManager _windowManager;
    private Display _display;

    // This is for measuring frame rate, you can ignore
    private float _actualFramesPerSecond = -1;
    private long _startTime = -1;
    private int _frameCnt = 0;

    public MotionSensorView(Context context) {
        super(context);
        init(null, null, 0);
    }

    public MotionSensorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public MotionSensorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr) {

        this.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);

        int inverseBackgroundColor = ~DEFAULT_BACKGROUND_COLOR | 0xFF000000;
        _paintText.setColor(inverseBackgroundColor);
        _paintText.setTextSize(40f);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;

            // See https://developer.android.com/guide/topics/sensors/sensors_motion.html
            _sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            _accelerometerSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            _windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            _display = _windowManager.getDefaultDisplay();

            // See https://developer.android.com/reference/android/hardware/SensorManager.html
            // We really should be doing this in the Activity, so we can appropriately pause use of the sensor
            // https://developer.android.com/training/basics/activity-lifecycle/pausing.html

            // The official Google accelerometer example code found here:
            //   https://github.com/android/platform_development/blob/master/samples/AccelerometerPlay/src/com/example/android/accelerometerplay/AccelerometerPlayActivity.java
            // explains that it is not necessary to get accelerometer events at a very high rate, by using a slower rate (SENSOR_DELAY_UI), we get an
            // automatic low-pass filter, which "extracts" the gravity component of the acceleration. As an added benefit, we use less power and
            // CPU resources. I haven't experimented with this, so can't be sure.
            _sensorManager.registerListener(this, _accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        // start time helps measure fps calculations
        if(_startTime == -1) {
            _startTime = SystemClock.elapsedRealtime();
        }
        _frameCnt++;

        super.onDraw(canvas);


        int numBalls = this.balls.size();
        for(Ball ball : this.balls){
            // draw current
            canvas.drawCircle(ball.xLocation, ball.yLocation, ball.radius, ball.paint);
        }


        // The code below is about measuring and printing out fps calculations. You can ignore
        long endTime = SystemClock.elapsedRealtime();
        long elapsedTimeInMs = endTime - _startTime;
        if(elapsedTimeInMs > 1000){
            _actualFramesPerSecond = _frameCnt / (elapsedTimeInMs/1000f);
            _frameCnt = 0;
            _startTime = -1;
        }
        //MessageFormat: https://developer.android.com/reference/java/text/MessageFormat.html
        canvas.drawText(MessageFormat.format("fps: {0,number,#.#} | rotation: {1}", _actualFramesPerSecond, _display.getRotation()), 5, 40, _paintText);
        canvas.drawText(MessageFormat.format("balls: {0}", numBalls), 5, 80, _paintText);
        canvas.drawText(MessageFormat.format("raw accel: {0,number,#.#}, {1,number,#.#}, {2,number,#.#}", _rawAccelerometerValues[0], _rawAccelerometerValues[1], _rawAccelerometerValues[2]), 5, 120, _paintText);
        canvas.drawText(MessageFormat.format("smoothed : {0,number,#.#}, {1,number,#.#}, {2,number,#.#}", _linearAcceleration[0], _linearAcceleration[1], _linearAcceleration[2]), 5, 160, _paintText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // See: https://developer.android.com/training/gestures/multi.html
        int action = MotionEventCompat.getActionMasked(motionEvent);
        //Log.d("OnTouchEvent","The action is " + MotionEventUtils.actionToString(action));

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:

                // setup initial velocity to processed acceleration (not perfect, but something other than 0)
                float xVelocity = _processedAcceleration[0];
                float yVelocity = _processedAcceleration[1];

                //setup random radius
                float radius = Math.max(_random.nextFloat() * MAX_BALL_RADIUS, MIN_BALL_RADIUS);

                Ball ball = new Ball(radius, curTouchX, curTouchY, xVelocity, yVelocity, ArtColorUtils.getRandomOpaqueColor());
                this.balls.add(ball);

                invalidate();
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                if(_lastTimeAccelSensorChangedInMs == -1){
                    _lastTimeAccelSensorChangedInMs = SystemClock.elapsedRealtime();
                }
                long curTimeInMs = SystemClock.elapsedRealtime();

                // Set the raw values, the x accel value is in 0, the y accel value is in 1, and the z accel value is in 2
                _rawAccelerometerValues[0] = event.values[0];
                _rawAccelerometerValues[1] = event.values[1];
                _rawAccelerometerValues[2] = event.values[2];

                // smooth the accelerometer signal and remove gravity
                // from https://developer.android.com/guide/topics/sensors/sensors_motion.html
                // In this example, alpha is calculated as t / (t + dT),
                // where t is the low-pass filter's time-constant and
                // dT is the event delivery rate.
                final float alpha = 0.8f;

                // Isolate the force of gravity with the low-pass filter.
                _gravity[0] = alpha * _gravity[0] + (1 - alpha) * _rawAccelerometerValues[0];
                _gravity[1] = alpha * _gravity[1] + (1 - alpha) * _rawAccelerometerValues[1];
                _gravity[2] = alpha * _gravity[2] + (1 - alpha) * _rawAccelerometerValues[2];

                // Remove the gravity contribution with the high-pass filter.
                _linearAcceleration[0] = _rawAccelerometerValues[0] - _gravity[0];
                _linearAcceleration[1] = _rawAccelerometerValues[1] - _gravity[1];
                _linearAcceleration[2] = _rawAccelerometerValues[2] - _gravity[2];

                // Processed acceleration
                final int multiplier = 50;
                _processedAcceleration[0] = _linearAcceleration[0] * multiplier;
                _processedAcceleration[1] = _linearAcceleration[1] * multiplier;
                _processedAcceleration[2] = _linearAcceleration[2] * multiplier;

                // this makes it so that the accelerometer works in any orientation
                float temp = -1;
                switch (_display.getRotation()) {
                    case Surface.ROTATION_0:
                        _processedAcceleration[0] = -_processedAcceleration[0];
                        _processedAcceleration[1] = _processedAcceleration[1];
                        break;
                    case Surface.ROTATION_90:
                        temp = _processedAcceleration[0];
                        _processedAcceleration[0] = _processedAcceleration[1];
                        _processedAcceleration[1] = temp;
                        break;
                    case Surface.ROTATION_180:
                        _processedAcceleration[0] = _processedAcceleration[0];
                        _processedAcceleration[1] = -_processedAcceleration[1];
                        break;
                    case Surface.ROTATION_270:
                        temp = _processedAcceleration[0];
                        _processedAcceleration[0] = -_processedAcceleration[1];
                        _processedAcceleration[1] = -temp;
                        break;
                }

                // go through the balls and update their velocity and position
                for(Ball ball : this.balls){

                    // add the current acceleration to the velocity
                    // note: if you just want the balls to move based on current accel, change this to equals rather than +=
                    ball.xVelocity += _processedAcceleration[0];
                    ball.yVelocity += _processedAcceleration[1];

                    ball.xLocation += ball.xVelocity * (curTimeInMs - _lastTimeAccelSensorChangedInMs)/1000f;
                    ball.yLocation += ball.yVelocity * (curTimeInMs - _lastTimeAccelSensorChangedInMs)/1000f;

                    // check x ball boundary
                    if(ball.getRight() > getWidth()){
                        ball.xLocation = getWidth() - ball.radius;
                    }else if(ball.getLeft() < 0){
                        ball.xLocation = ball.radius;
                    }

                    // check y ball boundary
                    if(ball.getBottom() > getHeight()){
                        ball.yLocation = getHeight() - ball.radius;
                    }else if(ball.getTop() < 0){
                        ball.yLocation = ball.radius;
                    }
                }

                invalidate();
                _lastTimeAccelSensorChangedInMs = curTimeInMs;
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
