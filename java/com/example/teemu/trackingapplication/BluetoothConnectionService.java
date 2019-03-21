package com.example.teemu.trackingapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {

    // Static variables
    private static final String appName = "robotTracking";
    private static final UUID MY_UUID =
            UUID.fromString("7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d");

    // For handling bluetooth connection
    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;

    private ConnectedThread connectedThread;

    // Instance for handling thread acception
    private AcceptThread acceptThread;

    Context context;

    public BluetoothConnectionService(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();    // This will launch start method, which starts the whole process of connection
    }

    // This thread listens for incoming connections.
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID);
                Debug.print("myApp", "Accept Thread: setting up server with " + MY_UUID, 2, "console");
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print("myApp", "Accept Thread: IOExeption: " + e.getMessage(), 2, "console");
            }
            bluetoothServerSocket = tmp;
        }


        public void run() {
            Debug.print("myApp", "Accept Thread: run() called" + MY_UUID, 2, "console");

            BluetoothSocket socket = null;
            // This will only go through if it's accepted
            try {
                Debug.print("myApp", "run(): RFCOM server socket started...", 2, "console");
                socket = bluetoothServerSocket.accept();
                Debug.print("myApp", "run(): Connection succesful!", 2, "console");
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print("myApp", "Accept Thread: IOExeption: " + e.getMessage(), 2, "console");
            }

            // If socket is null
            if (socket != null) {
                connected(socket, mDevice);      // To be implemented
            }
        }

        public void cancel() {
            Debug.print("myApp", "AcceptThread cancel: Cancelling thread..", 2, "console");
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


        /** This thread tries to create outgoing connection with device
         *  Connection either fails, or succeeds
         */
        private class ConnectThread extends Thread {
            private BluetoothSocket bluetoothSocket;

            public ConnectThread(BluetoothDevice device, UUID uuid){
                Debug.print("myApp", "ConnectThread: constructor", 2, "console");
                mDevice = device;
                deviceUUID = uuid;
            }

            // This will execute automatically in the background
            public void run(){
                BluetoothSocket tmp = null;
                Debug.print("myApp", "ConnectThread: run: run started", 2, "console");

                // Get bluetooth socket for a connection
                // with the given bluetooth device
                try {
                    Debug.print("myApp", "ConnectThread: run: trying to create RFCOM socket", 2, "console");
                    tmp = mDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
                } catch (IOException e) {
                    e.printStackTrace();
                    Debug.print("myApp", "ConnectThread: run: Could not create RFCOM socket: " + e.getMessage(), 2, "console");
                }
                bluetoothSocket = tmp;

                // Discovery mode is memory consuming, so cancel it after connection is made
                bluetoothAdapter.cancelDiscovery();

                // This will only return succesful connection or exception
                try {
                    bluetoothSocket.connect();
                    Debug.print("myApp", "ConnectThread: run: connection established", 2, "console");
                } catch (IOException e) {
                    e.printStackTrace();
                    Debug.print("myApp", "ConnectThread: run: Could not bluetoothSocket.connect: " + e.getMessage(), 2, "console");
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Debug.print("myApp", "ConnectThread: run: could not close socket: " + e.getMessage(), 2, "console");
                    }
                }

                connected(bluetoothSocket, mDevice);    // To be implemented
            }

            public void cancel(){
                try {
                    Debug.print("myApp", "ConnectThread: cancel: closing the socket", 2, "console");
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Debug.print("myApp", "ConnectThread: cancel: Could not cancel the socket" + e.getMessage(), 2, "console");
                }

            }
        }

    /**
     * Start the chat service.
     * Start AcceptThread to start the listening mode.
     */

    public synchronized void start(){
        Debug.print("myApp", "start: ", 2, "console");
        if(connectThread != null){  // If there is connection, cancel it
            connectThread.cancel();
            connectThread = null;
        }
        if(acceptThread == null){   // If theres no acceptThread instance
            acceptThread = new AcceptThread();
            acceptThread.start();   // Start the AcceptThread itself
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Debug.print("myApp", "startClient: started", 2, "console");

        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }


    private class ConnectedThread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket){
            bluetoothSocket = socket;
            Debug.print("myApp", "ConnectedThread: starting", 2, "console");

            InputStream tmpIn = null;
            OutputStream tmpOut= null;

            try {
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print("myApp", "ConnectedThread: could not get input or outputstream: " + e.getMessage(), 2, "console");
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run(){
            Debug.print("myApp", "RUN method started", 2, "console");
            byte[] buffer = new byte[1024];     // Store buffer for stream
            int bytes;  // Bytes returned from read()

            while(true){    // runs "forever"
                try {
                    bytes = inputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Debug.print("myApp", "ConnectedThread: run: InputStream: " + incomingMessage, 2, "console");
                } catch (IOException e) {
                    e.printStackTrace();
                    Debug.print("myApp", "ConnectedThread: " + e.getMessage(), 2, "console");
                    break;
                }
            }
        }

        // You can call this from main activity and send data to remote device
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Debug.print("myApp", "ConnectedThread: write: Writing to outputstream: " + text, 2, "console");
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print("myApp", "ConnectedThread: could not write to outputstream: " + e.getMessage(), 2, "console");
            }
        }

        public void cancel(){
            try {
                Debug.print("myApp", "ConnectedThread: cancel: Cancelling bluetoothSocket", 2, "console");
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Debug.print("myApp", "ConnectedThread: cancel: Couldn't cancel: " + e.getMessage(), 2, "console");
            }
        }
    }

    // Initialize connection
    private void connected(BluetoothSocket socket, BluetoothDevice device){
        Debug.print("myApp", "connected: Starting", 2, "console");

        connectedThread = new ConnectedThread(socket);
        //connectThread.start();    this didn't work, so let's not thread it

        connectedThread.run();
    }

    public void write(byte[] out){
        Debug.print("myApp", "write: write called", 2, "console");
        connectedThread.write(out);
    }

}
