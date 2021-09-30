package edu.mit.ll.nics.android.data.messages;

import com.google.gson.Gson;

import java.util.ArrayList;

import edu.mit.ll.nics.android.data.OpenElevation;

public class OpenElevationMessage {

    private ArrayList<OpenElevation> results;

    public ArrayList<OpenElevation> getResults() {
        return results;
    }

    public void setResults(ArrayList<OpenElevation> results) {
        this.results = results;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
