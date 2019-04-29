package com.example.networkapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
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
    private BluetoothDevice bltDevice;
    private UUID deviceUUID;
    private ConnectedThread connectedThread;
    private ServThread servThread;
    private boolean mode;

    public boolean isConnected;

    public BltConnectionService(Context context, BluetoothAdapter bltAdapter, BluetoothDevice bltDevice){
        this.context = context;
        this.bltAdapter = bltAdapter;
        this.bltDevice = bltDevice;
        appName = context.getString(R.string.app_name);
        uuid = UUID.fromString(context.getString(R.string.uuid));
        mode = false;
        isConnected = false;
        //Initialize mode
        startMode();
    }

    public void startMode(){
        servThread = null;
        connectedThread = null;

        //Try to connect as client
        BluetoothSocket bltSocket = null;

        try {
            BluetoothSocket temp = bltDevice.createRfcommSocketToServiceRecord(uuid);
            Log.d("Phase startMode","temp client socket created");
            bltSocket = temp;
        } catch (IOException e){
            Log.e("Phase startMode", "failed to create temp client socket", e);
        }

        bltAdapter.cancelDiscovery();

        try {
            bltSocket.connect();
            Log.d("Phase startMode", "connection as client successful");
            connected(bltSocket, bltDevice);
        } catch (IOException e){
            mode = true;
            try {
                bltSocket.close();
            } catch (IOException e1) {
                Log.e("Phase startMode", "close failure", e1);
            }
        }

        //Try to serv
        if(mode){
            servThread = new ServThread();
            servThread.start();
        }
    }

    private class ServThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public ServThread(){
            BluetoothServerSocket temp = null;
            try {
                temp = bltAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, uuid);
                Log.d("Phase startMode", "temp server socket created");
            } catch (IOException e){
                Log.e("Phase startMode", "failed to create temp server socket", e);
            }
            serverSocket = temp;
        }

        @Override
        public void run() {
            BluetoothSocket ovSocket = null;

            if(serverSocket != null){
                try {
                    Log.d("Phase startMode", "Waiting for a connection");
                    ovSocket = serverSocket.accept();
                    Log.d("Phase startMode","Connection as server successful");
                    connected(ovSocket, bltDevice);
                } catch (IOException e1){
                    Log.e("Phase startMode", "Failed to connect as server", e1);
                }
            }
        }

        public void cancel(){

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
                    Intent intent= new Intent("REQUEST");
                    intent.putExtra("RequestValue","suddenEnd");
                    locBroadcastManager.sendBroadcast(intent);
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

    private void connected(BluetoothSocket bltSocket, BluetoothDevice bltDevice){
        connectedThread = new ConnectedThread(bltSocket);
        connectedThread.start();
    }

    public void write(String text){
        connectedThread.write(text);
    }
}
