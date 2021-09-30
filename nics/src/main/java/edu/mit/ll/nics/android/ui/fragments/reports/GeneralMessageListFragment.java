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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.data.ReportProgress;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.databinding.FragmentGeneralMessageListBinding;
import edu.mit.ll.nics.android.enums.SortBy;
import edu.mit.ll.nics.android.enums.SortOrder;
import edu.mit.ll.nics.android.interfaces.ClickCallback;
import edu.mit.ll.nics.android.interfaces.ReportClickCallback;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.ui.adapters.GeneralMessageAdapter;
import edu.mit.ll.nics.android.ui.adapters.GeneralMessageAdapter.GeneralMessageAdapterFactory;
import edu.mit.ll.nics.android.ui.adapters.GeneralMessageAdapter.GeneralMessageViewHolder;
import edu.mit.ll.nics.android.ui.fragments.MapFragment;
import edu.mit.ll.nics.android.ui.viewmodel.GeneralMessageListViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_GENERAL_MESSAGE_PROGRESS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_GENERAL_MESSAGES_WORKER;

@AndroidEntryPoint
public class GeneralMessageListFragment extends ReportListFragment {

    private GeneralMessageAdapter mAdapter;
    private GeneralMessageListViewModel mViewModel;
    private FragmentGeneralMessageListBinding mBinding;
    private boolean mEventsRegistered = false;

    @Inject
    GeneralMessageRepository mRepository;

    @Inject
    GeneralMessageAdapterFactory mAdapterFactory;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the nav controller for this fragment.
        mNavController = mNavHostFragment.getNavController();

        // Request an update of the most recent reports.
        refresh();

        Timber.tag(DEBUG).d("Created %s", GeneralMessageListFragment.class.getSimpleName());
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_general_message_list.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_general_message_list, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link GeneralMessageListViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(GeneralMessageListViewModel.class);

        // Bind the lifecycle owner, viewmodel, and the fragment to the xml layout.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setReadOnly(!mPreferences.getSelectedCollabroom().doIHaveMarkupPermission(mPreferences.getUserId()));
        mBinding.setFragment(this);

        // Initialize and bind the paged list adapter to the xml layout.
        mAdapter = mAdapterFactory.create(mClickCallback, mRetryCallback, mViewModel, mLifecycleOwner);
        mBinding.setAdapter(mAdapter);
        mBinding.executePendingBindings();

        mBinding.swipeRefresh.setOnRefreshListener(() -> {
            refresh();
            mBinding.swipeRefresh.setRefreshing(false);
        });

        // Subscribe to the view model's live data observables.
        subscribeToModel();
    }

    /**
     * Observe the {@link LiveData} objects from the {@link GeneralMessageListViewModel} and make
     * updates accordingly.
     */
    private void subscribeToModel() {
        // Observe the general messages to be shown in the paged list view.
        mViewModel.getGeneralMessages().observe(mLifecycleOwner, reports -> {
            // Update the list with the general message reports.
            mAdapter.submitData(mLifecycleOwner.getLifecycle(), reports);
        });

        // Observe the general message GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_GENERAL_MESSAGES_WORKER).observe(mLifecycleOwner, workInfos -> {
            for (WorkInfo workInfo : workInfos) {
                // If the progress reported by the worker is not 100 (finished), then set loading to true.
                mViewModel.setLoading(workInfo != null && workInfo.getProgress().getInt(PROGRESS, 100) != 100);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mEventsRegistered) {
            LiveDataBus.subscribe(NICS_GENERAL_MESSAGE_PROGRESS, this, this::onSendProgress);
            mEventsRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mEventsRegistered) {
            LiveDataBus.unregister(NICS_GENERAL_MESSAGE_PROGRESS);
            mEventsRegistered = false;
        }
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    /**
     * Inflate the menu options from nics/src/main/res/menu/general_message_list.
     * @param menu The {@link Menu} menu interface to manage the options menu for this fragment.
     * @param inflater The {@link MenuInflater} inflater that will instantiate the general message
     *                 list XML objects.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.general_message_list, menu);
    }

    /**
     * Perform an action depending on which options menu item was selected.
     *
     * @param item {@link MenuItem} that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            // Toggle showing the search bar.
            mViewModel.toggleSearching();
        } else if (item.getItemId() == R.id.sort) {
            // Show a popup menu with different sorting options.
            showSortingOptions();
        } else if (item.getItemId() == R.id.refresh) {
            // Request an update of the most recent general messages.
            refresh();
        } else if (item.getItemId() == R.id.markAllAsRead) {
            // Mark all general messages as read in the database.
            mRepository.markAllGeneralMessagesRead();
        }

        return super.onOptionsItemSelected(item);
    }

    private void onSendProgress(Object data) {
        if (data instanceof ReportProgress) {
            ReportProgress progress = (ReportProgress) data;

            List<GeneralMessage> reports = mAdapter.snapshot().getItems();

            for (int i = 0; i < reports.size(); i++) {
                try {
                    GeneralMessage report = reports.get(i);

                    if (report.getId() == progress.getReportId()) {
                        GeneralMessageViewHolder holder = (GeneralMessageViewHolder) mBinding.generalMessages.findViewHolderForLayoutPosition(i);
                        if (holder != null) {
                            Timber.tag(DEBUG).d("Setting progress for report %s: %s", report.getId(), progress.getProgress());
                            holder.setProgress(progress.getProgress());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Show a popup menu of all of the sorting options. This will be attached to the sorting option
     * {@link MenuItem}.
     */
    private void showSortingOptions() {
        // Create a new popup menu and inflate it with the sorting options.
        PopupMenu popupMenu = new PopupMenu(mActivity, mActivity.findViewById(R.id.sort));
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.report_sort, popupMenu.getMenu());

        // Initialize the current checked values for the sorting menu.
        Menu menu = popupMenu.getMenu();
        if (mViewModel.getSortOrder().getValue().equals(SortOrder.ASC)) {
            menu.findItem(R.id.ascending).setChecked(true);
        } else {
            menu.findItem(R.id.descending).setChecked(true);
        }

        // Initialize what the menu sort by option should be based upon what is selected in view model.
        SortBy sortBy = mViewModel.getSortBy().getValue();
        switch (sortBy) {
            case DESCRIPTION:
                menu.findItem(R.id.sortByDescription).setChecked(true);
                break;
            case USERNAME:
                menu.findItem(R.id.sortByUsername).setChecked(true);
                break;
            default:
                menu.findItem(R.id.sortByDate).setChecked(true);
                break;
        }

        // Set the item click listener to update the view model with the selection.
        popupMenu.setOnMenuItemClickListener(item -> {
            // Update the menu item to toggle the checked option.
            item.setChecked(!item.isChecked());

            // TODO realized that looking up by title doesnt work because it could change languages .. need to do it by resource id
            // Set sort order or the sort by depending on which group the item is in.
            if (item.getGroupId() == R.id.order) {
                mViewModel.setSortOrder(SortOrder.lookUp(String.valueOf(item.getTitle())));
            } else if (item.getGroupId() == R.id.sort) {
                mViewModel.setSortBy(SortBy.lookUp(String.valueOf(item.getTitle())));
            }

            // Don't close the popup menu when the user makes a selection.
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(getContext()));

            return false;
        });

        popupMenu.show();
    }

    @Override
    public void refresh() {
        mNetworkRepository.getGeneralMessages();
        mNetworkRepository.postGeneralMessages();
        if (mAdapter != null) {
            List<GeneralMessage> reports = mAdapter.snapshot().getItems();

            for (int i = 0; i < reports.size(); i++) {
                try {
                    GeneralMessageViewHolder holder = (GeneralMessageViewHolder) mBinding.generalMessages.findViewHolderForLayoutPosition(i);
                    if (holder != null) {
                        holder.refreshPicasso();
                        // TODO might not need to do this notify item changed.
                        mAdapter.notifyItemChanged(i);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * If there is a selected report and this is clicked, then unselect the report. If there isn't a
     * currently selected report, navigate to the {@link GeneralMessageFragment} to create a new
     * report.
     */
    @Override
    public void add() {
        if (mViewModel.getSelectedReport().getValue() != null) {
            mViewModel.setSelectedReport(null);
        } else {
            // add new report.
            navigateSafe(mNavController, GeneralMessageListFragmentDirections.openGeneralMessage());
        }
    }

    /**
     * View the selected report in the {@link GeneralMessageFragment}.
     */
    @Override
    public void view() {
        GeneralMessage report = mViewModel.getSelectedReport().getValue();
        if (report != null) {
            // Set has read for the selected report to true if it hasn't been read yet.
            if (!report.hasRead()) {
                mRepository.markAsRead(report.getId());
            }

            openReport(report.getId());
        }
    }

    /**
     * Edit the current selected report. This will currently only be an option for draft reports.
     */
    @Override
    public void edit() {
        GeneralMessage report = mViewModel.getSelectedReport().getValue();
        if (report != null && report.isDraft()) {
            openReport(report.getId());
        }
    }

    /**
     * Show a delete report {@link AlertDialog} for a confirmation to the user that they want to
     * delete the selected report. This will currently only be an option for draft reports.
     */
    @Override
    public void delete() {
        new MaterialAlertDialogBuilder(mActivity)
                .setTitle("Delete Report?")
                .setIcon(R.drawable.nics_logo)
                .setMessage("Are You Sure That You Want To Delete This Report?")
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> deleteReport())
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .create()
                .show();
    }

    /**
     * If the user confirms to delete the selected report, delete the report from the database and
     * reset the selected report in the view model to null.
     */
    protected void deleteReport() {
        GeneralMessage report = mViewModel.getSelectedReport().getValue();
        if (report != null) {
            mRepository.deleteGeneralMessageById(report.getId());
            mViewModel.setSelectedReport(null);
        }
    }

    @Override
    protected void sendProgress(Object data) {

    }

    /**
     * Open the {@link MapFragment} and zoom to where this particular report is located.
     */
    public void openReportInMap() {
        GeneralMessage report = mViewModel.getSelectedReport().getValue();
        if (report != null) {
            Timber.tag(DEBUG).d("Editing general message report %s.", report.getId());
            GeneralMessageListFragmentDirections.OpenGeneralMessageInMap action = GeneralMessageListFragmentDirections.openGeneralMessageInMap();
            action.setGeneralMessageId(report.getId());
            mViewModel.setSelectedReport(null);
            navigateSafe(mNavController, action);
        }
    }

    protected void openReport(long id) {
        // Open the general message fragment and pass the id of the selected report to it.
        GeneralMessageListFragmentDirections.OpenGeneralMessage action = GeneralMessageListFragmentDirections.openGeneralMessage();
        action.setId(id);
        mViewModel.setSelectedReport(null);
        navigateSafe(mNavController, action);
    }

    /**
     * The click callback for when a report is selected in the {@link RecyclerView}. Set the selected
     * report in the view model to be whatever report was selected in the list.
     */
    private final ReportClickCallback<GeneralMessage> mClickCallback = new ReportClickCallback<GeneralMessage>() {
        @Override
        public void onClick(GeneralMessage report) {
            mViewModel.setSelectedReport(report);
        }
    };

    private final ClickCallback mRetryCallback = this::refresh;
}