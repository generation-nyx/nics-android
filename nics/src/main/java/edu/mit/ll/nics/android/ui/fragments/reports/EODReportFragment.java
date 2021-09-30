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
package edu.mit.ll.nics.android.ui.fragments.reports;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.databinding.FragmentEodReportBinding;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.NetworkRepository;
import edu.mit.ll.nics.android.ui.adapters.PagerAdapter;
import edu.mit.ll.nics.android.ui.fragments.AppFragment;
import edu.mit.ll.nics.android.ui.fragments.TabFragment;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel.EODReportViewModelFactory;
import edu.mit.ll.nics.android.utils.Utils;
import edu.mit.ll.nics.android.utils.anim.ZoomOutPageTransformer;

import static edu.mit.ll.nics.android.utils.BitmapUtils.bitmapToByteArray;
import static edu.mit.ll.nics.android.utils.BitmapUtils.scaleBitmap;
import static edu.mit.ll.nics.android.utils.FileUtils.createJpegFile;
import static edu.mit.ll.nics.android.utils.FileUtils.saveBytesToFile;
import static edu.mit.ll.nics.android.utils.GeoUtils.getLocationFromString;
import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToDevice;
import static edu.mit.ll.nics.android.utils.ImageUtils.saveImageToStorage;
import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;
import static edu.mit.ll.nics.android.utils.Utils.forceHideKeyboard;
import static edu.mit.ll.nics.android.utils.Utils.popBackStack;
import static edu.mit.ll.nics.android.utils.constants.NICS.MAX_POST_IMAGE_QUALITY;
import static edu.mit.ll.nics.android.utils.constants.NICS.MAX_POST_IMAGE_SIZE;

@AndroidEntryPoint
public class EODReportFragment extends AppFragment {

    private ViewPager2 mViewPager;
    private EODReportViewModel mViewModel;
    private FragmentEodReportBinding mBinding;

    @Inject
    EODReportViewModelFactory mViewModelFactory;

    @Inject
    EODReportRepository mRepository;

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
     * nics/src/main/res/layout/fragment_eod_report.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_eod_report, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link EODReportViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = Navigation.findNavController(requireView());

        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_photo_white);

        // Create a new instance of the view model for this fragment.
        EODReportViewModel.Factory factory = new EODReportViewModel.Factory(mViewModelFactory, initEODReport(), defaultBitmap);
        mViewModel = new ViewModelProvider(this, factory).get(EODReportViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);

        mViewPager = mBinding.pager;
        mViewPager.setPageTransformer(new ZoomOutPageTransformer());

        List<TabFragment> fragments = Arrays.asList(
                EODReportTeamLocationFragment.newInstance(),
                EODReportTaskDescriptionFragment.newInstance(),
                EODReportUxoReportsFragment.newInstance(),
                EODImageFragment.newInstance()
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
        new TabLayoutMediator(tabLayout, mViewPager, (tab, position) ->
                tab.setText(fragments.get(position).getTabTitle(mContext))).attach();

        subscribeToModel();
    }

    /**
     * Subscribe this fragment to the {@link EODReportViewModel} to observe changes and dynamically
     * update the UI components and vice versa.
     */
    private void subscribeToModel() {
        mViewModel.getLatitude().observe(mLifecycleOwner, latitude -> {
            mViewModel.getEODReport().setLatitude(getLocationFromString(latitude));
            mBinding.executePendingBindings();
        });

        mViewModel.getLongitude().observe(mLifecycleOwner, longitude -> {
            mViewModel.getEODReport().setLongitude(getLocationFromString(longitude));
            mBinding.executePendingBindings();
        });

        mViewModel.getSelectedTaskType().observe(mLifecycleOwner, taskType -> {
            mViewModel.getEODReport().setTaskType(taskType);
            mBinding.executePendingBindings();
        });

        mViewModel.getSelectedTeam().observe(mLifecycleOwner, team -> {
            mViewModel.getEODReport().setTeam(team);
            mBinding.executePendingBindings();
        });

        mViewModel.getSelectedCanton().observe(mLifecycleOwner, canton -> {
            mViewModel.getEODReport().setCanton(canton);
            mBinding.executePendingBindings();
        });

        mViewModel.getSelectedTown().observe(mLifecycleOwner, town -> {
            mViewModel.getEODReport().setTown(town);
            mBinding.executePendingBindings();
        });

        mViewModel.getMacID().observe(mLifecycleOwner, macID -> {
            mViewModel.getEODReport().setMacID(macID);
            mBinding.executePendingBindings();
        });

        mViewModel.getMedevacPointTimeDistance().observe(mLifecycleOwner, medevacPointTimeDistance -> {
            mViewModel.getEODReport().setMedevacPointTimeDistance(medevacPointTimeDistance);
            mBinding.executePendingBindings();
        });

        mViewModel.getContactPerson().observe(mLifecycleOwner, contactPerson -> {
            mViewModel.getEODReport().setContactPerson(contactPerson);
            mBinding.executePendingBindings();
        });

        mViewModel.getContactPhone().observe(mLifecycleOwner, contactPhone -> {
            mViewModel.getEODReport().setContactPhone(contactPhone);
            mBinding.executePendingBindings();
        });

        mViewModel.getContactAddress().observe(mLifecycleOwner, contactAddress -> {
            mViewModel.getEODReport().setContactAddress(contactAddress);
            mBinding.executePendingBindings();
        });

        mViewModel.getRemarks().observe(mLifecycleOwner, remarks -> {
            mViewModel.getEODReport().setRemarks(remarks);
            mBinding.executePendingBindings();
        });

        mViewModel.getExpendedResources().observe(mLifecycleOwner, expendedResources -> {
            mViewModel.getEODReport().setExpendedResources(expendedResources);
            mBinding.executePendingBindings();
        });

        mViewModel.getDirectlyInvolved().observe(mLifecycleOwner, directlyInvolved -> {
            mViewModel.getEODReport().setDirectlyInvolved(directlyInvolved);
            mBinding.executePendingBindings();
        });

        mViewModel.isDraft().observe(mLifecycleOwner, isDraft -> {
            mViewModel.getEODReport().setDraft(isDraft);
            onBackPressedCallback.setEnabled(isDraft);
            mBinding.executePendingBindings();
        });

        mViewModel.getUxos().observe(mLifecycleOwner, uxos -> {
            mViewModel.getEODReport().setUxo(new ArrayList<>(uxos));
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

    @Override
    public void onDestroy() {
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
                showExitWithoutSavingDialog();
            } else {
                // Otherwise, select the previous step.
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        }
    };

    /**
     * Initialize the EOD report that will be tied to this form.
     * Either create a new one, or load one from the database using the id.
     *
     * @return {@link EODReport} The EOD report that will be associated with this form.
     */
    private EODReport initEODReport() {
        // Get the report id if there is one, otherwise it's a new report.
        long id = EODReportFragmentArgs.fromBundle(getArguments()).getId();

        if (id != -1L) {
            return mRepository.getEODReportById(id);
        } else {
            return new EODReport().create(mPreferences);
        }
    }

    /**
     * On click callback for the submit button. If either the latitude or longitude inputs don't
     * have values, then an alert dialog appears to warn the user that there is no location set for
     * the form. They can either submit the form as is, or go back and add a location.
     */
    public void submitReport() {
        if (Utils.hasNoValue(mViewModel.getEODReport().getLatitude()) || Utils.hasNoValue(mViewModel.getEODReport().getLongitude())) {
            new MaterialAlertDialogBuilder(mActivity)
                    .setTitle("No Location Set.")
                    .setIcon(R.drawable.nics_logo)
                    .setMessage("A location has not been set for this report. Would you like to set a location before sending?")
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(getString(R.string.no), (dialog, which) -> submit())
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            submit();
        }
    }

    /**
     * Adds the {@link EODReport} form to the database and posts it to the server using the
     * {@link NetworkRepository#postEODReports()} ()} api call. It then pops the current fragment off of
     * the back stack so that the view model can be properly cleared.
     */
    private void submit() {
        // If the rotation degrees isn't divisble by 360, then we know that the image has rotated.
        if (mViewModel.getBitmap().getValue() != null) {
            //scale bitmap to max 1024 in either direction and lower quality to 80%
            Bitmap bitmap = scaleBitmap(mViewModel.getBitmap().getValue(), MAX_POST_IMAGE_SIZE);
            File temp = createJpegFile(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            Uri uri = saveImageToDevice(bitmap, temp, MAX_POST_IMAGE_QUALITY);

            saveImageToStorage(bitmap, mContext.getContentResolver(), MAX_POST_IMAGE_QUALITY);
            mViewModel.getEODReport().setFullPath(uri.getPath());
        }

        mViewModel.getEODReport().setSeqTime(System.currentTimeMillis());
        mViewModel.getEODReport().setDraft(false);

        mRepository.addEODReportToDatabase(mViewModel.getEODReport(), result -> mMainHandler.post(() -> {
            mNetworkRepository.postEODReports();
            popBackStack(mNavController);
        }));
    }

    /**
     * On click callback for the clear form button. Shows an alert dialog confirming to the user that
     * they want to clear the form. If they say turnOn, it will clear all of the form data.
     */
    public void showClearDialog() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle("Clear Form")
                .setIcon(R.drawable.nics_logo)
                .setMessage("Would you like to clear this form?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> clearForm())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    public void clearForm() {
        mViewModel.setPhotoUri(null);
        mViewModel.setCurrentImageRotation(0);
        mViewModel.setLatitude("0.0");
        mViewModel.setLongitude("0.0");
        mViewModel.setDraft(true);
        mViewModel.setSelectedTeam(null);
        mViewModel.setSelectedCanton(null);
        mViewModel.setSelectedTown(null);
        mViewModel.setSelectedTaskType(null);
        mViewModel.setMacID(EMPTY);
        mViewModel.setMedevacPointTimeDistance(EMPTY);
        mViewModel.setContactPerson(EMPTY);
        mViewModel.setContactPhone(EMPTY);
        mViewModel.setContactAddress(EMPTY);
        mViewModel.setRemarks(EMPTY);
        mViewModel.setExpendedResources(EMPTY);
        mViewModel.setDirectlyInvolved(EMPTY);
        mViewModel.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_photo_white));
        mViewModel.getEODReport().setFullPath(EMPTY);
        mViewModel.setUxos(new ArrayList<>());
    }

    /**
     * If the user is editing a draft form and clicks the back button, this will show an
     * AlertDialog verifying that the user wants to exit without saving the draft or
     * submitting.
     */
    public void showExitWithoutSavingDialog() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle(getString(R.string.confirm_continue_to_title))
                .setIcon(R.drawable.nics_logo)
                .setMessage(String.format(getString(R.string.confirm_continue_to_description), getString(R.string.EODREPORT)))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> popBackStack(mNavController))
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    /**
     * Saves the form as it currently is to the local database and leaves it as a draft, so that
     * the user can continue working on it later. It then pops the current fragment off of the back
     * stack so that the view model can be properly cleared.
     */
    public void saveAsDraft() {
        mViewModel.getEODReport().setSeqTime(System.currentTimeMillis());
        mRepository.addEODReportToDatabase(mViewModel.getEODReport());
        popBackStack(mNavHostFragment);
    }

    /**
     * Copy the current report as a new draft report.
     */
    public void copyAsNewReport() {
        mViewModel.setDraft(true);
        mViewModel.getEODReport().setId(-1L);
    }
}
