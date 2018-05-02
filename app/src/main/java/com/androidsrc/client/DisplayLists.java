package com.androidsrc.client;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

public class DisplayLists extends AsyncTask <Void, Void, String[]> {
    Context context;
    private TextView gps_tv, search_tv, confirm_tv;
    double[][] gpsList, searchList, confirmList;

    private String[] displayResults = new String[3];



    DisplayLists (Context context,
                 TextView gps_tv, TextView search_tv, TextView confirm_tv,
                 double[][] gpsList, double[][] searchList, double[][] confirmList)
    {
        this.context = context;
        this.gps_tv = gps_tv;
        this.search_tv = search_tv;
        this.confirm_tv = confirm_tv;
        this.gpsList = gpsList;
        this.searchList = searchList;
        this.confirmList = confirmList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String[] doInBackground(Void... voids) {
        displayResults[0] = printList(gpsList);
        displayResults[1] = printList(searchList);
        displayResults[2] = printList(confirmList);
        return displayResults;
    }

    @Override
    protected void onPostExecute(String[] strings) {
        gps_tv.setText(strings[0]);
        search_tv.setText(strings[1]);
        confirm_tv.setText(strings[2]);
    }

    private String printList (double[][] list) {
        String str = "";

        for (int i=0; i<list.length; i++) {
            str += "{" + (float) list[i][0] + ", " + (float) list[i][1] + "} \n";
        }

        return str;
    }
}
