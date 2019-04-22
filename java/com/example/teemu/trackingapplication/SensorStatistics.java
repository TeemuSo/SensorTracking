package com.example.teemu.trackingapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Objects;


/**
 *      This class is for streaming sensors real time data
 *      @author Teemu Sormunen
 */
public class SensorStatistics extends Fragment {

    RequestSensorInterface requestSensorInterface;
    Button btnGetData;
    Button btnStopMonitor;

    // Initialize UI elements
    TextView accelX;
    TextView accelY;
    TextView accelZ;
    TextView pitch;
    TextView yaw;
    TextView roll;

    /**
     * Interface for requesting sensor data, and initializing UI components in main activity
     */
    public interface RequestSensorInterface {

        /**
         * Control whether real time sensor data is wanted
         *
         * @param request true or false, start streaming or stop it
         */
        void requestSensor(boolean request);

        /**
         * Initialize sensor fragments UI components in main activity
         * @param btnGetData button: start monitoring data
         * @param btnStopMonitor button: stop monitoring data
         */
        void initUISensor(Button btnGetData, Button btnStopMonitor);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.realtime_fragment, container, false);
        initVar(view);

        btnGetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInterface();
            }
        });

        btnStopMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestSensorInterface != null){
                    requestSensorInterface.requestSensor(false);
                }
            }
        });


        Activity activity = getActivity();
        LocalBroadcastManager.getInstance(activity).registerReceiver(dataBroadcastReceiver,
                new IntentFilter("realtime.data"));

        // Create interface
        requestSensorInterface = (SensorStatistics.RequestSensorInterface) activity;
        requestSensorInterface.initUISensor(btnGetData, btnStopMonitor);
        return view;
    }

    /**
     * Initialize variables in sensor statistics fragment
     * @param view view of the fragment
     */
    private void initVar(View view) {
        btnGetData = view.findViewById(R.id.btn_get_realtime);
        btnStopMonitor = view.findViewById(R.id.btn_stop_monitor);
        accelX = view.findViewById(R.id.tv_acc_x);
        accelY = view.findViewById(R.id.tv_acc_y);
        accelZ = view.findViewById(R.id.tv_acc_z);
        pitch = view.findViewById(R.id.tv_pitch);
        yaw = view.findViewById(R.id.tv_yaw);
        roll = view.findViewById(R.id.tv_roll);


    }

    /**
     * Process data which is sent by Raspberry Pi (RPi). Use RegEx for parsing the data
     * in to right format - this requires data to be in same format each time it is
     * sent from RPi.
     */
    private final BroadcastReceiver dataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Debug.print("TAG", "Broadcast received: " + action, 2, "console");
            if (action != null && action.equals("realtime.data")) {
                String state = intent.getExtras().getString("data");
                Debug.print("TAG", "String before parsin: " + state, 2, "console");
                accelX.setText("x: " + state.split("[(' ]")[2]);
                accelY.setText("y: " + state.split("[(' ]")[3]);
                accelZ.setText("z: " + state.split("[(' ]")[4]);

                pitch.setText("pitch: " + state.split("[(' ]")[7]);
                yaw.setText("yaw: " + state.split("[(' ]")[8]);
                roll.setText("roll: " + state.split("[(' ]")[9]);
            }
        }
    };

    /**
     * Creates interface for sending wanted commands -> main activity -> raspberry
     */
    public void setInterface(){
        //this.context = context;

        Activity activity = getActivity();

        try {
            assert requestSensorInterface != null;
            requestSensorInterface.requestSensor(true);
        } catch (ClassCastException e){
            throw new ClassCastException(activity.toString() + " must override onDataSend");
        }
    }

    /**
     * Force orientation to portrait mode for better user experience
     * @param isVisibleToUser is fragment visible to user
     */
    // Set orientation portrait
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Activity curActivity = getActivity();
            if (curActivity != null) {
                curActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    /**
     * Unregister broadcastreceiver when destroying the view.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getActivity())).unregisterReceiver(dataBroadcastReceiver);
    }
}
