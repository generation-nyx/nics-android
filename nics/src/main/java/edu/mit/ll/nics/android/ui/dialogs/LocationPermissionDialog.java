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
package edu.mit.ll.nics.android.ui.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.DialogLocationPermissionsBinding;
import edu.mit.ll.nics.android.repository.SettingsRepository;
import edu.mit.ll.nics.android.services.ServiceManager;

import static edu.mit.ll.nics.android.utils.CheckPermissions.getLocationPermissions;
import static edu.mit.ll.nics.android.utils.Utils.isTrue;
import static edu.mit.ll.nics.android.utils.constants.NICS.PRIVACY_POLICY;

@AndroidEntryPoint
public class LocationPermissionDialog extends AppDialog {

    private DialogLocationPermissionsBinding mBinding;
    private ActivityResultLauncher<String[]> mRequestLocationPermissions;

    @Inject
    ServiceManager mServiceManager;

    @Inject
    SettingsRepository mSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply custom style to this dialog.
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog);

        // Register for activity result callback for when the user responds to the location permissions request.
        mRequestLocationPermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::locationPermissionsResult);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_location_permissions, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.setLifecycleOwner(getViewLifecycleOwner());
        mBinding.setDialog(this);
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    public void privacyPolicy() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)));
    }

    public void turnOn() {
        mRequestLocationPermissions.launch(getLocationPermissions());
    }

    private void locationPermissionsResult(Map<String, Boolean> results) {
        if (isTrue(results.values())) {
            // Start location updates now that permission is accepted.
            mServiceManager.getLocationService().startLocationUpdates(mSettings.getMDTDataRate());
        } else {
            Snackbar.make(requireView(), "Location permissions denied. Mobile device tracking will be turned off.", Snackbar.LENGTH_LONG).show();
        }

        dismiss();
    }

    public void noThanks() {
        dismiss();
    }
}
