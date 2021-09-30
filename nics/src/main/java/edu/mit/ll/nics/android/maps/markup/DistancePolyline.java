package edu.mit.ll.nics.android.maps.markup;

import android.app.Activity;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.RoundCap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.mit.ll.nics.android.maps.LocationSegment;
import edu.mit.ll.nics.android.maps.tags.DistancePolylineTag;
import edu.mit.ll.nics.android.repository.PreferencesRepository;

public class DistancePolyline extends MarkupSegment {

    private final LocationSegment mLocationSegment;
    private final LatLng mStartCoordinates;
    private final LatLng mEndCoordinates;

    public DistancePolyline(LocationSegment segment,
                            GoogleMap map,
                            PreferencesRepository preferences,
                            Activity activity) {
        super(map, preferences, activity);

        mLocationSegment = segment;

        mStartCoordinates = new LatLng(mLocationSegment.getStartLocation().getLatitude(), mLocationSegment.getStartLocation().getLongitude());
        mEndCoordinates = new LatLng(mLocationSegment.getEndLocation().getLatitude(), mLocationSegment.getEndLocation().getLongitude());
        setPoints(new ArrayList<>(Arrays.asList(mStartCoordinates, mEndCoordinates)));

        List<PatternItem> pattern = Arrays.asList(new Gap(20), new Dash(20));
        setPattern(pattern);
        setStartCap(new RoundCap());
        setEndCap(new RoundCap());
        super.setTag(new DistancePolylineTag(mLocationSegment));
    }

    public LocationSegment getLocationSegment() {
        return mLocationSegment;
    }

    public Location getStartLocation() {
        return mLocationSegment.getStartLocation();
    }

    public Location getEndLocation() {
        return mLocationSegment.getEndLocation();
    }
}
