package com.example.networkapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class BattleNavalActivity extends AppCompatActivity {
    BltConnectionService bcs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plateau_de_jeu);

        bcs = MainActivity.bltConnectionService;

        final BroadcastReceiver brcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals("REQUEST")){
                    Log.d("Request",intent.getStringExtra("RequestValue"));
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter("REQUEST");
        registerReceiver(brcReceiver, intentFilter);
    }

    public void attack(View view){
        bcs.write("yo la mif");
    }
}
