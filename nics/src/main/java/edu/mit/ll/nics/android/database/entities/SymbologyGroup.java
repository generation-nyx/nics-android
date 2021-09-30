package edu.mit.ll.nics.android.database.entities;

import static edu.mit.ll.nics.android.utils.constants.Database.SYMBOLOGY_TABLE;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import edu.mit.ll.nics.android.utils.gson.NestedJSONTypeAdapter;

@Entity(tableName = SYMBOLOGY_TABLE, indices = {@Index(value = {"name"}, unique = true)})
public class SymbologyGroup {

    @PrimaryKey
    private long symbologyid;
    private long created;
    private String owner;
    private String name;
    private String description;

    @Embedded
    @JsonAdapter(NestedJSONTypeAdapter.class)
    private Listing listing;

    public SymbologyGroup(long symbologyid, long created, String owner, String name, String description, Listing listing) {
        this.symbologyid = symbologyid;
        this.created = created;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.listing = listing;
    }

    public long getSymbologyid() {
        return symbologyid;
    }

    public long getCreated() {
        return created;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Listing getListing() {
        return listing;
    }

    public static class Listing {

        private String parentPath;

        private List<Symbology> listing;

        public Listing(String parentPath, List<Symbology> listing) {
            this.parentPath = parentPath;
            this.listing = listing;
        }

        public String getParentPath() {
            return parentPath;
        }

        public List<Symbology> getListing() {
            return listing;
        }
    }

    public static class Symbology {

        @SerializedName("desc")
        private String description;

        private String filename;

        public Symbology(String description, String filename) {
            this.description = description;
            this.filename = filename;
        }

        public String getDescription() {
            return description;
        }

        public String getFilename() {
            return filename;
        }
    }
}


