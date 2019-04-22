package com.example.teemu.trackingapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 *          This class is mainly for creating bluetooth connection,
 *          and sending and receiving content from the Raspberry Pi.
 *          @author Teemu Sormunen
 */
public class BluetoothConnectionService {

    /**
     * Application name
     */
    private static final String appName = "robotTracking";

    /**
     * UUID of the device
     */
    private static final UUID MY_UUID =
            UUID.fromString("7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d");

    /**
     * Tag for debug logging
     */
    private static final String TAG = "myApp";

    // For handling bluetooth connection
    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;

    private ConnectedThread connectedThread;
    private ConnectionStatusInterface connectionStatusInterface;

    // Instance for handling thread acception
    private AcceptThread acceptThread;

    Context context;


    /**
     * Initialize interface and context
     * @param context context, where object is instantiated
     */
    public BluetoothConnectionService(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Activity activity = (Activity) context;
        connectionStatusInterface = (BluetoothConnectionService.ConnectionStatusInterface) activity;
        start();    // This will launch start method, which starts the whole process of connection
    }

    /**
     * Interface for determining status of connection between
     * Raspberry Pi and phone
     */
    public interface ConnectionStatusInterface {

        /**
         * Checks whether the connection is established
         *
         * @param condition are devices connected or not
         */
        void connectionStatus(boolean condition);
    }


    /**
     * This thread listens for incoming connections
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID);
                Debug.print(TAG, "Accept Thread: setting up server with " + MY_UUID, 2, "console");
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print(TAG, "Accept Thread: IOExeption: " + e.getMessage(), 2, "console");
            }
            bluetoothServerSocket = tmp;
        }

        /**
         * Creates connection between devices
         */
        public void run() {
            Debug.print(TAG, "Accept Thread: run() called" + MY_UUID, 2, "console");

            BluetoothSocket socket = null;
            // This will only go through if it's accepted
            try {
                Debug.print(TAG, "run(): RFCOM server socket started...", 2, "console");
                socket = bluetoothServerSocket.accept();
                Debug.print(TAG, "run(): Connection succesful!", 2, "console");
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print(TAG, "Accept Thread: IOExeption: " + e.getMessage(), 2, "console");
            }

            // If socket is null
            if (socket != null) {
                connected(socket, mDevice);
            }
        }

        /**
         * Cancel bluetooth socket. Created for possible later usage.
         */
        public void cancel() {
            Debug.print(TAG, "AcceptThread cancel: Cancelling thread..", 2, "console");
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This thread tries to create outgoing connection with device
     * Connection either fails, or succeeds
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;

        /**
         * Initialize parameters for the class
         * @param device device on which the connection is made with
         * @param uuid UUID of the device
         */
        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Debug.print(TAG, "ConnectThread: constructor", 2, "console");
            mDevice = device;
            deviceUUID = uuid;
        }

        /**
         * Takes care of RFCOMM socket creation
         */
        public void run() {
            BluetoothSocket tmp = null;
            Debug.print(TAG, "ConnectThread: run: run started", 2, "console");
            // Get bluetooth socket for a connection
            // with the given bluetooth device
            try {
                Debug.print(TAG, "ConnectThread: run: trying to create RFCOM socket", 2, "console");
                tmp = mDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                e.printStackTrace();
                connectionStatusInterface.connectionStatus(false);

                Debug.print(TAG, "ConnectThread: run: Could not create RFCOM socket: " + e.getMessage(), 2, "console");
            }
            bluetoothSocket = tmp;

            // Discovery mode is memory consuming, so cancel it after connection is made
            bluetoothAdapter.cancelDiscovery();

            // This will only return succesful connection or exception
            try {
                bluetoothSocket.connect();
                Debug.print(TAG, "ConnectThread: run: connection established", 2, "console");
            } catch (IOException e) {
                e.printStackTrace();
                connectionStatusInterface.connectionStatus(false);

                Debug.print(TAG, "ConnectThread: run: Could not bluetoothSocket.connect: " + e.getMessage(), 2, "console");
                try {
                    bluetoothSocket.close();

                } catch (IOException e1) {
                    e1.printStackTrace();
                    Debug.print(TAG, "ConnectThread: run: could not close socket: " + e.getMessage(), 2, "console");
                }
            }

            connected(bluetoothSocket, mDevice);
        }

        /**
         * Cancel method for manually canceling the connection.
         */
        public void cancel() {
            try {
                Debug.print(TAG, "ConnectThread: cancel: closing the socket", 2, "console");
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print(TAG, "ConnectThread: cancel: Could not cancel the socket" + e.getMessage(), 2, "console");
            }

        }
    }

    /**
     * Start the communication service.
     * Start AcceptThread to start the listening mode.
     */
    public synchronized void start() {
        Debug.print(TAG, "start: ", 2, "console");
        if (connectThread != null) {  // If there is connection, cancel it
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread == null) {   // If theres no acceptThread instance
            acceptThread = new AcceptThread();
            acceptThread.start();   // Start the AcceptThread itself
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Debug.print(TAG, "startClient: started", 2, "console");

        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    /**
     * This class is responsible for handling bluetooth communication and pairing
     */
    private class ConnectedThread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        /**
         * Initializes bluetooth socket that is used.
         * Creates incoming and outgoing connections with the socket.
         * @param socket socket for managing the connection
         */
        public ConnectedThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            Debug.print(TAG, "ConnectedThread: starting", 2, "console");

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                connectionStatusInterface.connectionStatus(false);

                Debug.print(TAG, "ConnectedThread: could not get input or outputstream: " + e.getMessage(), 2, "console");
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        /**
         * Listens for incoming messages
         */
        public void run() {
            Debug.print(TAG, "RUN method started", 2, "console");
            byte[] buffer = new byte[1024];     // Store buffer for stream
            int bytes;  // Bytes returned from read()
            int index = 0;
            connectionStatusInterface.connectionStatus(true);
            List<String> holdMessage = new ArrayList<>();
            //(incomingMessage = new BufferedReader(new InputStreamReader(inputStream)).readLine()) != null
            String incomingMessage;

            // Read all available lines
            while (true) {
                try {
                    Debug.print(TAG, "available: " + inputStream.available(), 2, "console");
                    if ((bytes = inputStream.read(buffer)) != -1) {
                        incomingMessage = new String(buffer, 0, bytes);
                        // bytes = inputStream.read(buffer);
                        // String incomingMessage = new String(buffer, 0, bytes);
                        index += 1;
                        holdMessage.add(incomingMessage);
                        TimeUnit.SECONDS.sleep(1);
                        if (inputStream.available() == 0) {
                            if (!holdMessage.isEmpty()) {
                                // Send data to correct fragment based on identifier
                                String identifier = holdMessage.get(0).split("\n", 2)[0];
                                String restOfString = holdMessage.get(0).split("\n", 2)[1];
                                Debug.print(TAG, "Identifier: " + identifier, 2, "console");
                                switch (identifier) {
                                    case "gps":
                                        // Send the data to graphs
                                        Debug.print(TAG, "Removing: " + identifier, 2, "console");
                                        holdMessage.set(0, restOfString);

                                        LocalBroadcastManager localBroadcastManagerGps =
                                                LocalBroadcastManager.getInstance(context);
                                        Intent intentGps = new Intent("graph.data");
                                        intentGps.putStringArrayListExtra("data1", (ArrayList<String>) holdMessage);
                                        intentGps.setClass(context, Graphs.class);
                                        TimeUnit.SECONDS.sleep(1);
                                        localBroadcastManagerGps.sendBroadcast(intentGps);
                                        TimeUnit.SECONDS.sleep(1);
                                        break;
                                    // Send data to SensorStatistics
                                    case "realtime":
                                        // Send the data to graphs
                                        // First parse 'identifier' away
                                        Debug.print(TAG, "parsed string; " + holdMessage.get(0), 2, "console");
                                        LocalBroadcastManager localBroadcastManager =
                                                LocalBroadcastManager.getInstance(context);
                                        Intent intentRealTime = new Intent("realtime.data");
                                        intentRealTime.putExtra("data", restOfString);
                                        intentRealTime.setClass(context, SensorStatistics.class);
                                        localBroadcastManager.sendBroadcast(intentRealTime);

                                }
                                holdMessage.clear();
                            }
                        }
                    }
                } catch (IOException e) {
                    Debug.print(TAG, "ConnectedThread: " + e.getMessage(), 2, "console");
                    connectionStatusInterface.connectionStatus(false);
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }


        /**
         * This method can be call outside of it's own class
         * Sends bytes to the receiver
         *
         * @param bytes Message is converted to bytes
         */
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Debug.print(TAG, "ConnectedThread: write: Writing to outputstream: " + text, 2, "console");
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print(TAG, "ConnectedThread: could not write to outputstream: " + e.getMessage(), 2, "console");
            }
        }

        public void cancel() {
            try {
                Debug.print(TAG, "ConnectedThread: cancel: Cancelling bluetoothSocket", 2, "console");
                bluetoothSocket.close();
                connectionStatusInterface.connectionStatus(false);

            } catch (IOException e) {
                e.printStackTrace();
                Debug.print(TAG, "ConnectedThread: cancel: Couldn't cancel: " + e.getMessage(), 2, "console");
            }
        }

    }

    /**
     * This method creates new thread and initiates connection with the thread
     *
     * @param socket socket used for connection
     * @param device (optional) used for identifying the device, good for
     *               debugging purposes
     */
    private void connected(BluetoothSocket socket, BluetoothDevice device) {
        Debug.print(TAG, "connected: Starting", 2, "console");

        connectedThread = new ConnectedThread(socket);
        //connectThread.start();    this didn't work, so let's not thread it

        connectedThread.run();
    }

    /**
     * Calls write method from connected thread to send outgoing message
     * @param out message that is sent out in bytes
     */
    public void write(byte[] out) {
        Debug.print(TAG, "write: write called", 2, "console");
        connectedThread.write(out);
    }

    /**
     * This method can be also used for converting the incoming stream to right format.
     * Not used in this case.
     *
     * @param is input string
     * @return
     */
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
