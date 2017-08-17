package com.ombapit.alarmbutton.utils;

import android.app.Activity;
import android.app.Application;

/**
 * Created by 247 on 10/8/2016.
 */

public class GlobalVar {


    private static GlobalVar instance = new GlobalVar();

    // Getter-Setters
    public static GlobalVar getInstance() {
        return instance;
    }

    public static void setInstance(GlobalVar instance) {
        GlobalVar.instance = instance;
    }

    private String smsc= "";


    private GlobalVar() {

    }


    public String getSMSCenter() {
        return smsc;
    }


    public void setSMSCenter(String smsc) {
        this.smsc = smsc;
    }

}