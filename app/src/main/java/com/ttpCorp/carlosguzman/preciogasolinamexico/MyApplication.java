package com.ttpCorp.carlosguzman.preciogasolinamexico;

import android.app.Application;

/**
 * solamente es usada para acceder a los IDs de los objectos en la base de datos Web.
 */
public class MyApplication extends Application {


    private String regionesList[] =  new String [1000];
    private String regionesIDs[] =  new String[1000];


    public String getRegionesList(int i) {
        return regionesList[i];
    }

    public void setRegionesList(String someVariable, int i) {
        this.regionesList[i] = someVariable;
    }
    public String getRegionesID(int i) {
        return regionesIDs[i];
    }

    public void setRegionesIDs(String someVariable, int i) {
        this.regionesIDs[i] = someVariable;
    }
}
