/**
 *  <p>This application creates bluetooth connection to monitor and control
 *  mobile robot usage. It's intended to be used with Raspberry Pi 3.
 *  In GitHub there is Python code, which can be downloaded and used in co-operation.
 *  This is not to be used as it is, if pre-written Python scripts are not used.
 *  </p><br>
 * @author Teemu Sormunen
 * @version 22.04.2019
 * <p>Minimum SDK version: 26
 * Target SDK version: 28</p>
 *
 *
 */

package com.example.teemu.trackingapplication;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;


/**
 *          This class is main activity. It's responsible for forwarding
 *          data between separate fragments, and as well as working as
 *          main structure behind other classes.
 *          @author Teemu Sormunen
 */
public class MainActivity extends AppCompatActivity implements Graphs.OnSendDataInterface, BluetoothConfigure.BluetoothInterface,
        SensorStatistics.RequestSensorInterface,
        MovementControl.MovementControlInterface, BluetoothConnectionService.ConnectionStatusInterface {
    /**
     * Tag for debug logging
     */
    private static final String TAG = "myApp";

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private SectionsPageAdapter mSectionsPageAdapter;

    private static final UUID MY_UUID =
            UUID.fromString("7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d");
    private String RPi_ID = "B8:27:EB:FE:7D:34";
    boolean directSearch = false;
    boolean connectionStatus = false;

    // Access to BluetoothConfigure UI elements
    ListView lvDevices;
    private Button btnStartConnection;
    private Button btnStopConnection;
    private Button btnSend;
    private Button btnDirectConnect;
    private Button btnDiscoverDevices;

    // Access MovementControl UI elements
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private ImageButton btnRight;
    private ImageButton btnLeft;
    private ProgressBar loading;

    // Access SensorStatistics UI elements
    Button btnGetData;
    Button btnStopMonitor;

    Thread waitingThread;   // Thread for controlling progress bar

    // Action bar items
    ImageView mBTStatus;
    ImageView mConnectStatus;

    // Access to Graphs UI elements
    Button btnGpsData;
    Button btnIMUData;

    // Device details
    BluetoothDevice mBluetoothDevice;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    BluetoothAdapter mBluetoothAdapter;

    public static BluetoothConfigure mBluetoothConfigure;

    /**
     * This broadcast receiver works as listener, whether bluetooth state has had any changes.
     * It also controls buttons clickability, and icon that indicates whether bluetooth
     * is enabled or not.
     */
    public final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        connectionStatus(false);
                        showButtons(false);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        connectionStatus(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        showButtons(true);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as
     * discoverability mode on/off or expire.
     */
    public final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * Executed by btnDiscover() method.
     */
    public BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            // If searching just to list items in a listview
            if (action.equals(BluetoothDevice.ACTION_FOUND) && !directSearch) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvDevices.setAdapter(mDeviceListAdapter);
            }
            // If searching with specific address
            if (action.equals(BluetoothDevice.ACTION_FOUND) && directSearch) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals(RPi_ID)) {                                                   // Assign actual id from device address
                    mBluetoothDevice = device;
                    mBluetoothDevice.createBond();
                    mBluetoothAdapter.cancelDiscovery();

                    // Create new instance of the BTCS
                    mBluetoothConfigure.mBluetoothConnectionService = new BluetoothConnectionService(MainActivity.this);
                    startConnection();
                }
            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    public final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBluetoothDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    /**
     * Initialize customizations that are made behind fragments.
     * Initialize variables.
     * @param savedInstanceState previously saved instance of the state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create custom top bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_toolbar);
        getSupportActionBar().setElevation(0);

        LocalBroadcastManager.getInstance(this).registerReceiver(mdiscoverTimingReceiver,
                new IntentFilter("discover.status"));

        // Setup viewPager, and setup it with current fragments
        // Also setup actionbar variables
        mViewPager = findViewById(R.id.container);
        mBTStatus = findViewById(R.id.bluetooth_status);
        mConnectStatus = findViewById(R.id.connection_status);
        mConnectStatus.setImageResource(R.drawable.not_on2);

        setupViewPager(mViewPager);

        mTabLayout = findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        initVar();

        // Start interface communication with Graphs
        if (findViewById(R.id.graph_fragment) != null) {
            if (savedInstanceState != null) {
                return;
            }
            Graphs graphs = new Graphs();

            FragmentTransaction fragmentTransaction1 = getSupportFragmentManager().beginTransaction().add(R.id.graph_fragment, graphs, null);
            fragmentTransaction1.commit();
        }

        // Start interface communication with BluetoothConfigure
        if (findViewById(R.id.bluetooth_fragment) != null) {
            if (savedInstanceState != null) {
                return;
            }

            BluetoothConfigure bluetoothConfigure = new BluetoothConfigure();

            FragmentTransaction fragmentTransaction2 = getSupportFragmentManager().beginTransaction().add(R.id.bluetooth_fragment, bluetoothConfigure, null);
            fragmentTransaction2.commit();
        }
    }

    /**
     * Initialize variables that are used for inter-fragment communication.
     */
    private void initVar() {
        if (mBluetoothConfigure == null)
            mBluetoothConfigure = getBluetoothConfigure();
        mBTDevices = new ArrayList<>();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    /**
     * Add Fragments to the view pager -container
     *
     * @param viewPager - holds all the fragments
     */
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new BluetoothConfigure(), "BluetoothConfigure");
        adapter.addFragment(new Graphs(), "Graphs");
        adapter.addFragment(new SensorStatistics(), "SensorStatistics");
        adapter.addFragment(new MovementControl(), "Movements");
        viewPager.setAdapter(adapter);
    }


    /**
     * Call this when 'start connection' -button is pressed.
     */
    public void startConnection() {
        Debug.print(TAG, "address achieved is " + mBluetoothDevice, 2, "console");
        startBTConnection(mBluetoothDevice, MY_UUID);
    }

    /**
     * Start the actual routine for initiating the bluetooth communication.
     * @param device device with which communication is started.
     * @param uuid UUID of the device to be connected to.
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Debug.print(TAG, "startBTConnection: initializing RFCOM connection", 2, "console");
        mBluetoothConfigure.mBluetoothConnectionService.startClient(device, uuid);  // Start client with this command
    }


    /**
     * Sets buttons as 'clickable' or not.
     * Changes icon which indicates whether bluetooth is on or off
     *
     * @param condition - true or false, enable or disable
     */
    public void showButtons(boolean condition) {
        btnStartConnection.setEnabled(condition);
        btnDirectConnect.setEnabled(condition);
        btnDiscoverDevices.setEnabled(condition);
        btnSend.setEnabled(condition);


        // Set action bar images to indicate on or off
        if (condition) {
            mBTStatus.setImageResource(R.drawable.on);

            btnSend.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnStartConnection.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnDirectConnect.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnDiscoverDevices.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
        } else {
            mBTStatus.setImageResource(R.drawable.not_on2);
            // Set buttons as disabled
            btnSend.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnStartConnection.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnDirectConnect.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnDiscoverDevices.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
        }
    }

    /**
     * Unregisted broadcastreceivers on destroy.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
            unregisterReceiver(mdiscoverTimingReceiver);
            //mBluetoothAdapter.cancelDiscovery();
        } catch (IllegalArgumentException e) {
            Debug.print(TAG, "NOT REGISTERED: " + e, 2, "console");
        }
    }

    public static BluetoothConfigure getBluetoothConfigure() {
        mBluetoothConfigure = new BluetoothConfigure();
        return mBluetoothConfigure;
    }

    @Override
    public void onDataSend(String message) {
        byte[] bytes = message.getBytes();
        if (mBluetoothConfigure == null) {
            mBluetoothConfigure = getBluetoothConfigure();
            mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
        } else {
            mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
        }
    }

    public void initBroadcast4() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);
    }


    public void cancelDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    public void startDiscovery() {
        mBluetoothAdapter.startDiscovery();
    }

    public void setDirectSearch(boolean condition) {
        directSearch = condition;
    }

    public boolean getDirectSearch() {
        return directSearch;
    }

    public boolean getIsDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }


    public void initBroadcast3(String ID) {
        this.RPi_ID = ID;
        Debug.print("myApp", "Init broadcast entered", 2, "console");
        // IF direct search,
        // Create listener for when searching has been too long
        if (directSearch) {
            // Register broadcastlistener here
            loading.setVisibility(View.VISIBLE);
            waitingThread = new Thread(new DiscoverSleepThread(getApplicationContext()), "sleepThread");
            waitingThread.start();
        }

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
    }

    /**
     * For checking whether to stop discovery mode
     */
    private final BroadcastReceiver mdiscoverTimingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Debug.print(TAG, "Broadcast received", 2, "console");
            if (action != null && action.equals("discover.status")) {
                cancelDiscovery();
                if (!connectionStatus)
                    Toast.makeText(getApplicationContext(), "Connect again. Nothing found.", Toast.LENGTH_LONG).show();
                loading.setVisibility(View.INVISIBLE);
            }

        }
    };


    public void initBroadcast1() {
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);
    }

    public void enableBT() {
        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBTIntent);
    }

    public BluetoothDevice getDevice(int position) {
        return mBTDevices.get(position);
    }

    public void createBond(int position) {
        mBTDevices.get(position).createBond();
        mBluetoothDevice = mBTDevices.get(position);
        // Then create new instance of BluetoothConnectionService
        mBluetoothConfigure.mBluetoothConnectionService = new BluetoothConnectionService(MainActivity.this);
    }

    public void writeMessage(byte[] bytes) {
        mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
    }

    public void startBTConnection() {
        startConnection();
    }

    @Override
    public void initUI(ListView lv, Button send, Button startCon, Button stopCon,
                       Button directCon, Button discover, ProgressBar loading) {
        this.lvDevices = lv;
        this.btnSend = send;
        this.btnStartConnection = startCon;
        this.btnStopConnection = stopCon;
        this.btnDirectConnect = directCon;
        this.btnDiscoverDevices = discover;
        this.loading = loading;

        // Disable stop connection
        btnStopConnection.setVisibility(View.INVISIBLE);

        // Check also if bluetooth is enabled, and clickability of buttons
        if (mBluetoothAdapter.isEnabled()) {
            showButtons(true);
        } else {
            showButtons(false);
        }

        // Set button status according to connection status
        if (connectionStatus) {
            btnSend.setEnabled(true);
            btnSend.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
        } else {
            btnSend.setEnabled(false);
            btnSend.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
        }
    }

    public void initUIGraphs(Button gpsData, Button IMUData) {
        this.btnGpsData = gpsData;
        this.btnIMUData = IMUData;

        if (connectionStatus) {
            btnGpsData.setEnabled(true);
            btnIMUData.setEnabled(true);
            btnGpsData.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnIMUData.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));

        } else {
            btnGpsData.setEnabled(false);
            btnIMUData.setEnabled(false);
            btnGpsData.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnIMUData.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public void requestSensor(boolean request) {
        if (request) {
            String reqData = "sensorData";
            byte[] bytes = reqData.getBytes();
            if (mBluetoothConfigure == null) {
                mBluetoothConfigure = getBluetoothConfigure();
                Debug.print("TAG", "mBluetoothConfigure = " + mBluetoothConfigure, 2, "console");
                mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
            } else {
                mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
            }
        } else {
            String reqData = "noMonitor";
            byte[] bytes = reqData.getBytes();
            if (mBluetoothConfigure == null) {
                mBluetoothConfigure = getBluetoothConfigure();
                Debug.print("TAG", "mBluetoothConfigure = " + mBluetoothConfigure, 2, "console");
                mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
            } else {
                mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
            }
        }
    }

    @Override
    public void initUISensor(Button btnGetData, Button btnStopMonitor) {
        this.btnGetData = btnGetData;
        this.btnStopMonitor = btnStopMonitor;

        // Set button status according to connection status
        if (connectionStatus) {
            btnGetData.setEnabled(true);
            btnStopMonitor.setEnabled(true);
            btnGetData.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnStopMonitor.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
        } else {
            btnGetData.setEnabled(false);
            btnStopMonitor.setEnabled(false);
            btnGetData.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnStopMonitor.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
        }
    }


    @Override
    public void controlMovement(String control) {
        byte[] bytes = control.getBytes();
        Debug.print("TAG", "Control movement done with " + control, 2, "console");
        if (mBluetoothConfigure == null) {
            mBluetoothConfigure = getBluetoothConfigure();
            Debug.print("TAG", "mBluetoothConfigure = " + mBluetoothConfigure, 2, "console");
            mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
        } else {
            mBluetoothConfigure.mBluetoothConnectionService.write(bytes);
        }

    }

    @Override
    public void initUIMovement(ImageButton btnForward, ImageButton btnBackward, ImageButton btnRight, ImageButton btnLeft) {
        this.btnForward = btnForward;
        this.btnBackward = btnBackward;
        this.btnRight = btnRight;
        this.btnLeft = btnLeft;

        // Set button status according to connection status
        if (connectionStatus) {
            btnBackward.setEnabled(true);
            btnForward.setEnabled(true);
            btnRight.setEnabled(true);
            btnLeft.setEnabled(true);
            btnBackward.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnForward.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnRight.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
            btnLeft.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color, null));
        } else {
            btnBackward.setEnabled(false);
            btnForward.setEnabled(false);
            btnRight.setEnabled(false);
            btnLeft.setEnabled(false);
            btnBackward.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnForward.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnRight.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
            btnLeft.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.button_color_disabled, null));
        }
    }

    @Override
    public void connectionStatus(boolean condition) {

        Debug.print(TAG, "Connectionstatus interface called", 2, "console");
        if (condition) {
            mConnectStatus.setImageResource(R.drawable.on);
            waitingThread.interrupt();
            loading.setVisibility(View.INVISIBLE);
            connectionStatus = true;
            //mBluetoothConfigure.reInitUI();

        } else {
            mConnectStatus.setImageResource(R.drawable.not_on2);
            connectionStatus = false;
            //mBluetoothConfigure.reInitUI();
        }
    }

}

/**
 * This thread simply sleeps and waits if device is found and connected.
 * If it's not, it kindly asks user to try to search again for the device
 * Otherwise this thread is killed when device is connected
 */
class DiscoverSleepThread implements Runnable {

    private Thread thread;
    private Context context;
    private static final String TAG = "myApp";

    /**
     * Initialize new thread, and start it
     * @param context current context while starting the thread
     */
    DiscoverSleepThread(Context context) {
        this.thread = new Thread(this, "sleep");
        thread.start();
        this.context = context;
    }

    /**
     * Thread starts sleeping. If it finishes, error message is printed,
     * if not, then the thread and progressbar animation is dismissed.
     */
    @Override
    public void run() {
        try {
            Debug.print(TAG, "Thread sleeping started", 2, "console");
            Thread.sleep(13000);
        } catch (InterruptedException e) {
            Debug.print(TAG, "Thread interrupted", 2, "console");
            //  Toast.makeText(context, "Connected to device", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }
        if (!Thread.currentThread().isInterrupted()) {
            // Send boolean to mainactivity
            LocalBroadcastManager localBroadcastManagerDisc =
                    LocalBroadcastManager.getInstance(context);
            Intent intentGps = new Intent("discover.status");
            intentGps.putExtra("boolean", true);
            intentGps.setClass(context, MainActivity.class);
            localBroadcastManagerDisc.sendBroadcast(intentGps);
            Debug.print(TAG, "Broadcast sent", 2, "console");
            Thread.currentThread().interrupt();
            return;
        }
    }
}