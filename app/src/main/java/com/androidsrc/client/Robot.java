package com.androidsrc.client;

import android.os.AsyncTask;
import android.widget.TextView;

public class Robot extends AsyncTask<Void, Void, Void> {

    public double[] location;
    public boolean isMannequinFound = false;
    boolean isSearching = false;
    public boolean hasLidar;
    public double[] destination = new double[2];
    public double[] objLocation = new double[2];
    public Client mClient;
    //Server mServer;

    Robot(String phoneIp, boolean hasLidar, TextView response, double[] location) {
        this.mClient = new Client(phoneIp,8080, response);
        //this.mServer = new Server(MainActivity.this);
        this.hasLidar = hasLidar;
        this.location = location;
    }

    @Override
    protected Void doInBackground(Void... voids) {


        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
