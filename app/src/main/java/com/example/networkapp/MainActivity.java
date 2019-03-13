package com.example.networkapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    ListView deviceView;
    BltDeviceAdapter bltDeviceAdapter;
    ArrayList<BluetoothDevice> discoveredDevices;
    static final BluetoothAdapter bltAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        discoveredDevices = new ArrayList<BluetoothDevice>();
        deviceView = (ListView) findViewById(R.id.deviceView);
        bltDeviceAdapter = new BltDeviceAdapter(this, discoveredDevices);
        deviceView.setAdapter(bltDeviceAdapter);
        deviceView.setOnItemClickListener(new bltDeviceClickListener(bltAdapter));

        /*Initialize bluetooth*/
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
        if(bltAdapter == null){
            //Le bluetooth n'est pas supporté
            Intent closeIntent = new Intent(Intent.ACTION_MAIN);
            closeIntent.addCategory(Intent.CATEGORY_HOME);
            startActivity(closeIntent);
        }
        if(!bltAdapter.isEnabled()){
            //Le bluetooth n'est pas allumé
            Intent enableBltIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBltIntent, 1);
        }else{
            listBltDevice(bltAdapter);
        }
    }

    public void listBltDevice(BluetoothAdapter bltAdapter){
        /*Check for paired devices*/
        Set<BluetoothDevice> pairedDevices = bltAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice bltDevice : pairedDevices){
                Log.d("pairedDevice", bltDevice.getName());
            }
        }

        /*Discover bluetooth*/
        BroadcastReceiver brcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){//La recherche est fructueuse
                    BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(newDevice.getName() != null){
                        Log.d("device found", newDevice.getName());
                        discoveredDevices.add(newDevice);
                        bltDeviceAdapter.notifyDataSetChanged();
                    }
                }
            }
        };

        IntentFilter intFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(brcReceiver, intFilter);
        bltAdapter.startDiscovery();
        //TODO unregister
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(requestCode == 1 && resultCode == -1){
            listBltDevice(bltAdapter);
        }
    }

    /*Pairing bluetooth*/
    private class bltDeviceClickListener implements android.widget.AdapterView.OnItemClickListener {
        BluetoothAdapter bltAdapter;

        public bltDeviceClickListener(BluetoothAdapter bltAdapter){
            this.bltAdapter = bltAdapter;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            bltAdapter.cancelDiscovery();
            Log.d("Item clicked", discoveredDevices.get(position).getName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                discoveredDevices.get(position).createBond();
            }
        }
    }
}
