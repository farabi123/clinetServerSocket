package com.androidsrc.client;

import android.widget.TextView;

public class Robot {
    double[] location;
    boolean isMannequinFound = false;
    boolean isSearching = false;
    boolean hasLidar;
    boolean isFieldScanComplete = false;
    double[] destination = new double[2];
    double[] lgps = new double[2];
    Client mClient;
    //Server mServer;

    Robot(String phoneIp, boolean hasLidar, double[] location, TextView response_tv) {
        this.mClient = new Client(phoneIp,8080, response_tv);
        //this.mServer = new Server(MainActivity.this);
        this.hasLidar = hasLidar;
        this.location = location;
    }

    double[] getRobotLocation() {
        return location;
    }

    void setRobotLocation (double[] location) {
        this.location = location;
    }

    void setDestination (double[] destination) {
        this.destination = destination;
        this.isSearching = true;
    }

    double[] getDestination() { return this.destination; }

    void setMannStatus(boolean state) {
        this.isMannequinFound = state;

        if (isMannequinFound) {
            this.isSearching = false;
        }
    }

    boolean isHasLidar() { return this.hasLidar; }

    void setIsFieldScanComplete (boolean state){
        this.isFieldScanComplete = state;
    }

    boolean getFieldScanStatus () { return this.isFieldScanComplete; }

    boolean getMannStatus() {
        return this.isMannequinFound;
    }

    void setObjectLocation (double[] lidarGPS) {
        this.lgps = lidarGPS;
    }

    double[] getObjectLocation () { return lgps; }
}
