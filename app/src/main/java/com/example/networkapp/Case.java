package com.example.networkapp;


import java.io.Serializable;

public class Case implements Serializable {
    public int le;
    public int ch;



    Case(int c,int l){
        le=l;
        ch=c;
    }

    Case(){
        le=-1;
        ch=-1;
    }

    public void setCase(int c,int l){
        le=l;
        ch=c;
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == Case.class){
            final Case c = (Case) obj;
            return (this.ch==c.ch && this.le==c.le);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return (10*ch+le);
    }


}
