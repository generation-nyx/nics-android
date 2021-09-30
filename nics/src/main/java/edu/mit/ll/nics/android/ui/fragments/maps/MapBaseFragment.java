package edu.mit.ll.nics.android.ui.fragments.maps;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.maps.LocationSegment;
import edu.mit.ll.nics.android.maps.tags.DistancePolylineTag;
import edu.mit.ll.nics.android.repository.MapRepository;
import edu.mit.ll.nics.android.ui.fragments.AppFragment;
import edu.mit.ll.nics.android.ui.fragments.MapFragment;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.GeoUtils.calculateGeometryMidpoint;
import static edu.mit.ll.nics.android.utils.MapUtils.createInfoMarker;
import static edu.mit.ll.nics.android.utils.Utils.findFragment;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public abstract class MapBaseFragment extends AppFragment implements GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowCloseListener, GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCircleClickListener, GoogleMap.OnMarkerDragListener,
        GoogleMap.OnInfoWindowLongClickListener {

    protected GoogleMap mMap;
    protected MapFragment mMapFragment;
    protected MapViewModel mMapViewModel;

    @Inject
    protected MapRepository mRepository;

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable Bundle savedInstanceState) {
        // Get a reference to this view's lifecycle owner.
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = Navigation.findNavController(requireView());

        // Get the view model for this fragment as well as the shared MapViewModel.
        mMapFragment = (MapFragment) findFragment(mNavHostFragment, MapFragment.class);
        mMapViewModel = new ViewModelProvider(Objects.requireNonNull(mMapFragment)).get(MapViewModel.class);

        initMap();
    }

    protected void initMap() {
        mMap = mMapViewModel.getMap();
        if (mMap != null) {
            mMap.setOnMapClickListener(this);
            mMap.setOnMapLongClickListener(this);
            mMap.setOnInfoWindowClickListener(this);
            mMap.setOnInfoWindowCloseListener(this);
            mMap.setOnInfoWindowLongClickListener(this);
            mMap.setOnPolylineClickListener(this);
            mMap.setOnPolygonClickListener(this);
            mMap.setOnMarkerClickListener(this);
            mMap.setOnCircleClickListener(this);
            mMap.setOnMarkerDragListener(this);
        }
    }

    @Override
    public void onCircleClick(@NonNull @NotNull Circle circle) {
        Timber.tag(DEBUG).d("Disabled circle click during editing feature.");
    }

    @Override
    public void onInfoWindowClick(@NonNull @NotNull Marker marker) {
        Timber.tag(DEBUG).d("Disabled info window click during editing feature.");
    }

    @Override
    public void onInfoWindowClose(@NonNull @NotNull Marker marker) {
        Timber.tag(DEBUG).d("Disabled info window close during editing feature.");
    }

    @Override
    public void onInfoWindowLongClick(@NonNull @NotNull Marker marker) {
        Timber.tag(DEBUG).d("Disabled info window long click during editing feature.");
    }

    @Override
    public void onPolygonClick(@NonNull @NotNull Polygon polygon) {
        Timber.tag(DEBUG).d("Disabled polygon click during editing feature.");
    }

    @Override
    public void onPolylineClick(@NonNull @NotNull Polyline polyline) {
        try {
            if (polyline.getTag() instanceof DistancePolylineTag) {
                List<LatLng> points = polyline.getPoints();
                LatLng midpoint = calculateGeometryMidpoint(points);
                DistancePolylineTag tag = (DistancePolylineTag) polyline.getTag();
                LocationSegment segment = tag.getLocationSegment();

                JsonObject attr = new JsonObject();
                attr.addProperty("icon", R.drawable.planned_fire_line);

                Double distance = mMapViewModel.getDistance().getValue();
                if (distance != null) {
                    attr.addProperty(getString(R.string.markup_distance), distance);
                }

                Double elevation = mMapViewModel.getElevation().getValue();
                if (elevation != null) {
                    attr.addProperty(getString(R.string.markup_elevation), elevation);
                }

                Double course = mMapViewModel.getCourse().getValue();
                if (course != null) {
                    attr.addProperty(getString(R.string.markup_course), course);
                }

                mMapFragment.setInfoMarker(createInfoMarker(midpoint, attr));
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d("Failed to show polyline info window.");
        }
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        return false;
    }

    @Override
    public void onMarkerDragStart(@NonNull @NotNull Marker marker) {

    }

    @Override
    public void onMarkerDrag(@NonNull @NotNull Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(@NonNull @NotNull Marker marker) {

    }
}
