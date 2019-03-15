package com.example.networkapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

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

    public BltConnectionService(Context context, BluetoothAdapter bltAdapter){
        this.context = context;
        this.bltAdapter = bltAdapter;
        appName = context.getString(R.string.app_name);
        uuid = UUID.fromString(context.getString(R.string.uuid));
        start();
    }

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket serverSocket;

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

        public void run(){
            BluetoothSocket bltSocket = null;

            try{
                Log.d("AcceptThread run", "running");
                bltSocket = serverSocket.accept();
                Log.d("AcceptThread run", "Connection success");
            } catch (IOException e) {
                Log.e("AcceptThread run", "Connection failure", e);
            }

            if(bltSocket != null){
                connected(bltSocket, bltDevice);
            }
        }

        public void cancel(){
            try{
                serverSocket.close();
            } catch (IOException e) {
                Log.d("Close serverSocket", "Failure", e);
            }
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket bltSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            bltDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket temp = null;

            try {
                temp = bltDevice.createRfcommSocketToServiceRecord(deviceUUID);
                Log.d("ConnectThread", "socket creation success");
            } catch (IOException e) {
                Log.e("ConnectThread", "socket creation failure", e);
            }

            bltSocket = temp;

            bltAdapter.cancelDiscovery();

            try {
                bltSocket.connect();
                Log.d("ConnectThread", "connection success");
            } catch (IOException e) {
                try {
                    bltSocket.close();
                } catch (IOException e1) {
                    Log.e("ConnectThread", "close failure", e1);
                }
                Log.e("ConnectThread", "connection failure", e);
            }

            connected(bltSocket, bltDevice);
        }

        public void cancel(){
            try{
                bltSocket.close();
            } catch (IOException e) {
                Log.e("ConnectThread", "close failure", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket bltSocket;
        private final InputStream inptStream;
        private final OutputStream outptStream;

        public ConnectedThread(BluetoothSocket socket){
            bltSocket = socket;
            InputStream inStream = null;
            OutputStream outStream = null;

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
                }catch (IOException e){
                    Log.e("ConnectedThread","reading failure", e);
                    break;
                }
            }
        }

        public void write(){
            String text = "test";
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

    public void write(){
        connectedThread.write();
    }
}
