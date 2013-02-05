
package com.android.calibrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.SystemProperties;

import java.io.File;
import java.io.FileInputStream;
import java.util.StringTokenizer;

public class BootdoneReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Intent mBootIntent = new Intent(context, AndroidCalibrate.class);
        mBootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        File desFile = new File("/data/etc/pointercal");
        if (!desFile.exists()) {
            String strCal = SystemProperties.get("dev.calibrate.parameters", "null");
            if (true == strCal.equalsIgnoreCase("null"))
                context.startActivity(mBootIntent);
            else
                Calibrate.setCalibrateParameters(strCal, "1");
        }
        else {
            try {
                FileInputStream file_cal = new FileInputStream(desFile);
                byte[] mBuffer = new byte[64];
                int len = file_cal.read(mBuffer);
                file_cal.close();
                if (len > 0) {
                    int i;
                    for (i = 0; i < len; i++) {
                        if (mBuffer[i] == '\n' || mBuffer[i] == 0) {
                            break;
                        }
                    }
                    len = i;
                }
                Calibrate.setCalibrateParameters(new String(mBuffer, 0, 0, len), "1");
                StringTokenizer st = new StringTokenizer(new String(mBuffer, 0, 0, len), ",");
            } catch (java.io.FileNotFoundException e) {
                Log.i("InputDevice", "FileNotFound!");
            } catch (java.io.IOException e) {
                Log.i("InputDevice", "IOException");
            }
        }

    }
}
