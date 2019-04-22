package com.example.teemu.trackingapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Objects;


/**
 *
 *       This class is responsible for handling bluetooth configurations.
 *       The UI communication are moved from here to main activity.
 *       @author Teemu Sormunen
 */
public class BluetoothConfigure extends Fragment implements AdapterView.OnItemClickListener,
        View.OnClickListener {

    /**
     * Tag for debug logging
     */
    private static final String TAG = "myApp";

    public BluetoothConnectionService mBluetoothConnectionService;

    // Set interface
    BluetoothInterface mBluetoothInterface;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBTDevice;

    View view;          // Store view to be accessed later
    Context context;    // Store context for Toast maketext
    ViewGroup container; // Store the viewgroup

    // Set UI variables
    Button btnStartConnection;
    Button btnStopConnection;
    Button btnSend;
    Button btnDirectConnect;
    Button btnONOFF;
    Button btnDiscoverDevices;
    TextView tvDirectAddress;
    EditText etDirectAddress;
    EditText etSend;
    ProgressBar loading;

    public ListView lvNewDevices;

    /**
     * Interface for communicating with main activity
     */
    public interface BluetoothInterface {

        /**
         * Initialize broadcast receiver responsible for detecting bluetooth status changes
         */
        void initBroadcast1();

        /**
         * Initialize broadcast receiver responsible for listing unpaired devices
         *
         * @param ID id of the raspberry to be searched
         */
        void initBroadcast3(String ID);

        /**
         * Initialize broadcast receiver which is responsible for bonding
         */
        void initBroadcast4();

        /**
         * Set search as direct or manual
         *
         * @param condition true or false
         */
        void setDirectSearch(boolean condition);

        /**
         * Enable bluetooth status
         */
        void enableBT();

        /**
         * Create bond between raspberry and phone
         *
         * @param position position of the item found from list
         */
        void createBond(int position);

        /**
         * Send message to raspberry
         *
         * @param bytes message in bytes
         */
        void writeMessage(byte[] bytes);

        /**
         * Start bluetooth connection
         */
        void startBTConnection();

        /**
         * Access BluetoothConfigure listview items from main activity
         *
         * @param lv listview
         * @param send Button send
         * @param startCon Button start connection
         * @param directCon Button choose direct connection
         * @param discover Button choose manual listview connection
         */
        void initUI(ListView lv, Button send, Button startCon, Button stopCon,
                    Button directCon, Button discover, ProgressBar loading);

        /**
         * @return bluetooth adapter
         */
        BluetoothAdapter getBluetoothAdapter();

        /**
         * @return is search direct or manual
         */
        boolean getDirectSearch();

        /**
         * @return is device discovering currently
         */
        boolean getIsDiscovering();

        /**
         * @param position position of device
         * @return return device located in the position
         */
        BluetoothDevice getDevice(int position);

       // public boolean connectStatus(); TO DO
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = (Activity) context;
        this.context = context;

        try {
            mBluetoothInterface = (BluetoothInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must override BluetoothInterface");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Broadcasts when bond state changes (ie:pairing)
        mBluetoothInterface.initBroadcast4();
    }

    /**
     * Initialize variables when view is created
     * @param v current view
     */
    public void initVar(View v) {
        // Define UI components
        btnSend = v.findViewById(R.id.btn_send);
        btnStartConnection = v.findViewById(R.id.btn_startConnection);
        btnStopConnection = v.findViewById(R.id.btn_stopConnection);
        btnDirectConnect = v.findViewById(R.id.btn_direct_connect);
        btnONOFF = v.findViewById(R.id.btnONOFF);
        btnDiscoverDevices = v.findViewById(R.id.btn_discover);
        tvDirectAddress = v.findViewById(R.id.tv_device_address);
        etDirectAddress = v.findViewById(R.id.et_address);
        etSend = v.findViewById(R.id.et_send);
        lvNewDevices = v.findViewById(R.id.lvNewDevices);
        loading = v.findViewById(R.id.pb_loading);

        mBluetoothAdapter = mBluetoothInterface.getBluetoothAdapter();

        // Hide user defined options as default
        etDirectAddress.setVisibility(View.INVISIBLE);
        tvDirectAddress.setVisibility(View.INVISIBLE);
        loading.setVisibility(View.INVISIBLE);
    }

    /**
     * Set onclicklisteners, init variables and inflate view here
     * @param inflater Inflates fragment to main container
     * @param container main container, holding all fragments
     * @param savedInstanceState last state that was held
     * @return - returns view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bluetooth_fragment, container, false);
        initVar(view);
        mBluetoothInterface.initUI(this.lvNewDevices, btnSend, btnStartConnection, btnStopConnection,
                btnDirectConnect, btnDiscoverDevices, loading);
        this.container = container;
        setRetainInstance(true);
        // Set onclicklisteners
        btnSend.setOnClickListener(this);
        btnDiscoverDevices.setOnClickListener(this);
        btnStartConnection.setOnClickListener(this);
        btnDirectConnect.setOnClickListener(this);
        btnONOFF.setOnClickListener(this);

        return view;
    }

    /**
     * Force portrait mode. UI looks much better in landscape, and is more usable
     *
     * @param isVisibleToUser Is fragment visible to user
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
     * Find which button has been clicked, and deal with it.
     * @param v view where it's clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnONOFF:
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
                break;

            case R.id.btn_direct_connect:
                // Hide ListView when direct connect is chosen
                lvNewDevices.setVisibility(View.INVISIBLE);
                tvDirectAddress.setVisibility(View.VISIBLE);
                etDirectAddress.setVisibility(View.VISIBLE);
                mBluetoothInterface.setDirectSearch(true);
                mBluetoothAdapter.cancelDiscovery();
                break;

            case R.id.btn_discover:
                // Hide direct connect options when manual browsing is chosen
                lvNewDevices.setVisibility(View.VISIBLE);
                tvDirectAddress.setVisibility(View.INVISIBLE);
                etDirectAddress.setVisibility(View.INVISIBLE);
                mBluetoothInterface.setDirectSearch(false);
                btnDiscover();
                break;
            case R.id.btn_startConnection:
                if (mBluetoothInterface.getDirectSearch()) {
                    btnDiscover();
                } else
                    mBluetoothInterface.startBTConnection();
                break;
            case R.id.btn_send:

                // Get characters from EditText and write them to other device
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothInterface.writeMessage(bytes);
                break;
            case R.id.lvNewDevices:
                lvNewDevices.setOnItemClickListener(BluetoothConfigure.this);
        }
    }

    /**
     * Called when item is clicked in listview
     * @param adapterView View where items are stored
     * @param view View where adapter is stored
     * @param position Position of item that is clicked
     * @param l not used
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick: You Clicked on a device.");
        this.mBTDevice = mBluetoothInterface.getDevice(position);

        Log.d(TAG, "onItemClick: deviceName = " + mBTDevice.getName());
        Log.d(TAG, "onItemClick: deviceAddress = " + mBTDevice.getAddress());
        Toast.makeText(context, "item chosen: " + mBTDevice.getName(), Toast.LENGTH_SHORT).show();

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + mBTDevice.getName());
            mBluetoothInterface.createBond(position);
        }
    }



    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = getActivity().checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += getActivity().checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    /**
     * Enables or disables bluetooth status
     */
    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            mBluetoothInterface.enableBT();
            mBluetoothInterface.initBroadcast1();   // make IntentFilter for listening action changes
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            mBluetoothInterface.initBroadcast1();
        }
    }

    /**
     * Start discovery of all nearby devices
     */
    public void btnDiscover() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");
        String ID = "";
        if (mBluetoothInterface.getDirectSearch())
            ID = etDirectAddress.getText().toString();

        if (mBluetoothInterface.getIsDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            mBluetoothInterface.initBroadcast3(ID);
        }
        if (!mBluetoothInterface.getIsDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            // Start discovery and make an intent filter for finding action
            mBluetoothAdapter.startDiscovery();
            mBluetoothInterface.initBroadcast3(ID);
        }
    }

}
