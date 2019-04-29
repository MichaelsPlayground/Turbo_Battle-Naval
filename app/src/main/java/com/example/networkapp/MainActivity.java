package com.example.networkapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button bltButton;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        bltButton = (Button) findViewById(R.id.bluetoothButton);
        bltButton.setOnClickListener(handlerBlt);
    }

    View.OnClickListener handlerBlt = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), StartGameBltActivity.class);
            startActivity(intent);
        }
    };
}
