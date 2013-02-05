
package com.android.calibrate;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

class ScreenDisplay {
    public int UI_SCREEN_WIDTH = 800;
    public int UI_SCREEN_HEIGHT = 480;
    int WIDTH = 1024;
    int HEIGHT = 1024;
    public int xCal[] = new int[5];
    public int yCal[] = new int[5];
    public int xList[] = new int[5];
    public int yList[] = new int[5];

    public void setScreen(int width, int height) {
        UI_SCREEN_WIDTH = width;
        UI_SCREEN_HEIGHT = height;
        int xcal[] = {
                UI_SCREEN_WIDTH / 10, UI_SCREEN_WIDTH - UI_SCREEN_WIDTH / 10,
                UI_SCREEN_WIDTH - UI_SCREEN_WIDTH / 10, UI_SCREEN_WIDTH / 10, UI_SCREEN_WIDTH / 2
        };
        int ycal[] = {
                UI_SCREEN_HEIGHT / 10, UI_SCREEN_HEIGHT / 10,
                UI_SCREEN_HEIGHT - UI_SCREEN_HEIGHT / 10, UI_SCREEN_HEIGHT - UI_SCREEN_HEIGHT / 10,
                UI_SCREEN_HEIGHT / 2
        };
        int xlist[] = {
                WIDTH / 10, WIDTH - WIDTH / 10, WIDTH - WIDTH / 10, WIDTH / 10, WIDTH / 2
        };
        int ylist[] = {
                HEIGHT / 10, HEIGHT / 10, HEIGHT - HEIGHT / 10, HEIGHT - HEIGHT / 10, HEIGHT / 2
        };
        for (int i = 0; i < 5; i++)
        {
            xList[i] = xlist[i];
            yList[i] = ylist[i];

        }

        for (int i = 0; i < 5; i++)
        {
            xCal[i] = xcal[i];
            yCal[i] = ycal[i];

        }

    }
}

public class AndroidCalibrate extends Activity {
    final String TAG = "TSCalibration";
    ScreenDisplay screen;
    final int errorMax = 50;
    int direction;
    CrossView myview;

    private Calibrate cal;

    private void setFullScreen(ScreenDisplay screen) {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screen.setScreen(dm.widthPixels, dm.heightPixels);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen = new ScreenDisplay();
        setFullScreen(screen);
        myview = new CrossView(this);
        setContentView(myview);

        cal = new Calibrate();
        direction = 0;

        myview.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                int X = Calibrate.getRawX();
                int Y = Calibrate.getRawY();
                Log.i("OnTouch", X + "," + Y);
                v.invalidate();
                if (direction <= 4) {
                    Log.i("TS_Calibration time onTouchListener", " " + direction);
                    cal.get_sample(direction, X, Y, screen.xList[direction],
                            screen.yList[direction]);
                }
                if (direction == 4) {
                    cal.perform_calibration();
                    if (cal.IsCalibrationOk(errorMax, screen.WIDTH, screen.HEIGHT)) {
                        cal.calibrate_main();
                        Toast.makeText(getBaseContext(), "Calibrate Done!", Toast.LENGTH_SHORT)
                                .show();

                        AndroidCalibrate.this.finish();
                    }
                    else
                    {
                        Toast.makeText(getBaseContext(),
                                "Calibration error is too large\nPlease try again!",
                                Toast.LENGTH_SHORT).show();
                        direction = -1;

                    }
                }
                direction++;
                return false;
            }
        });
    }

    public class CrossView extends View {
        public CrossView(Context context) {
            super(context);
        }

        public void onDraw(Canvas canvas) {

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            if ((direction > -1) && (direction < 5)) {
                DrawCalibrationPoints(direction, paint, canvas);
            }
            canvas.drawText(getResources().getString(R.string.app_name),
                    screen.UI_SCREEN_WIDTH / 2 - 50, screen.UI_SCREEN_HEIGHT / 2, paint);
            super.onDraw(canvas);
        }

        public void DrawCalibrationPoints(int deret, Paint p, Canvas canvas)
        {
            canvas.drawLine(screen.xCal[direction] - 10, screen.yCal[direction],
                    screen.xCal[direction] + 10, screen.yCal[direction], p);
            canvas.drawLine(screen.xCal[direction], screen.yCal[direction] - 10,
                    screen.xCal[direction], screen.yCal[direction] + 10, p);
            p.setColor(Color.WHITE);
        }
    }
}
