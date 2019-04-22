package com.example.teemu.trackingapplication;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

/**
 *          Debugging class.
 *          @author Teemu Sormunen
 */
public class Debug {

    public static Context context;
    public static Toast debug;

    // Set PRINT_METHOD either as 'console' or 'interface'
    // If you are running a thread, do NOT set PRINT_METHOD as interface

    // For loading the correct context of DEBUG_LEVEL
    public static void loadDebug(Context host) {
        context = host.getApplicationContext();
    }


    /**
     * Print messages
     * @param myActivity ID for which logging is stored in
     * @param s text to be logged
     * @param level priority level where logging is done to
     * @param PRINT_METHOD printed either to 'console' or 'interface'
     */
    public static void print(String myActivity, String s, int level, String PRINT_METHOD) {

        // Print depending on the level chosen
        // Also, choose to print either on user interface or console
        if (level == 1) {
            if(PRINT_METHOD.equals("console"))
                Log.v(myActivity, s);
            if(PRINT_METHOD.equals("interface")) {
                debug = Toast.makeText(context, s , Toast.LENGTH_SHORT);
                debug.show();
            }
        }

        if (level == 2) {
            if (BuildConfig.DEBUG) {
                if (PRINT_METHOD.equals("console"))
                    Log.d(myActivity, s);
                if (PRINT_METHOD.equals("interface")) {
                    debug = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                    debug.show();
                }
            }
        }

        if (level == 3) {
            if (PRINT_METHOD.equals("console"))
                Log.i(myActivity, s);
            if (PRINT_METHOD.equals("interface")) {
                debug = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                debug.show();
            }
        }

        if (level == 4) {
            if (PRINT_METHOD.equals("console"))
                Log.w(myActivity, s);
            if (PRINT_METHOD.equals("interface")) {
                debug = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                debug.show();
            }
        }

        if (level == 5) {
            if (PRINT_METHOD.equals("console"))
                Log.e(myActivity, s);
            if (PRINT_METHOD.equals("interface")) {
                debug = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                debug.show();
            }
        }


    }

}