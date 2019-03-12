package com.example.networkapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class BltDeviceAdapter extends BaseAdapter {
    List<BluetoothDevice> bltList;
    LayoutInflater inflater;
    Context context;

    public BltDeviceAdapter(Context context, List<BluetoothDevice> bltList){
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.bltList = bltList;
    }

    @Override
    public int getCount() {
        return this.bltList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.bltList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.blt_device, null);
        TextView bltName = (TextView) view.findViewById(R.id.bltName);
        bltName.setText(bltList.get(position).getName());
        return view;
    }
}
