package edu.mit.ll.nics.android.database.entities;

import androidx.room.Embedded;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

public class SymbologyResponse {

    public SymbologyResponse(List<SymbologyGroup> symbologies, List<SymbologyGroup> orgSymbologies) {
        this.symbologies = symbologies;
        this.orgSymbologies = orgSymbologies;
    }

    private List<SymbologyGroup> symbologies;

    private List<SymbologyGroup> orgSymbologies;

    public List<SymbologyGroup> getSymbologies() {
        return symbologies;
    }

    public List<SymbologyGroup> getOrgSymbologies() {
        return orgSymbologies;
    }
}


