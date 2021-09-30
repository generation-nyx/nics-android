package edu.mit.ll.nics.android.maps;

import android.location.Location;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.UUID;

import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;

public class EnhancedLocation {

    private UUID mUUID;
    private Location mLocation;
    private LiveDataTrigger mTrigger;

    public EnhancedLocation(UUID uuid, Location location, LiveDataTrigger trigger) {
        mUUID = uuid;
        mLocation = location;
        mTrigger = trigger;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EnhancedLocation)) return false;

        EnhancedLocation location = (EnhancedLocation) o;

        return new EqualsBuilder()
                .append(getUUID(), location.getUUID())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getUUID())
                .toHashCode();
    }

    public UUID getUUID() {
        return mUUID;
    }

    public void setUUID(UUID uuid) {
        mUUID = uuid;
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    public LiveDataTrigger getTrigger() {
        return mTrigger;
    }

    public void setTrigger(LiveDataTrigger trigger) {
        mTrigger = trigger;
    }
}

