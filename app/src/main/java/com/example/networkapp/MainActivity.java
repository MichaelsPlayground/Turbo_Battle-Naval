package com.example.networkapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    ListView discoveredView;
    ListView pairedView;
    BltDeviceAdapter bltDiscoveredAdapter;
    BltDeviceAdapter bltPairedAdapter;
    ArrayList<BluetoothDevice> discoveredDevices;
    ArrayList<BluetoothDevice> pairedDevices;
    private static final BluetoothAdapter bltAdapter = BluetoothAdapter.getDefaultAdapter();

    Button btConnect;
    Button btSend;

    public static BltConnectionService bltConnectionService;
    BluetoothDevice bltDevice;
    private UUID uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize List
        discoveredDevices = new ArrayList<BluetoothDevice>();
        pairedDevices = new ArrayList<BluetoothDevice>();
        discoveredView = (ListView) findViewById(R.id.discoveredView);
        pairedView = (ListView) findViewById(R.id.pairedView);
        bltDiscoveredAdapter = new BltDeviceAdapter(this, discoveredDevices);
        bltPairedAdapter = new BltDeviceAdapter(this, pairedDevices);
        discoveredView.setAdapter(bltDiscoveredAdapter);
        pairedView.setAdapter(bltPairedAdapter);
        discoveredView.setOnItemClickListener(new BltDeviceClickListener(bltAdapter));
        pairedView.setOnItemClickListener(new PairedDeviceClickListener(bltAdapter));

        //Initialize Button
        btConnect = (Button) findViewById(R.id.btConnect);
        btSend = (Button) findViewById(R.id.btSend);
        btConnect.setEnabled(false);
        btSend.setEnabled(false);
        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("About to be connected",bltDevice.getName());
                startBltConnection(bltDevice, uuid);
                btConnect.setEnabled(false);
            }
        });
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bltConnectionService.isConnected){
                    Intent intent = new Intent(MainActivity.this, BattleNavalActivity.class);
                    startActivity(intent);
                }else{
                    Toast toast = Toast.makeText(getApplicationContext(), "Vous n'êtes pas encore connecté", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0,10);
                    toast.show();
                }
            }
        });

        uuid = UUID.fromString(MainActivity.this.getString(R.string.uuid));

        /*Initialize bluetooth*/
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
        if(bltAdapter == null){
            //Bluetooth not supported
            Intent closeIntent = new Intent(Intent.ACTION_MAIN);
            closeIntent.addCategory(Intent.CATEGORY_HOME);
            startActivity(closeIntent);
        }
        if(!bltAdapter.isEnabled()){
            //Bluetooth not enabled
            Intent enableBltIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBltIntent, 1);
        }else{
            listPairedDevice(bltAdapter);
            listDiscoveredDevice(bltAdapter);
        }
    }

    //Once bluetooth is enabled we start listing paired and discovered devices
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(requestCode == 1 && resultCode == -1){
            listPairedDevice(bltAdapter);
            listDiscoveredDevice(bltAdapter);
        }
    }

    public void listPairedDevice(BluetoothAdapter bltAdapter){
        /*Check for paired devices*/
        Set<BluetoothDevice> pairedSetDevices = bltAdapter.getBondedDevices();
        if(pairedSetDevices.size() > 0){
            for(BluetoothDevice bltDevice : pairedSetDevices){
                if(!pairedDevices.contains(bltDevice)) {
                    Log.d("pairedDevice", bltDevice.getName());
                    pairedDevices.add(bltDevice);
                    bltPairedAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public void listDiscoveredDevice(final BluetoothAdapter bltAdapter){
        /*Discover bluetooth*/
        final BroadcastReceiver brcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){//New device discovered
                    BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(newDevice.getName() != null){
                        Log.d("device found", newDevice.getName());
                        if(!discoveredDevices.contains(newDevice)){//Let's add it to our discoveredList
                            discoveredDevices.add(newDevice);
                            bltDiscoveredAdapter.notifyDataSetChanged();
                        }
                    }
                }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){//New device paired
                    Log.d("bond state", "from " + intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, 0) + " to " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                    if(intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == 12){//Let's add it to our pairedList
                        listPairedDevice(bltAdapter);
                    }
                }
            }
        };

        IntentFilter intFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(brcReceiver, intFilter);
        bltAdapter.startDiscovery();
    }

    public void startBltConnection(BluetoothDevice device, UUID uuid){
        bltConnectionService.startClient(device, uuid);
    }

    /*Pairing bluetooth*/
    private class BltDeviceClickListener implements android.widget.AdapterView.OnItemClickListener {
        BluetoothAdapter bltAdapter;

        public BltDeviceClickListener(BluetoothAdapter bltAdapter){
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

    /*Starting connection*/
    private class PairedDeviceClickListener implements AdapterView.OnItemClickListener {
        BluetoothAdapter bltAdapter;

        public PairedDeviceClickListener(BluetoothAdapter bltAdapter){
            this.bltAdapter = bltAdapter;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(!btConnect.isEnabled()){
                bltAdapter.cancelDiscovery();
                bltDevice = pairedDevices.get(position);
                bltConnectionService = new BltConnectionService(getApplicationContext(), bltAdapter);
                btConnect.setEnabled(true);
                btSend.setEnabled(true);
                Toast toast = Toast.makeText(getApplicationContext(), "Vous allez jouer avec " + bltDevice.getName(), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0,10);
                toast.show();
            }
        }
    }
}
