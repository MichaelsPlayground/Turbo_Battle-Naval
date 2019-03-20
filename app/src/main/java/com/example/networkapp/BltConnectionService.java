package com.example.networkapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

public class BltConnectionService {
    private final UUID uuid;
    private final String appName;
    private final BluetoothAdapter bltAdapter;

    Context context;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private BluetoothDevice bltDevice;
    private UUID deviceUUID;
    private ConnectedThread connectedThread;

    public boolean isConnected;

    public BltConnectionService(Context context, BluetoothAdapter bltAdapter){
        this.context = context;
        this.bltAdapter = bltAdapter;
        appName = context.getString(R.string.app_name);
        uuid = UUID.fromString(context.getString(R.string.uuid));
        isConnected = false;
        //Initialize server
        start();
    }

    //Server initialization
    private class AcceptThread extends Thread{
        private final BluetoothServerSocket serverSocket;

        //Create a server socket
        public AcceptThread(){
            BluetoothServerSocket temp = null;

            try{
                temp = bltAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, uuid);
                Log.d("Set up","success");
            } catch (IOException e) {
                Log.e("Set up","failure", e);
            }

            serverSocket = temp;
        }

        //Wait until the server socket is used
        public void run(){
            BluetoothSocket bltSocket = null;

            try{
                Log.d("AcceptThread run", "running");
                bltSocket = serverSocket.accept();
                Toast toast = Toast.makeText(context, "Vous êtes connectés !", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0,10);
                toast.show();
                Log.d("AcceptThread run", "Connection success");
            } catch (IOException e) {
                Log.e("AcceptThread run", "Connection failure", e);
            }

            if(bltSocket != null){
                connected(bltSocket, bltDevice);
            }

            //Then delete it chen it becomes useless
            cancel();
        }

        public void cancel(){
            try{
                serverSocket.close();
            } catch (IOException e) {
                Log.d("Close serverSocket", "Failure", e);
            }
        }
    }

    //Client initialization
    private class ConnectThread extends Thread{
        private BluetoothSocket bltSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            bltDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket temp = null;

            //Create a client socket
            try {
                temp = bltDevice.createRfcommSocketToServiceRecord(deviceUUID);
                Log.d("ConnectThread", "socket creation success");
            } catch (IOException e) {
                Log.e("ConnectThread", "socket creation failure", e);
            }

            bltSocket = temp;

            bltAdapter.cancelDiscovery();

            //Attempt a connection with the distant server socket
            try {
                bltSocket.connect();
                Log.d("ConnectThread", "connection success");
                connected(bltSocket, bltDevice);
            } catch (IOException e) {
                try {
                    bltSocket.close();
                } catch (IOException e1) {
                    Log.e("ConnectThread", "close failure", e1);
                }
                Log.e("ConnectThread", "connection failure", e);
            }
        }

        public void cancel(){
            try{
                bltSocket.close();
            } catch (IOException e) {
                Log.e("ConnectThread", "close failure", e);
            }
        }
    }

    //Manage connection
    private class ConnectedThread extends Thread {
        private final BluetoothSocket bltSocket;
        private final InputStream inptStream;
        private final OutputStream outptStream;
        private final LocalBroadcastManager locBroadcastManager = LocalBroadcastManager.getInstance(context);

        public ConnectedThread(BluetoothSocket socket){
            isConnected = true;

            bltSocket = socket;
            InputStream inStream = null;
            OutputStream outStream = null;

            //Initialize connection management
            try {
                inStream = bltSocket.getInputStream();
                outStream = bltSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inptStream = inStream;
            outptStream = outStream;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while (true){
                try{
                    bytes = inptStream.read(buffer);
                    Log.d("Message received", new String(buffer, 0, bytes));
                    Intent intent= new Intent("REQUEST");
                    intent.putExtra("RequestValue",new String(buffer, 0, bytes));
                    locBroadcastManager.sendBroadcast(intent);
                }catch (IOException e){
                    Log.e("ConnectedThread","reading failure", e);
                    break;
                }
            }
        }

        //Send request to the distant server
        public void write(String text){
            try {
                outptStream.write(text.getBytes());
            } catch (IOException e) {
                Log.e("ConnectedThread", "writing failure", e);
            }
        }

        public void cancel(){
            try{
                bltSocket.close();
            }catch (IOException e){
                Log.e("ConnectedThread","close socket failure", e);
            }
        }
    }

    public synchronized void start(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
        if(acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    private void connected(BluetoothSocket bltSocket, BluetoothDevice bltDevice){
        connectedThread = new ConnectedThread(bltSocket);
        connectedThread.start();
    }

    public void write(String text){
        connectedThread.write(text);
    }
}
