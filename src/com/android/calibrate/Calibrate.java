
package com.android.calibrate;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Calibrate {
    private calibration cal;
    private static final String sys_path = "/sys/module/at91_adc/parameters/";
    public Calibrate() {
        cal = new calibration();
        String strPara = String.format("%d,%d,%d,%d,%d,%d,%d", 0,
                0, 0, 0, 0, 0, 0);
        setCalibrateParameters(strPara, "0");
    }

    class calibration {
        int x[] = new int[5];
        int xfb[] = new int[5];
        int y[] = new int[5];
        int yfb[] = new int[5];
        int a[] = new int[7];
    };

    public boolean IsCalibrationOk(int error, int screenWidth, int screenHeight) {
        int xDiff = 0, yDiff = 0;
        int xSlope = 0, ySlope = 0;
        Boolean xOk = false, yOk = false;

        int cal_x = (cal.x[4] * cal.a[1] + cal.y[4] * cal.a[2] + cal.a[0]) / cal.a[6];
        int cal_y = (cal.x[4] * cal.a[4] + cal.y[4] * cal.a[5] + cal.a[3]) / cal.a[6];
        xDiff = Math.abs(screenWidth / 2 - cal_x);
        yDiff = Math.abs(screenHeight / 2 - cal_y);
        xOk = (xDiff >= -error) && (xDiff <= error);
        yOk = (yDiff >= -error) && (yDiff <= error);
        System.out.printf("OK?:cal.fb[4] x=%d,y=%d   (%d,%d)\n", cal_x, cal_y, screenWidth,
                screenHeight);
        if (xOk && yOk)
            return true;
        else
            return false;

    }

    void CalRaw2Rel(int xSlope, int ySlope, int relX[], int relY[], int screenWidth,
            int screenHeight) {

        relX[0] = (cal.xfb[0] + (Math.abs(cal.x[0] - cal.x[4]) * 1024) / xSlope);
        relY[0] = (cal.yfb[0] + (Math.abs(cal.y[0] - cal.y[4]) * 1024) / ySlope);

        if (relX[0] < 0) {
            relX[0] = 0;
        }

        if (relX[0] > screenWidth) {
            relX[0] = screenWidth;
        }

        if (relY[0] < 0) {
            relY[0] = 0;
        }

        if (relY[0] > screenHeight) {
            relY[0] = screenHeight;
        }
    }

    boolean perform_calibration() {
        Log.i("TS_Calibration", "perform_calibration");
        int j;
        float n, x, y, x2, y2, xy, z, zx, zy;
        float det, a, b, c, e, f, i;
        float scaling = (float) 65536.0;

        // Get sums for matrix
        n = x = y = x2 = y2 = xy = 0;
        for (j = 0; j < 5; j++) {
            n += 1.0;
            x += (float) cal.x[j];
            y += (float) cal.y[j];
            x2 += (float) (cal.x[j] * cal.x[j]);
            y2 += (float) (cal.y[j] * cal.y[j]);
            xy += (float) (cal.x[j] * cal.y[j]);
        }

        // Get determinant of matrix -- check if determinant is too small
        det = n * (x2 * y2 - xy * xy) + x * (xy * y - x * y2) + y
                * (x * xy - y * x2);
        if (det < 0.1 && det > -0.1) {
            Log.i("ts_calibrate: determinant is too small -- %f\n", "" + det);
            return false;
        }

        // Get elements of inverse matrix
        a = (x2 * y2 - xy * xy) / det;
        b = (xy * y - x * y2) / det;
        c = (x * xy - y * x2) / det;
        e = (n * y2 - y * y) / det;
        f = (x * y - n * xy) / det;
        i = (n * x2 - x * x) / det;

        // Get sums for x calibration
        z = zx = zy = 0;
        for (j = 0; j < 5; j++) {
            z += (float) cal.xfb[j];
            zx += (float) (cal.xfb[j] * cal.x[j]);
            zy += (float) (cal.xfb[j] * cal.y[j]);
        }

        // Now multiply out to get the calibration for framebuffer x coord
        cal.a[0] = (int) ((a * z + b * zx + c * zy) * (scaling));
        cal.a[1] = (int) ((b * z + e * zx + f * zy) * (scaling));
        cal.a[2] = (int) ((c * z + f * zx + i * zy) * (scaling));

        System.out.printf("%f %f %f\n", (a * z + b * zx + c * zy), (b * z + e
                * zx + f * zy), (c * z + f * zx + i * zy));

        // Get sums for y calibration
        z = zx = zy = 0;
        for (j = 0; j < 5; j++) {
            z += (float) cal.yfb[j];
            zx += (float) (cal.yfb[j] * cal.x[j]);
            zy += (float) (cal.yfb[j] * cal.y[j]);
        }

        // Now multiply out to get the calibration for framebuffer y coord
        cal.a[3] = (int) ((a * z + b * zx + c * zy) * (scaling));
        cal.a[4] = (int) ((b * z + e * zx + f * zy) * (scaling));
        cal.a[5] = (int) ((c * z + f * zx + i * zy) * (scaling));

        System.out.printf("%f %f %f\n", (a * z + b * zx + c * zy), (b * z + e
                * zx + f * zy), (c * z + f * zx + i * zy));

        // If we got here, we're OK, so assign scaling to a[6] and return
        cal.a[6] = (int) scaling;
        return true;
        /*
         * // This code was here originally to just insert default values
         * for(j=0;j<7;j++) { c->a[j]=0; } c->a[1] = c->a[5] = c->a[6] = 1;
         * return 1;
         */
    }

    boolean get_sample(int index, int x1, int y1, int x, int y) {
        Log.i("TS_Calibration", "get_sample");
        cal.y[index] = y1;
        cal.x[index] = x1;
        cal.xfb[index] = x;
        cal.yfb[index] = y;
        return true;
    }

    static public void setCalibrateParameters(String strPara, String strCal) {
        try {
            String[] values = strPara.split(",");

            if (values.length == 7) {
                final String prefix = sys_path;
                stringToFile(prefix + "tx1", values[0]);
                stringToFile(prefix + "ty1", values[1]);
                stringToFile(prefix + "tz1", values[2]);
                stringToFile(prefix + "tx2", values[3]);
                stringToFile(prefix + "ty2", values[4]);
                stringToFile(prefix + "tz2", values[5]);
                stringToFile(prefix + "ts", values[6]);
                stringToFile(prefix + "calibrated", strCal);
            } else {
                Log.i("Calibrate", "Invalid buffersize string: " + strPara);
            }
        } catch (IOException e) {
            Log.i("Calibrate", "Can't set parameters:" + e);
        }
    }

    static public void stringToFile(String filename, String string) throws IOException {
        FileWriter out = new FileWriter(filename);
        try {
            out.write(string);
        } finally {
            out.close();
        }
    }

    static public int getRawX() {
        String filename = sys_path + "rawX";
        String line;
        try {
            BufferedReader input = new BufferedReader(new FileReader(filename));
            if ((line = input.readLine()) != null)
                return Integer.parseInt(line);
        } catch (IOException e) {
            Log.i("Calibrate", "Can't get parameters raw x:" + e);
        }
        return -1;
    }

    static public int getRawY() {
        String filename = sys_path + "rawY";
        String line;
        try {
            BufferedReader input = new BufferedReader(new FileReader(filename));
            if ((line = input.readLine()) != null)
                return Integer.parseInt(line);
        } catch (IOException e) {
            Log.i("Calibrate", "Can't get parameters raw Y:" + e);
        }
        return -1;
    }

    int calibrate_main() {
        int result = 0;
        Log.i("TS_Calibration", "calibrate_main");
        if (cal.a[6] == 65536) {
            String strPara = String.format("%d,%d,%d,%d,%d,%d,%d", cal.a[1],
                    cal.a[2], cal.a[0], cal.a[4], cal.a[5], cal.a[3], cal.a[6]);
            setCalibrateParameters(strPara, "1");
            boolean success = new File("/data/etc").mkdir();
            if (!success) {
                Log.i(this.toString(), "Make dir /data/etc failed");
            }

            File desFile = new File("/data/etc/pointercal");
            if (!desFile.exists())
                try {
                    desFile.createNewFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            FileOutputStream fos;

            try {
                fos = new FileOutputStream(desFile);
                byte[] buf = strPara.getBytes();
                int bytesRead = buf.length;
                try {
                    fos.write(buf, 0, bytesRead);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            result = 0;

        } else {
            result = -1;
        }
        return result;
    }

}
