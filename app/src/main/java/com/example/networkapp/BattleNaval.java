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
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class BattleNaval extends Activity {

    BltConnectionService bcs;

    enum navire{None,T,C,SM,PA}  //liste des navires existants, leur rang dans l'énumération correspond aussi à leur longueur

    char cases[]={'A','B','C','D','E','F','G','H'};  //Lettre pour identifier les cases avec leur tag ou id, aurait put etre simplemet des chiffes

    TableLayout plat1; //Plateau de jeu constitué de 8 TableRow eux même constitués de 8 TextView servant de cases
    Button reset;//Bouton permetant de recommencer à placer les bateaux
    Button validate;//Bouton validant la position des bateaux
    //TextView des différents navires affichant leur longueur et quantité restante à placer
    TextView porteAvion;
    TextView croiseur;
    TextView sousMarin;
    TextView torpilleur;

    navire selected;//permet d'itentifier le type de navire sélectionné par l'utilisateur

    HashMap<String,ArrayList<Case>> navires;//contient les navires positionnés et les cases qu'ils occupent
    HashMap<navire,Integer> nav;//contient le nombre de navire restant à positionner selon leur type
    HashSet<Case> suggestions;//cases des positionnemt possible du bateau en train d'être placé par l'utilisteur
    HashSet<Case> occuped;//cases occupés par un bateau
    Case placing;//case du premier clique de l'utilisateur pour positionner le navire
    int mode;// mode/étape du placement d'un navire




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.placement);

        plat1 = findViewById(R.id.plat1);
        porteAvion = findViewById(R.id.porteAvion);
        croiseur = findViewById(R.id.croiseur);
        sousMarin = findViewById(R.id.sousMarin);
        torpilleur = findViewById(R.id.torpilleur);
        reset = findViewById(R.id.reset);
        validate = findViewById(R.id.validate);

        mode = 0;

        suggestions = new HashSet<>();
        occuped = new HashSet<>();

        navires = new HashMap<>();

        nav = new HashMap<>();
        nav.put(navire.None,-1);
        nav.put(navire.PA, 1);
        nav.put(navire.C,2);
        nav.put(navire.SM,1);
        nav.put(navire.T,3);

        selected = navire.None;

        placing = new Case();

        updateText();

        bcs = MainActivity.bltConnectionService;

        final BroadcastReceiver brcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("REQUEST")){
                String msg = intent.getStringExtra("RequestValue");
                Log.d("Brodcast received",msg);
                if(msg.contains("abandon")){
                    abandonAdverse();
                }
            }
        }
    };

        LocalBroadcastManager.getInstance(this).registerReceiver(brcReceiver, new IntentFilter("REQUEST"));
    }

    //met à jour les indications du nombres de navires à placer
    public void updateText(){
        porteAvion.setText("Porte avion : "+nav.get(navire.PA)+"     --  Longueur 5");
        croiseur.setText("Croiseur : "+nav.get(navire.C)+"     --  Longueur 3");
        sousMarin.setText("Sous-marin : "+nav.get(navire.SM)+"     --  Longueur 4");
        torpilleur.setText("Torpilleur : "+nav.get(navire.T)+"     --  Longueur 2");
    }


    //lance l'activité du mode de combat si tout les bateaux sont placés
    public void validate(View view){
        if(nav.get(navire.C)==0 && nav.get(navire.PA)==0 && nav.get(navire.SM)==0 && nav.get(navire.T)==0) {
            Intent intent = new Intent(BattleNaval.this, BattleNavalFight.class);
            intent.putExtra("navires",navires);
            startActivity(intent);
            finish();
        }else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Vous n'avez pas placé tous les navires")
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    }).show();
            // Create the AlertDialog object
            builder.create();
        }

    }

    //réinitialise l'activité
    public void reset(View view){
        recreate();
    }

    //vérifie si l'utilisateur veut vraiment abandonner la partie
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Voulez vous vraiment abandonner ?")
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        bcs.write("abandon");
                        finish();
                    }
                })
                .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                }).show();
        // Create the AlertDialog object
        builder.create();
    }


    //selection du porte avion pour etre placé et surligne la TextView(appelé lors d'un clique sur la TextView)
    public void spa (View view){
        if(nav.get(navire.PA)>0) {
            if(mode==1){
                clearSugg();
            }
            mode = 0;
            selected = navire.PA;
            resetChoosingColor();
            view.setBackgroundResource(R.color.colorChoosing);
        }
    }
    //selection du croiseur pour etre placé
    public void sc (View view){
        if(nav.get(navire.C)>0) {
            if(mode==1){
                clearSugg();
            }
            mode = 0;
            selected =navire.C;
            resetChoosingColor();
            view.setBackgroundResource(R.color.colorChoosing);
        }
    }
    //selection du sous-marin pour etre placé
    public void ssm (View view){
        if(nav.get(navire.SM)>0) {
            if(mode==1){
                clearSugg();
            }
            mode = 0;
            selected = navire.SM;
            resetChoosingColor();
            view.setBackgroundResource(R.color.colorChoosing);
        }
    }
    //selection du torpilleur pour etre placé
    public void st (View view){
        if(nav.get(navire.T)>0) {
            if(mode==1){
                clearSugg();
            }
            mode = 0;
            selected = navire.T;
            resetChoosingColor();
            view.setBackgroundResource(R.color.colorChoosing);
        }
    }

    //reset la couleur de la TextView des bateaux
    public void resetChoosingColor(){
        findViewById(R.id.porteAvion).setBackgroundResource(R.color.colorWhite);
        findViewById(R.id.sousMarin).setBackgroundResource(R.color.colorWhite);
        findViewById(R.id.torpilleur).setBackgroundResource(R.color.colorWhite);
        findViewById(R.id.croiseur).setBackgroundResource(R.color.colorWhite);
    }

    //appelé lors d'un clique sur l'une des cases
    public void place(View view) {
        if(selected==navire.None);
        else if(mode==0){
            mode++;
            suggest(view.getTag().toString(),view);
        }else{
            mode--;
            placeNav(view.getTag().toString());
        }
    }

    //montre les différentes positions possible du bateau en train d'être placé si celles-ci sont libre
    public void suggest(String tag,View view){
        int dist = selected.ordinal();
        int ch = tag.charAt(5)-'0';
        int le = tag.charAt(4)-'A';
        boolean overAll = false;
        boolean over = false;

        //test des différentes direction pour positionnement
        for (int i = ch; i<=ch+dist;i++){
            if(!ask(new Case(i,le))) over = true;
        }if(over){
            for (int i = ch; i<=ch+dist;i++) {
                unask(new Case(i, le));
            }
        }else overAll=true;
        over = false;


        for (int i = ch; i>=ch-dist;i--){
            if(!ask(new Case(i,le))) over = true;
        }if(over){
            for (int i = ch; i>=ch-dist;i--) {
                unask(new Case(i, le));
            }
        }else overAll=true;
        over = false;


        for (int i = le; i<=le+dist;i++){
            if(!ask(new Case(ch,i))) over = true;
        }if(over){
            for (int i = le; i<=le+dist;i++) {
                unask(new Case(ch, i));
            }
        }else overAll=true;
        over = false;


        for (int i = le; i>=le-dist;i--){
            if(!ask(new Case(ch,i))) over = true;
        }if(over){
            for (int i = le; i>=le-dist;i--) {
                unask(new Case(ch, i));
            }
        }else overAll=true;

        if(overAll){
            placing.setCase(ch,le);
            suggestions.add(placing);
            view.setBackgroundResource(R.color.colorChoosing);
        }else { //si aucune position n'est trouvée retourne au premier mode de positionnement
            mode--;
        }

    }

    //check si une case exist ou est libre, return true et la colore en rouge si c'est le cas
    public boolean ask(Case c){
        if(c.ch<=8 && c.le<8 && c.ch>0 && c.le>=0 && !occuped.contains(c)) {
            suggestions.add(c);
            int id = getResources().getIdentifier("Ally" + cases[c.le] + c.ch, "id", getPackageName());
            findViewById(id).setBackgroundResource(R.color.colorRed);
            return true;
        }
        return false;
    }

    //rétablie la couleur d'une case, appelé si toute la longeur d'un bateau ne passe pas dans une direction donnée lors de la suggestion
    public void unask(Case c){
        if(c.ch<=8 && c.le<8 && c.ch>0 && c.le>=0) {
            suggestions.remove(c);
            if(!occuped.contains(c)) {
                int id = getResources().getIdentifier("Ally" + cases[c.le] + c.ch, "id", getPackageName());
                findViewById(id).setBackgroundResource(R.color.colorSea);
            }
        }
    }

    //appelé lors du second clique de placement de l'utilisateur, si la case cliquée correspond à l'une des suggestion valide la placement, sinon l'annule
    public void placeNav(String tag){
        Case tempC;
        ArrayList<Case> temp = new ArrayList<>();
        Case c = new Case(tag.charAt(5)-'0',tag.charAt(4)-'A');
        int dist = selected.ordinal();
        if(suggestions.contains(c) && !c.equals(placing)) {
            navires.put(selected.toString()+(nav.get(selected)),temp);
            nav.put(selected,nav.get(selected)-1);
            if(nav.get(selected)==0) selected=navire.None;
            if (c.ch < placing.ch) {
                for (int i = placing.ch; i >= placing.ch-dist; i--) {
                    tempC = new Case(i, placing.le);
                    occuped.add(tempC);
                    temp.add(tempC);
                    int id = getResources().getIdentifier("Ally" + cases[placing.le] + i, "id", getPackageName());
                    findViewById(id).setBackgroundResource(R.color.colorChoosing);
                }
            } else if (c.ch > placing.ch) {
                for (int i = placing.ch; i <= placing.ch+dist; i++) {
                    tempC = new Case(i, placing.le);
                    occuped.add(tempC);
                    temp.add(tempC);
                    int id = getResources().getIdentifier("Ally" + cases[placing.le] + i, "id", getPackageName());
                    findViewById(id).setBackgroundResource(R.color.colorChoosing);
                }
            } else if (suggestions.contains(c)) {
                if (c.le < placing.le) {
                    for (int i = placing.le; i >= placing.le-dist; i--) {
                        tempC = new Case(placing.ch,i);
                        occuped.add(tempC);
                        temp.add(tempC);
                        int id = getResources().getIdentifier("Ally" + cases[i] + c.ch, "id", getPackageName());
                        findViewById(id).setBackgroundResource(R.color.colorChoosing);
                    }
                } else if (c.le > placing.le) {
                    for (int i = placing.le; i <= placing.le+dist; i++) {
                        tempC = new Case(placing.ch,i);
                        occuped.add(tempC);
                        temp.add(tempC);
                        int id = getResources().getIdentifier("Ally" + cases[i] + c.ch, "id", getPackageName());
                        findViewById(id).setBackgroundResource(R.color.colorChoosing);
                    }
                }
            }

            updateText();
        }
        clearSugg();
    }

    //reset des suggestion
    public void clearSugg(){
        for(Case c : suggestions) {
            if (!occuped.contains(c)) {
                int id = getResources().getIdentifier("Ally" + cases[c.le] + c.ch, "id", getPackageName());
                findViewById(id).setBackgroundResource(R.color.colorSea);
            }
        }
        suggestions.clear();
    }

    public void abandonAdverse(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Votre adversaire est parti")
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                }).show();
        // Create the AlertDialog object
        builder.create();
    }

}
