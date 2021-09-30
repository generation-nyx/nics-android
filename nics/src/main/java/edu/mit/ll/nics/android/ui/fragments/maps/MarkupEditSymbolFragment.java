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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.database.entities.SymbologyGroup;
import edu.mit.ll.nics.android.databinding.FragmentMarkupSymbolEditBinding;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.maps.markup.MarkupSymbol;
import edu.mit.ll.nics.android.maps.markup.MarkupType;
import edu.mit.ll.nics.android.repository.SymbologyRepository;
import edu.mit.ll.nics.android.ui.viewmodel.MapViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditSymbolViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupEditSymbolViewModel.MarkupEditSymbolViewModelFactory;
import edu.mit.ll.nics.android.utils.BitmapUtils;
import edu.mit.ll.nics.android.utils.Utils;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;

import static edu.mit.ll.nics.android.utils.MapUtils.zoomToFeature;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_SYMBOL_REQUEST;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.INPUT;
import static edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger.MAP;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MarkupEditSymbolFragment extends MarkupEditFeatureFragment {

    private MarkupSymbol mMarkup;
    private MarkupEditSymbolViewModel mViewModel;
    private FragmentMarkupSymbolEditBinding mBinding;

    @Inject
    MarkupEditSymbolViewModelFactory mViewModelFactory;

    @Inject
    SymbologyRepository mSymbolRepository;

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_markup_symbol_edit.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_markup_symbol_edit, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MarkupEditSymbolViewModel} for this fragment.
     * Get a reference to the shared {@link MapViewModel}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the view model with the markup feature.
        mMarkup = initMarkup();
        MarkupEditSymbolViewModel.Factory factory = new MarkupEditSymbolViewModel.Factory(mViewModelFactory, mMarkup);
        mViewModel = new ViewModelProvider(this, factory).get(MarkupEditSymbolViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);

        subscribeToModel();


        // if a symbolpath is not already set, default to the first symbol
        if (mViewModel.getSelectedSymbolPath().getValue() == null) {
            List<String> groupNames = mSymbolRepository.getSymbologyGroupNames();
            if (groupNames.size() > 0) {
                SymbologyGroup symbolGroup = mSymbolRepository.getSymbologyByName(groupNames.get(0));
                if (symbolGroup != null && symbolGroup.getListing().getListing().size() > 0) {
                    SymbologyGroup.Symbology symbol = symbolGroup.getListing().getListing().get(0);
                    mViewModel.setSelectedSymbolPath(symbolGroup.getListing().getParentPath() + '/' + symbol.getFilename());
                    mViewModel.setSelectedSymbolDescription(symbol.getDescription());
                }
            }
        }
    }

    /**
     * Unbind from all xml layouts and remove the back button pressed callback.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    /**
     * When the fragment is destroyed, remove any state handle fields.
     */
    @Override
    public void onDestroy() {
        removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_SYMBOL_REQUEST);
        super.onDestroy();
    }

    private void subscribeToModel() {
        // Listen for responses from the symbol picker dialog and update the view model's symbol.
        subscribeToDestinationResponse(R.id.markupEditSymbolFragment, PICK_SYMBOL_REQUEST, (DestinationResponse<String[]>) result -> {
            if (result != null) {
                mViewModel.setSelectedSymbolPath(result[0]);
                mViewModel.setSelectedSymbolDescription(result[1]);
            }
        });

        // Update the markup comment when there are changes in the input text form.
        mViewModel.getComment().observe(mLifecycleOwner, this::setComment);

        // Update the marker's icon when it is changed via the SymbolPickerDialog.
        mViewModel.getSelectedSymbolPath().observe(mLifecycleOwner, this::setIcon);

        // Update the marker's title when it is changed via the SymbolPickerDialog.
        mViewModel.getSelectedSymbolDescription().observe(mLifecycleOwner, this::setDescription);


        // Observe when the map marker has changed and update the text fields accordingly.
        mViewModel.getPoint().observe(mLifecycleOwner, point -> {
            if (point == null) {
                mViewModel.clearTextFields();
            } else {
                if (point.getTrigger().equals(MAP)) {
                    mViewModel.setTextFields(point);
                }
            }
        });

        // Observe the latitude text field for changes and update the map accordingly.
        mViewModel.getLatitude().observe(mLifecycleOwner, lat -> {
            // Only update the map if the text field change was from user input.
            if (lat != null && lat.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });

        // Observe the longitude text field for changes and update the map accordingly.
        mViewModel.getLongitude().observe(mLifecycleOwner, lon -> {
            // Only update the map if the text field change was from user input.
            if (lon != null && lon.getTrigger().equals(INPUT)) {
                updateMap();
            }
        });
    }

    public void addToMap(LatLng point, LiveDataTrigger trigger) {
        mMarkup.addToMap(point);
        mViewModel.setPoint(new LiveDataTriggerEvent<>(point, trigger));
    }

    public void removeFromMap(LiveDataTrigger trigger) {
        mMarkup.removeFromMap();
        mViewModel.setPoint(new LiveDataTriggerEvent<>(null, trigger));
    }

    public void setComment(String comment) {
        mMarkup.setComments(comment);
    }

    public void setDescription(String desc) {
        mMarkup.setDescription(desc);
    }

    public void setIcon(String iconPath) {
        mMarkup.setImagePath(iconPath);

        Glide.with(mActivity)
                .asDrawable()
                .load(mPreferences.getSymbologyURL() + iconPath)
                .placeholder(R.drawable.x)
                .into(new CustomTarget<Drawable>(75, 75) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        mMarkup.setIcon(BitmapUtils.fromDrawable(resource));
                        mBinding.markupSymbolView.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        mMarkup.setIcon(BitmapUtils.fromDrawable(placeholder));
                        mBinding.markupSymbolView.setImageDrawable(placeholder);
                    }
                });
    }

    private void updateMap() {
        try {
            // Gather the text field values.
            String latitude = Objects.requireNonNull(mViewModel.getLatitude().getValue()).getData();
            String longitude = Objects.requireNonNull(mViewModel.getLongitude().getValue()).getData();

            // If the text is empty, the whole coordinate with be invalid, so throw an exception.
            if (Utils.multiEmptyCheck(latitude, longitude)) {
                throw new IllegalArgumentException();
            }

            // Create a LatLng object from the text fields.
            LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

            // Add/move the marker on the map depending on the new coordinate value.
            addToMap(latLng, INPUT);
        } catch (Exception e) {
            // If any exception occurs, the text fields are invalid, so remove the marker from the map.
            removeFromMap(INPUT);
        }
    }

    public void showSymbolPicker() {
        navigateSafe(mNavController, MarkupEditSymbolFragmentDirections.openSymbolPicker());
    }

    @Override
    protected MarkupSymbol initMarkup() {
        long id = MarkupEditSymbolFragmentArgs.fromBundle(getArguments()).getId();
        if (id != -1L) {
            MarkupFeature feature = mRepository.getMarkupFeatureById(id);
            mMapViewModel.setEditingMarkupId(id);
            zoomToFeature(mMap, feature);
            return new MarkupSymbol(mMap, mPreferences, mActivity, feature, true);
        } else {
            return new MarkupSymbol(mMap, mPreferences, mActivity, false, true);
        }
    }

    @Override
    public void myLocation() {
        if (Double.isNaN(mPreferences.getMDTLatitude()) || Double.isNaN(mPreferences.getMDTLongitude())) {
            Snackbar.make(requireView(), getString(R.string.no_gps_position), Snackbar.LENGTH_SHORT).show();
        } else {
            LatLng latLng = new LatLng(mPreferences.getMDTLatitude(), mPreferences.getMDTLongitude());
            addToMap(latLng, MAP);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }
    }

    @Override
    protected void clearMap() {
        mViewModel.setPoint(null);
        if (mMarkup != null) {
            mMarkup.removeFromMap();
        }
    }

    @Override
    public void submit() {
        submit(mMarkup);
    }

    @Override
    public void zoom() {
        try {
            LatLng point = Objects.requireNonNull(mViewModel.getPoint().getValue()).getData();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 13));
        } catch (Exception e) {
            Snackbar.make(requireView(), "No marker to zoom to.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public String getType() {
        return String.valueOf(MarkupType.marker);
    }

    @Override
    public void onMapClick(@NotNull LatLng latLng) {
        addToMap(latLng, MAP);
    }

    @Override
    public void onMarkerDragStart(@NotNull Marker marker) {
        addToMap(marker.getPosition(), MAP);
    }

    @Override
    public void onMarkerDrag(@NotNull Marker marker) {
        addToMap(marker.getPosition(), MAP);
    }

    @Override
    public void onMarkerDragEnd(@NotNull Marker marker) {
        addToMap(marker.getPosition(), MAP);
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {
        if (mViewModel.getPoint() != null) {
            showClearDialog();
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        return false;
    }
}
