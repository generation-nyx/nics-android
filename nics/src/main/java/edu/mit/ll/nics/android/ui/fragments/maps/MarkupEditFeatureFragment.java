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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;
import edu.mit.ll.nics.android.enums.SendStatus;
import edu.mit.ll.nics.android.maps.markup.MarkupBaseShape;

import static edu.mit.ll.nics.android.utils.Utils.popBackStack;

@SuppressLint("PotentialBehaviorOverride")
@AndroidEntryPoint
public abstract class MarkupEditFeatureFragment extends MapBaseFragment {

    protected BitmapDescriptor markerBlueDescriptor;
    protected BitmapDescriptor markerYellowDescriptor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Saves the instance of the bitmap that represents the currently selected marker.
        Bitmap vertexImageYellow = BitmapFactory.decodeResource(getResources(), R.drawable.yellow_dot);
        vertexImageYellow = Bitmap.createScaledBitmap(vertexImageYellow, 50, 50, false);
        markerYellowDescriptor = BitmapDescriptorFactory.fromBitmap(vertexImageYellow);

        // Save the instance of the bitmap that represents non selected markers.
        Bitmap vertexImageBlue = BitmapFactory.decodeResource(getResources(), R.drawable.blue_dot);
        vertexImageBlue = Bitmap.createScaledBitmap(vertexImageBlue, 50, 50, false);
        markerBlueDescriptor = BitmapDescriptorFactory.fromBitmap(vertexImageBlue);

        // Register on back pressed callback.
        mActivity.getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapViewModel.setIsEditing(true);
    }

    @Override
    public void onDestroy() {
        mMapViewModel.setIsEditing(false);
        mMapViewModel.setEditingMarkupId(-1L);
        onBackPressedCallback.remove();
        super.onDestroy();
    }

    public abstract void myLocation();

    public abstract String getType();

    public abstract void zoom();

    protected abstract void clearMap();

    protected abstract MarkupBaseShape initMarkup();

    public abstract void submit();

    public void submit(MarkupBaseShape markup) {
        if (markup == null) {
            Snackbar.make(requireView(), getString(R.string.pleaseAddFeature), Snackbar.LENGTH_LONG).show();
            return;
        }

        if (!markup.isValid()) {
            Snackbar.make(requireView(), markup.getInvalidString(), Snackbar.LENGTH_SHORT).show();
            return;
        }

        MarkupFeature feature = markup.toFeature();
        feature.buildVector2Point(true);

        long id = mMapViewModel.getEditingMarkupId().getValue();
        if (id != -1L) {
            feature.setId(id);
            feature.setHazards();

            // Get all features that haven't posted yet.
            ArrayList<MarkupFeature> features = mRepository.getAllMarkupReadyToSendForUser(mPreferences.getUserName());

            // If the feature hasn't posted yet, just replace it locally and try to send it again.
            boolean isLocalUpdate = false;
            for (MarkupFeature f : features) {
                if (f.getId() == (id)) {
                    isLocalUpdate = true;
                    break;
                }
            }

            if (isLocalUpdate) {
                mRepository.addMarkupToDatabase(feature, result -> mMainHandler.post(() -> mNetworkRepository.postMarkupFeatures()));
            } else {
                feature.setSendStatus(SendStatus.UPDATE);
                mRepository.addMarkupToDatabase(feature, result -> mMainHandler.post(() -> mNetworkRepository.updateMarkupFeatures()));
            }
        } else {
            mRepository.addMarkupToDatabase(feature, result -> mMainHandler.post(() -> mNetworkRepository.postMarkupFeatures()));
        }

        exit();
    }

    protected void exit() {
        clearMap();
        popBackStack(mNavController);
    }

    public void showExitDialog() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle(getString(R.string.confirm_continue_to_title))
                .setIcon(R.drawable.nics_logo)
                .setMessage(String.format(getString(R.string.confirm_continue_to_description), getType()))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> exit())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    public void showClearDialog() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle("Clear Markup?")
                .setIcon(R.drawable.nics_logo)
                .setMessage("Are you sure that you want to clear the current markup?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> clearMap())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            showExitDialog();
        }
    };
}
