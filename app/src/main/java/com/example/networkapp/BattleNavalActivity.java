package com.example.networkapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class BattleNavalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plateau_de_jeu);

        Intent intent = getIntent();
        Log.d("ddddd", "av");
        MainActivity.bltConnectionService.write();
        Log.d("ddddd", "ddddfffff");
    }
}
