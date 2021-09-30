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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.tsuryo.swipeablerv.SwipeLeftRightCallback;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.databinding.FragmentMarkupPanelDefaultBinding;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.interfaces.MarkupFeatureItemCallback;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;
import edu.mit.ll.nics.android.maps.markup.MarkupFireLine;
import edu.mit.ll.nics.android.maps.tags.FeatureTag;
import edu.mit.ll.nics.android.maps.tags.HazardTag;
import edu.mit.ll.nics.android.maps.tags.ReportTag;
import edu.mit.ll.nics.android.ui.adapters.MarkupFeatureAdapter;
import edu.mit.ll.nics.android.ui.fragments.MapFragmentDirections;
import edu.mit.ll.nics.android.ui.viewmodel.maps.MarkupPanelDefaultViewModel;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.GeoUtils.calculateGeometryMidpoint;
import static edu.mit.ll.nics.android.utils.MapUtils.computeArea;
import static edu.mit.ll.nics.android.utils.MapUtils.computeDistance;
import static edu.mit.ll.nics.android.utils.MapUtils.createInfoMarker;
import static edu.mit.ll.nics.android.utils.MapUtils.getShapesFromFeatures;
import static edu.mit.ll.nics.android.utils.MapUtils.zoomToFeature;
import static edu.mit.ll.nics.android.utils.StringUtils.SPACE;
import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.isValidUsername;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public class MarkupPanelDefaultFragment extends MapBaseFragment {

    private MarkupFeatureAdapter mAdapter;
    private MarkupPanelDefaultViewModel mViewModel;
    private FragmentMarkupPanelDefaultBinding mBinding;

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_markup_panel_default.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_markup_panel_default, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link MarkupPanelDefaultViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(MarkupPanelDefaultViewModel.class);

        mMapViewModel.setIsEditing(false);
        mMapViewModel.setEditingMarkupId(-1L);

        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setReadOnly(!mPreferences.getSelectedCollabroom().doIHaveMarkupPermission(mPreferences.getUserId()));
        mBinding.setFragment(this);

        mAdapter = new MarkupFeatureAdapter(mMarkupFeatureClickCallback, mPreferences);
        mBinding.setAdapter(mAdapter);

        mBinding.markupShapesListView.setListener(new SwipeLeftRightCallback.Listener() {
            @Override
            public void onSwipedLeft(int position) {
                showEditDialog(mAdapter.getItem(position));
            }

            @Override
            public void onSwipedRight(int position) {
                showDeleteDialog(mAdapter.getItem(position));
            }
        });

        subscribeToModel(mAdapter);
    }

    private void subscribeToModel(MarkupFeatureAdapter adapter) {
        mMapViewModel.getMarkupFeatures().observe(mLifecycleOwner, features -> {
            GoogleMap map = mMapViewModel.getMap();
            if (features != null && map != null) {
                adapter.setMarkupFeatures(getShapesFromFeatures(features, map, mPreferences, mActivity));
            }
            mBinding.executePendingBindings();
        });
    }

    /**
     * Unbind from all xml layouts and cancel any pending dialogs.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    private final MarkupFeatureItemCallback<MarkupBaseShape> mMarkupFeatureClickCallback = new MarkupFeatureItemCallback<MarkupBaseShape>() {
        @Override
        public void onEditClick(MarkupBaseShape feature) {
            showEditDialog(feature);
        }

        @Override
        public void onDeleteClick(MarkupBaseShape feature) {
            showDeleteDialog(feature);
        }

        @Override
        public void onClick(MarkupBaseShape item) {
            zoomToFeature(mMapViewModel.getMap(), item);
        }
    };

    private void showEditDialog(MarkupBaseShape feature) {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle(getString(R.string.editThisFeature).concat("?"))
                .setIcon(R.drawable.nics_logo)
                .setMessage(getString(R.string.editConfirmation))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> editFeature(feature))
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .setOnDismissListener(dialog -> {
                    int position = mAdapter.getItem(feature);
                    if (position >= 0) {
                        mAdapter.notifyItemChanged(position);
                    }
                })
                .create()
                .show();
    }

    private void showDeleteDialog(MarkupBaseShape feature) {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle(getString(R.string.deleteThisFeature).concat("?"))
                .setIcon(R.drawable.nics_logo)
                .setMessage(getString(R.string.deleteConfirmation)
                        .concat((feature instanceof MarkupFireLine) ? " fireline?" :
                                (SPACE).concat(feature.getType().toString()).concat("?")))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> deleteFeature(feature))
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .setOnDismissListener(dialog -> {
                    int position = mAdapter.getItem(feature);
                    if (position >= 0) {
                        mAdapter.notifyItemChanged(position);
                    }
                })
                .create()
                .show();
    }

    public void editFeature(MarkupBaseShape feature) {
        long id = feature.getId();
        switch (feature.getType()) {
            case marker:
                editSymbol(id);
                break;
            case label:
                editText(id);
                break;
            case circle:
                editCircle(id);
                break;
            case sketch:
                if (feature instanceof MarkupFireLine) {
                    editFireLine(id);
                } else {
                    editLine(id);
                }
                break;
            case square:
                editRectangle(id);
                break;
            case hexagon:
            case polygon:
            case triangle:
                editPolygon(id);
                break;
        }
    }

    public void deleteFeature(MarkupBaseShape feature) {
        // Save a copy of the markup feature.
        MarkupFeature markup = mRepository.getMarkupFeatureById(feature.getId());

        // Remove the feature from the local db, so that it removes it from the map.
        mRepository.deleteMarkupFeatureById(feature.getId());

        // Show a snackbar to give the user 4 seconds to undo the deletion.
        Snackbar snackbar = Snackbar.make(mBinding.getRoot(), getString(R.string.featureDeleted), Snackbar.LENGTH_INDEFINITE);
        snackbar.setDuration(4000);

        // If user clicks the UNDO button, add feature back to database.
        snackbar.setAction(getString(R.string.undo), view -> mRepository.addMarkupToDatabase(markup));
        snackbar.show();

        // If the snackbar closes for any reason other than the undo button,
        // add the feature back to the database, except with the delete flag.
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    markup.setSendStatus(SendStatus.DELETE);

                    // deleteMarkupFeatures won't delete a feature unless its the current user
                    markup.setUserName(mPreferences.getUserName());

                    mRepository.addMarkupToDatabase(markup, result -> mMainHandler.post(() -> {
                        mNetworkRepository.deleteMarkupFeatures();
                    }));
                }
            }
        });
    }

    public void newSymbol() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editSymbol());
    }

    public void newLine() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editLine());
    }

    public void newRectangle() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editRectangle());
    }

    public void newPolygon() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editPolygon());
    }

    public void newCircle() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editCircle());
    }

    public void newFreehandPolygon() {
        View dialogView = View.inflate(mActivity, R.layout.dialog_freehand_selection, null);
        MaterialButton freehandPolygonButton = dialogView.findViewById(R.id.freehandPolygonButton);
        MaterialButton freehandPolylineButton = dialogView.findViewById(R.id.freehandPolylineButton);
        AlertDialog dialog = new MaterialAlertDialogBuilder(mActivity)
                .setTitle("Freehand Drawing")
                .setIcon(R.drawable.nics_logo)
                .setCancelable(true)
                .setView(dialogView)
                .create();

        freehandPolygonButton.setOnClickListener(v -> {
            navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editFreeHandPolygon());
            dialog.dismiss();
        });

        freehandPolylineButton.setOnClickListener(v -> {
            navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editFreeHandPolyline());
            dialog.dismiss();
        });

        dialog.show();
    }

    public void newFireline() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editFireline());
    }

    public void newText() {
        navigateSafe(mNavController, MarkupPanelDefaultFragmentDirections.editText());
    }

    public void editSymbol(long id) {
        MarkupPanelDefaultFragmentDirections.EditSymbol action = MarkupPanelDefaultFragmentDirections.editSymbol();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    public void editLine(long id) {
        MarkupPanelDefaultFragmentDirections.EditLine action = MarkupPanelDefaultFragmentDirections.editLine();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    public void editFireLine(long id) {
        MarkupPanelDefaultFragmentDirections.EditFireline action = MarkupPanelDefaultFragmentDirections.editFireline();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    public void editRectangle(long id) {
        MarkupPanelDefaultFragmentDirections.EditRectangle action = MarkupPanelDefaultFragmentDirections.editRectangle();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    public void editPolygon(long id) {
        MarkupPanelDefaultFragmentDirections.EditPolygon action = MarkupPanelDefaultFragmentDirections.editPolygon();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    public void editCircle(long id) {
        MarkupPanelDefaultFragmentDirections.EditCircle action = MarkupPanelDefaultFragmentDirections.editCircle();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    public void editText(long id) {
        MarkupPanelDefaultFragmentDirections.EditText action = MarkupPanelDefaultFragmentDirections.editText();
        action.setId(id);
        navigateSafe(mNavController, action);
    }

    @Override
    public void onInfoWindowClick(@NonNull @NotNull Marker marker) {
        try {
            if (marker.getTag() instanceof ReportTag) {
                if (((ReportTag<?>) marker.getTag()).getReport() instanceof EODReport) {
                    EODReport eodReport = (EODReport) ((ReportTag<?>) marker.getTag()).getReport();
                    MapFragmentDirections.OpenEODReport action = MapFragmentDirections.openEODReport();
                    action.setId(eodReport.getId());
                    navigateSafe(mNavHostFragment.getNavController(), action);
                } else if (((ReportTag<?>) marker.getTag()).getReport() instanceof GeneralMessage) {
                    GeneralMessage generalMessage = (GeneralMessage) ((ReportTag<?>) marker.getTag()).getReport();
                    MapFragmentDirections.OpenGeneralMessage action = MapFragmentDirections.openGeneralMessage();
                    action.setId(generalMessage.getId());
                    navigateSafe(mNavHostFragment.getNavController(), action);
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d(e, "Failed to open report from info window click.");
        }
    }

    @Override
    public void onInfoWindowClose(@NonNull @NotNull Marker marker) {
        if (mMapFragment.getInfoMarker() != null && mMapFragment.getInfoMarker().equals(marker)) {
            mMapFragment.removeInfoWindowMarker();
        }
    }

    @Override
    public void onMapClick(@NonNull @NotNull LatLng latLng) {
        try {
            List<MarkupBaseShape> shapes = mAdapter.getMarkupFeatures();
            for (MarkupBaseShape shape : shapes) {
                if (shape instanceof MarkupFireLine && shape.isTouchedBy(latLng)) {
                    MarkupFireLine fireLine = (MarkupFireLine) shape;
                    MarkupFeature feature = fireLine.getFeature();
                    List<LatLng> points = fireLine.getPoints();
                    LatLng midpoint = fireLine.getFirelineBounds().getCenter();
                    double distance = computeDistance(points, mSettings.getSelectedSystemOfMeasurement());
                    String comments = feature.getAttributes().getComments();
                    String username = mPreferences.getUserNickName(feature.getUserName());
                    username = isValidUsername(username) ? username : feature.getUserName();

                    JsonObject attr = new JsonObject();
                    int resourceId = R.drawable.line_black;
                    switch (feature.getDashStyle()) {
                        case "plannedFireline":
                        case "primary-fire-line":
                            resourceId = R.drawable.planned_fire_line;
                            break;
                        case "secondaryFireline":
                        case "secondary-fire-line":
                            resourceId = R.drawable.secondary_fire_line;
                            break;
                        case "fireSpreadPrediction":
                        case "fire-spread-prediction":
                            resourceId = R.drawable.fire_spread_prediction;
                            break;
                        case "completedDozer":
                        case "completed-dozer-line":
                            resourceId = R.drawable.completed_dozer_line;
                            break;
                        case "proposedDozer":
                        case "proposed-dozer-line":
                            resourceId = R.drawable.proposed_dozer_line;
                            break;
                        case "fireEdge":
                        case "fire-edge-line":
                            resourceId = R.drawable.fire_edge_line;
                            break;
                        case "map":
                        case "action-point":
                            resourceId = R.drawable.management_action_point;
                            break;
                        default:
                            break;
                    }

                    attr.addProperty("icon", resourceId);
                    attr.addProperty(getString(R.string.markup_user), username);
                    attr.addProperty(getString(R.string.markup_message), feature.getLabelText());
                    attr.addProperty(getString(R.string.markup_distance), distance);
                    attr.addProperty(getString(R.string.markup_comment), comments);

                    mMapFragment.setInfoMarker(createInfoMarker(fireLine.getFirelineBounds().getCenter(), attr));
                    break;
                }
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d("Failed to show fireline info window.");
        }
    }

    @Override
    public void onMapLongClick(@NonNull @NotNull LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(@NonNull @NotNull Marker marker) {
        mMapViewModel.setSelectedMarker(marker);
        return false;
    }

    @Override
    public void onPolygonClick(@NonNull @NotNull Polygon polygon) {
        try {
            List<LatLng> points = polygon.getPoints();
            LatLng midpoint = calculateGeometryMidpoint(points);

            if (polygon.getTag() instanceof FeatureTag) {
                FeatureTag tag = (FeatureTag) polygon.getTag();
                MarkupFeature feature = tag.getFeature();
                double area = computeArea(points, mSettings.getSelectedSystemOfMeasurement());
                String comments = feature.getAttributes().getComments();
                String username = mPreferences.getUserNickName(feature.getUserName());
                username = isValidUsername(username) ? username : feature.getUserName();

                JsonObject attr = new JsonObject();
                String markupType = feature.getType();
                switch (markupType) {
                    case "circle":
                        attr.addProperty("icon", R.drawable.circle_black);
                        break;
                    case "square":
                        attr.addProperty("icon", R.drawable.rectangle_black);
                        break;
                    default:
                        attr.addProperty("icon", R.drawable.trapezoid_black);
                        break;
                }

                attr.addProperty(getString(R.string.markup_user), username);
                attr.addProperty(getString(R.string.markup_message), feature.getLabelText());
                attr.addProperty(getString(R.string.markup_area), area);

                if (!emptyCheck(comments)) {
                    attr.addProperty(getString(R.string.markup_comment), comments);
                }

                mMapFragment.setInfoMarker(createInfoMarker(midpoint, attr));
            } else if (polygon.getTag() instanceof HazardTag) {
                HazardTag tag = (HazardTag) polygon.getTag();
                Hazard hazard = tag.getHazard();

                JsonObject attr = new JsonObject();
                attr.addProperty("icon", R.drawable.alert_yellow);
                attr.addProperty(getString(R.string.markup_message), hazard.getHazardLabel());
                attr.addProperty(getString(R.string.markup_comment), hazard.getHazardType());
                mMapFragment.setInfoMarker(createInfoMarker(midpoint, attr));
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d("Failed to show polyline info window.");
        }
    }

    @Override
    public void onPolylineClick(@NonNull @NotNull Polyline polyline) {
        super.onPolylineClick(polyline);
        try {
            if (polyline.getTag() instanceof FeatureTag) {
                List<LatLng> points = polyline.getPoints();
                LatLng midpoint = calculateGeometryMidpoint(points);
                FeatureTag tag = (FeatureTag) polyline.getTag();
                MarkupFeature feature = tag.getFeature();
                double distance = computeDistance(points, mSettings.getSelectedSystemOfMeasurement());
                String comments = feature.getAttributes().getComments();
                String username = mPreferences.getUserNickName(feature.getUserName());
                username = isValidUsername(username) ? username : feature.getUserName();

                JsonObject attr = new JsonObject();
                attr.addProperty("icon", R.drawable.line_black);
                attr.addProperty(getString(R.string.markup_user), username);
                attr.addProperty(getString(R.string.markup_message), feature.getLabelText());
                attr.addProperty(getString(R.string.markup_distance), distance);

                if (!emptyCheck(comments)) {
                    attr.addProperty(getString(R.string.markup_comment), comments);
                }

                mMapFragment.setInfoMarker(createInfoMarker(midpoint, attr));
            }
        } catch (Exception e) {
            Timber.tag(DEBUG).d("Failed to show polyline info window.");
        }
    }
}
