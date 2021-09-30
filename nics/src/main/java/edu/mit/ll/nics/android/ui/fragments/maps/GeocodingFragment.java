/*
 * Copyright (c) 2008-2021, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.android.ui.fragments.maps;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentGeocodingBinding;
import edu.mit.ll.nics.android.maps.EnhancedLatLng;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.ui.adapters.PagerAdapter;
import edu.mit.ll.nics.android.ui.fragments.AppFragment;
import edu.mit.ll.nics.android.ui.fragments.MapFragment;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.GeocodingViewModel;
import edu.mit.ll.nics.android.utils.ExtensionsKt;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;

import static edu.mit.ll.nics.android.utils.Utils.findFragment;
import static edu.mit.ll.nics.android.utils.Utils.forceHideKeyboard;
import static edu.mit.ll.nics.android.utils.Utils.popBackStack;
import static edu.mit.ll.nics.android.utils.constants.Events.GEOCODE_ZOOM;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_LOCATION_REQUEST;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class GeocodingFragment extends AppFragment implements GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowCloseListener, GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnCircleClickListener,
        GoogleMap.OnMarkerDragListener {

    private GoogleMap mMap;
    private MarkupSymbol mMarker;
    private MapViewModel mMapViewModel;
    private GeocodingViewModel mViewModel;
    private FragmentGeocodingBinding mBinding;
    private ViewPager2 mViewPager;
    private boolean mEventsRegistered = false;

    @Inject
    GeocodingViewModel.GeocodingViewModelFactory mViewModelFactory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register on back pressed callback.
        mActivity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_geocoding.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_geocoding, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link GeocodingViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Get teh view's lifecycle owner after the view has been created.
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = Navigation.findNavController(requireView());

        // Get the view model for this fragment as well as the shared MapViewModel.
        Fragment mapFragment = findFragment(mNavHostFragment, MapFragment.class);
        mMapViewModel = new ViewModelProvider(Objects.requireNonNull(mapFragment)).get(MapViewModel.class);

        initMap();
        mMarker = initMarker();

        GeocodingViewModel.Factory factory = new GeocodingViewModel.Factory(mViewModelFactory, mMarker);
        mViewModel = new ViewModelProvider(this, factory).get(GeocodingViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setFragment(this);

        mViewPager = mBinding.pager;

        List<GeocodingPageFragment> fragments = Arrays.asList(
                GeocodingDegMinSecFragment.newInstance(),
                GeocodingDegMinFragment.newInstance(),
                GeocodingLatLngFragment.newInstance(),
                GeocodingMgrsFragment.newInstance(),
                GeocodingCrsFragment.newInstance(),
                GeocodingAddressFragment.newInstance()
        );

        mViewPager.setAdapter(new PagerAdapter(this, fragments));
        mViewPager.setOffscreenPageLimit(6);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                fragments.get(position).refresh();
                forceHideKeyboard(mActivity);
                super.onPageSelected(position);
            }
        });

        TabLayout tabLayout = mBinding.tabs;
        new TabLayoutMediator(tabLayout, mViewPager, (tab, position) -> {
            tab.setText(fragments.get(position).getTabTitle(mContext));
            tab.setContentDescription(fragments.get(position).getTabContentDescription(mContext));
        }).attach();

        subscribeToModel();
    }

    private void subscribeToModel() {
        mViewModel.getPoint().observe(mLifecycleOwner, point -> {
            if (point != null && point.getLatLng() != null) {
                mMarker.addToMap(point.getLatLng());
            } else {
                mMarker.removeFromMap();
            }
        });
    }

    private void initMap() {
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

    private MarkupSymbol initMarker() {
        MarkupSymbol symbol = new MarkupSymbol(mMap, mPreferences, mActivity, true, true);
        try {
            LatLng point = GeocodingFragmentArgs.fromBundle(getArguments()).getSelectionPoint();
            symbol.setPoint(Objects.requireNonNull(point));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 13));
        } catch (Exception ignored) {
        }
        return symbol;
    }

    /**
     * Unbind from all xml layouts and cancel any pending dialogs.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mEventsRegistered) {
            LiveDataBus.subscribe(GEOCODE_ZOOM, this, data -> zoom());
            mEventsRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        // Clear the shared view model's values since it is using the activity's context.
        if (mMarker != null) {
            mMarker.removeFromMap();
        }
        mViewModel.reset();

        if(mEventsRegistered) {
            LiveDataBus.unregister(GEOCODE_ZOOM);
            mEventsRegistered = false;
        }
        onBackPressedCallback.remove();
        super.onDestroy();
    }

    /**
     * Override the onBackPressedCallback so that the user can go back to whatever edit panel they were working on.
     */
    final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            if (mViewPager.getCurrentItem() == 0) {
                // If the user is currently looking at the first step, allow the system to handle the
                // Back button. This calls finish() on this activity and pops the back stack.
                cancel();
            } else {
                // Otherwise, select the previous step.
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        }
    };

    public void cancel() {
        if (mMapViewModel.isSelectingLocation().getValue()) {
            popBackStack(mNavHostFragment);
        } else {
            popBackStack(mNavController);
        }
    }

    public void finish() {
        if (mMapViewModel.isSelectingLocation().getValue()) {
            if (mMarker != null) {
                LatLng position = mMarker.getPoint();
                ExtensionsKt.setNavigationResult(mNavHostFragment, PICK_LOCATION_REQUEST, position);
            }
        }
        cancel();
    }

    public void submit() {
        finish();
    }

    public void zoom() {
        EnhancedLatLng point = mViewModel.getPoint().getValue();
        if (point != null && point.getLatLng() != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point.getLatLng(), 13));
        } else {
            Snackbar.make(requireView(), "No marker to zoom to.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setPoint(LatLng point) {
        if (mViewModel.getPoint().getValue() == null) {
            mViewModel.setPoint(new EnhancedLatLng(UUID.randomUUID(), point, MAP));
        } else {
            EnhancedLatLng p = mViewModel.getPoint().getValue();
            p.setLatLng(point);
            p.setTrigger(MAP);
            mViewModel.setPoint(p);
        }
    }

    @Override
    public void onMapClick(@NotNull LatLng point) {
        setPoint(point);
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        setPoint(marker.getPosition());
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        setPoint(marker.getPosition());
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        setPoint(marker.getPosition());
    }

    @Override
    public void onCircleClick(@NonNull @NotNull Circle circle) {

    }

    @Override
    public void onInfoWindowClick(@NonNull @NotNull Marker marker) {

    }

    @Override
    public void onInfoWindowClose(@NonNull @NotNull Marker marker) {

    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        return false;
    }

    @Override
    public void onPolygonClick(@NonNull @NotNull Polygon polygon) {

    }

    @Override
    public void onPolylineClick(@NonNull @NotNull Polyline polyline) {

    }

    @Override
    public void onInfoWindowLongClick(@NonNull @NotNull Marker marker) {

    }
}
