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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;
import java.util.Objects;


/**
 *      This class is for controlling graph fragment.
 *      It's purpose is to show different data outputs as a graph.
 *      @author Teemu Sormunen
 */
public class Graphs extends Fragment implements View.OnClickListener {

    /**
     * Tag for debug logging
     */
    private static final String TAG = "myApp";

    LineGraphSeries<DataPoint> seriesLatitude;
    LineGraphSeries<DataPoint> seriesLongitude;
    private float[] latitude;
    private float[] longitude;


    // Create callback for interface
    OnSendDataInterface sendDataInterface;

    // Create instance of BluetoothConfigure to play with
    // BluetoothConnectionService via BluetoothConfigure
    BluetoothConfigure bluetoothConfigure;

    Context context;
    // Define UI elements
    GraphView latitudeGraph;
    GraphView longitudeGraph;
    Button btnGpsData;
    Button btnKalmanData;

    /**
     * Interface for communicating with main activity
     */
    public interface OnSendDataInterface {

        /**
         * Methods for communicating between fragments, and/or main activity
         *
         * @param message - message that is sent to raspberry
         */
        public void onDataSend(String message);

        /**
         * Initialize graphs in graph fragment
         *
         * @param gpsData button: gpsdata
         * @param IMUData button: IMUdata
         */
        public void initUIGraphs(Button gpsData, Button IMUData);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Set onclicklisteners, and initialize broadcastmanager for listening
     * @param inflater - Inflates fragment to main container
     * @param container - main container, holding all fragments
     * @param savedInstanceState - last state that was held
     * @return - returns view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.graph_fragment, container, false);
        initVar(view);
        sendDataInterface.initUIGraphs(btnGpsData, btnKalmanData);
        btnGpsData.setOnClickListener(this);
        btnKalmanData.setOnClickListener(this);

        // Register broadcastlistener here
        Activity activity = (Activity) context;
        LocalBroadcastManager.getInstance(activity).registerReceiver(dataBroadcastReceiver,
                new IntentFilter("graph.data"));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * Initialize variables
     * @param v - pass view
     */
    private void initVar(View v) {
        // Init all the UI components
        latitudeGraph = (GraphView) v.findViewById(R.id.latitude_graph);
    //    longitudeGraph = v.findViewById(R.id.longitude_graph);
        btnGpsData = v.findViewById(R.id.btn_gps_data);
        btnKalmanData = v.findViewById(R.id.btn_kalman_data);
    }

    /**
     * Force portrait mode for better user experience
     *
     * @param isVisibleToUser - is fragment visible to user
     */
    @Override
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
     * Create graph of your preferred data set
     * @param arraySize - amount of data points included
     */
    private void createLatitudeGraph(int arraySize) {
        // Local variable for x-axis values
        float x = 0;
        double latMax = 0;
        double latMin = 99;
        seriesLatitude = new LineGraphSeries<>();
        for (int i = 0; i < arraySize; i++) {
            x += 0.01;
            seriesLatitude.appendData(new DataPoint(x, latitude[i]), true, arraySize);
            if(latitude[i] < latMin)
                latMin = latitude[i];
            if(latitude[i] > latMax)
                latMax = latitude[i];
        }
        seriesLatitude.setTitle("GPS Latitude");
        seriesLatitude.setDataPointsRadius(5);
        latitudeGraph.getViewport().setMaxY(latMax);
        latitudeGraph.getViewport().setMinY(latMin);
        latitudeGraph.getViewport().setYAxisBoundsManual(true);
        latitudeGraph.addSeries(seriesLatitude);
    }

    /**
     * Request correct graph data
     * @param v Get correct button that has been clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_gps_data:
                String getGps = "getGPS";
                sendDataInterface.onDataSend(getGps);
                break;
            case R.id.btn_kalman_data:
                String getKalman = "getKalman";
                sendDataInterface.onDataSend(getKalman);
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        this.context = context;

        Activity activity = (Activity) context;

        try {
            sendDataInterface = (OnSendDataInterface) activity;
        } catch (ClassCastException e){
            throw new ClassCastException(activity.toString() + " must override onDataSend");
        }
    }

    /**
     * Unregisted broadcastreceiver on destroy
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(Objects.requireNonNull(getActivity())).unregisterReceiver(dataBroadcastReceiver);
    }

    /**
     * Broadcastreceiver for receiving data from BluetoothConnectionService
     */
    private final BroadcastReceiver dataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Debug.print(TAG, "Broadcast received: " + action, 2, "console");
            if (action != null && action.equals("graph.data")) {
                List<String> state = intent.getExtras().getStringArrayList("data1");
                Debug.print(TAG, "String before parsin: " + state, 2, "console");
                int startIndex = 0;
                String[] parsed;
                String temp = "";
                for(int i = 0; i < state.size(); i++) {
                    temp += state.get(i);
                }
                parsed = temp.split("[^\\d\\.]");
                latitude = new float[parsed.length/2];   // Rough estimation of array size needed
                for (int i = 0; i < parsed.length; i++) {
                    if (parsed[i].matches(".*\\d+.*")) {
                        latitude[startIndex] = Float.valueOf(parsed[i]);     // Add valid numbers to newly created array.
                        Debug.print(TAG, "value: " + latitude[startIndex] + " stored to array", 2, "console");
                        startIndex += 1;
                    }
                }
                createLatitudeGraph(startIndex);
            }
        }
    };
}

