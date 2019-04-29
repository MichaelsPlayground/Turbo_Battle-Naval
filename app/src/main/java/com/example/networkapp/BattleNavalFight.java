package com.example.networkapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class BattleNavalFight extends Activity {

    BltConnectionService bcs;

    char cases[]={'A','B','C','D','E','F','G','H'};

    TextView info;//affiche l'état du jeu

    //plateau adverse et allié
    TableLayout plat1;
    TableLayout plat2;

    HashSet<Case> attacked; //contient les positions déjà attaqué

    HashMap<String, ArrayList<Case>> navires;//contient les bateaux placés lors de la phase précédente

    boolean yourTurn;//vrai si c'est au tour de l'utilisateur faux sinon

    boolean restart;//vrai si l'utilisateur recommence une partie

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plateau_de_jeu);
        Intent intent = getIntent();
        info = findViewById(R.id.info);
        plat1 = findViewById(R.id.plat1);
        plat2 = findViewById(R.id.plat2);
        navires = new HashMap<>((HashMap<String, ArrayList<Case>>) intent.getSerializableExtra("navires"));//récupération du positionnement des navires adverses
        attacked=new HashSet<>();

        restart=false;

        loadPlateau(navires);//rempli le plateau allié avec les bateaux positionnés

        bcs = StartGameBltActivity.bltConnectionService;//permet la communication avec l'adversaire

        //traite les messages reçu
        final BroadcastReceiver brcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals("REQUEST")){
                   String msg = intent.getStringExtra("RequestValue");
                   Log.d("Brodcast received",msg);
                   if(msg.contains("attack")){
                       Log.d("launch attacked", msg.substring(6,8));
                       attacked(msg.substring(6,8));
                   }else if(msg.contains("abandon")){
                       abandonAdverse();
                   }else if(msg.contains("touch")){
                       touched(msg.substring(5,7));
                   }else if(msg.contains("lost")){
                       win();
                   }else if(msg.contains("miss")){
                       info.setText(R.string.miss);
                   }else if(msg.contains("sinked")){
                        sinked(msg.substring(6,8));
                   }else if(msg.equals("start")){
                       yourTurn=true;
                       bcs.write("atoi");
                       info.setText(R.string.turn);
                   }else if (msg.contains("atoi")){
                       yourTurn=false;
                   }else if(msg.equals("restart")){
                       restart();
                   }else if(msg.equals("suddenEnd")){
                       Toast.makeText(getApplicationContext(), R.string.suddenEnd, Toast.LENGTH_LONG).show();
                       Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
                       startActivity(intent2);
                   }
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(brcReceiver, new IntentFilter("REQUEST"));

        bcs.write("start");
    }



    public void loadPlateau(HashMap<String,ArrayList<Case>> nav){
        for(ArrayList<Case> al : nav.values()){
            for (Case c : al){
                int id = getResources().getIdentifier("Ally" + cases[c.le] + c.ch, "id", getPackageName());
                findViewById(id).setBackgroundResource(R.color.colorChoosing);
            }

        }
    }


    //vérifie si l'adversaire veut abandonner
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.abandon)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        bcs.write("abandon");
                        finish();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                }).show();
        // Create the AlertDialog object
        builder.create();
    }

    //lance une attaque sur une case du plateu adverse
    public void attack(View view) {
        if(view.getParent().getParent().equals(plat2));
        else if(yourTurn){
            int ch = view.getTag().toString().charAt(5)-'0';
            int le = view.getTag().toString().charAt(4)-'A';
            Case c = new Case(ch,le);
            if(!attacked.contains(c)){
                bcs.write("attack"+view.getTag().toString().substring(4,6));
                view.setBackgroundResource(R.color.justDark);
                attacked.add(c);
                yourTurn=false;
            }
        }
    }

    //attaque de l'adversair reçu, vérifie si un bateau à été touché voire coulé
    public void attacked(String place){
        Log.d("attacked",place);

        boolean touched =false;
        int ch = place.charAt(1)-'0';
        int le = place.charAt(0)-'A';
        int id = getResources().getIdentifier("Ally"+place,"id", getPackageName());
        Case attacked =new Case(ch,le);
        for(ArrayList<Case> al : navires.values()){
            for (Case c : al) {
                if (c.equals(attacked)) {
                    touched = true;
                }
            }
           if(touched){
               findViewById(id).setBackgroundResource(R.color.colorRed);
               al.remove(attacked);
               if(al.isEmpty()){
                   sink();
                   bcs.write("sinked"+place);
               }
               else bcs.write("touch"+place);
               break;
           }
        }
        if(!touched){
            bcs.write("miss");
            findViewById(id).setBackgroundResource(R.color.colorDarkerSea);
        }
        info.setText(R.string.turn);
        yourTurn=true;
    }

    //si un bateau est coulé vérifie s'il en reste
    public void sink(){
        boolean lost = true;
        for(ArrayList<Case> al : navires.values()) {
            if(!al.isEmpty()){
                lost = false;
            }
        }
        if (lost){
            bcs.write("lost");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.revenge)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            bcs.write("restart");
                            restart=true;
                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    }).show();
            // Create the AlertDialog object
            builder.create();

        }
    }

    //abandon de l'adversaire
    public void abandonAdverse(){
        info.setText(R.string.opponentLeft);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.opponentLeft)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                }).show();
        // Create the AlertDialog object
        builder.create();
    }

    //bateau adverse touché
    public void touched(String place){
        int id = getResources().getIdentifier("Enem"+place,"id", getPackageName());
        findViewById(id).setBackgroundResource(R.color.colorRed);
        info.setText(R.string.hit);
    }

    //bateau adverse coulé
    public void sinked(String place) {
        Log.d("noyé",place);
        int id = getResources().getIdentifier("Enem"+place,"id", getPackageName());
        findViewById(id).setBackgroundResource(R.color.colorRed);
        info.setText(R.string.hitnsunk);
    }

    //partie gagné
    public void win(){
        info.setText(R.string.congrat);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.win)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        bcs.write("restart");
                        restart=true;
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        }).show();
        // Create the AlertDialog object
        builder.create();
    }

    public void restart(){
        if(restart){
            restart=false;
            bcs.write("restart");
            Log.d("Restarting","");
            Intent intent = new Intent(BattleNavalFight.this, BattleNaval.class);
            startActivity(intent);
            finish();
        }
    }


}
