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
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.databinding.FragmentEodReportListBinding;
import edu.mit.ll.nics.android.enums.SortBy;
import edu.mit.ll.nics.android.enums.SortOrder;
import edu.mit.ll.nics.android.interfaces.ClickCallback;
import edu.mit.ll.nics.android.interfaces.ReportClickCallback;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.ui.adapters.EODReportAdapter;
import edu.mit.ll.nics.android.ui.adapters.EODReportAdapter.EODReportAdapterFactory;
import edu.mit.ll.nics.android.ui.adapters.EODReportAdapter.EODReportViewHolder;
import edu.mit.ll.nics.android.ui.fragments.MapFragment;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportListViewModel;
import edu.mit.ll.nics.android.utils.livedata.LiveDataBus;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.constants.Events.NICS_EOD_REPORT_PROGRESS;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;
import static edu.mit.ll.nics.android.workers.AppWorker.PROGRESS;
import static edu.mit.ll.nics.android.workers.Workers.GET_EOD_REPORTS_WORKER;

@AndroidEntryPoint
public class EODReportListFragment extends ReportListFragment {

    private EODReportAdapter mAdapter;
    private EODReportListViewModel mViewModel;
    private FragmentEodReportListBinding mBinding;
    private boolean mEventsRegistered = false;

    @Inject
    EODReportRepository mRepository;

    @Inject
    EODReportAdapterFactory mAdapterFactory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.tag(DEBUG).d("Created %s", EODReportListFragment.class.getSimpleName());
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_eod_report_list.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_eod_report_list, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the {@link EODReportListViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();
        mViewModel = new ViewModelProvider(this).get(EODReportListViewModel.class);

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
     * Observe the {@link LiveData} objects from the {@link EODReportListViewModel} and make
     * updates accordingly.
     */
    private void subscribeToModel() {
        // Observe the eod reports to be shown in the paged list view.
        mViewModel.getEodReports().observe(mLifecycleOwner, reports -> {
            // Update the list with the EOD reports.
            mAdapter.submitData(mLifecycleOwner.getLifecycle(), reports);
        });

        // Observe the EOD report GET request worker's progress to update the xml layout depending on if it's loading or not.
        mWorkManager.getWorkInfosForUniqueWorkLiveData(GET_EOD_REPORTS_WORKER).observe(mLifecycleOwner, workInfos -> {
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
            LiveDataBus.subscribe(NICS_EOD_REPORT_PROGRESS, this, this::sendProgress);
            mEventsRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mEventsRegistered) {
            LiveDataBus.unregister(NICS_EOD_REPORT_PROGRESS);
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
     * Inflate the menu options from nics/src/main/res/menu/eod_report_list.
     * @param menu The {@link Menu} menu interface to manage the options menu for this fragment.
     * @param inflater The {@link MenuInflater} inflater that will instantiate the EOD report
     *                 list XML objects.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.eod_report_list, menu);
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
            // Request an update of the most recent eod reports.
            refresh();
        } else if (item.getItemId() == R.id.markAllAsRead) {
            // Mark all general messages as read in the database.
            mRepository.markAllEODReportsRead();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void sendProgress(Object data) {
        if (data instanceof ReportProgress) {
            ReportProgress progress = (ReportProgress) data;

            List<EODReport> reports = mAdapter.snapshot().getItems();

            for (int i = 0; i < reports.size(); i++) {
                try {
                    EODReport report = reports.get(i);

                    if (report.getId() == progress.getReportId()) {
                        EODReportViewHolder holder = (EODReportViewHolder) mBinding.eodReports.findViewHolderForLayoutPosition(i);
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
        PopupMenu popupMenu = new PopupMenu(mActivity, mActivity.findViewById(R.id.sort));
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.eod_report_sort, popupMenu.getMenu());

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
            case TEAM:
                menu.findItem(R.id.sortByTeam).setChecked(true);
                break;
            case CANTON:
                menu.findItem(R.id.sortByCanton).setChecked(true);
                break;
            case TOWN:
                menu.findItem(R.id.sortByTown).setChecked(true);
                break;
            default:
                menu.findItem(R.id.sortByDate).setChecked(true);
                break;
        }

        // Set the item click listener to update the view model with the selection.
        popupMenu.setOnMenuItemClickListener(item -> {
            // Update the menu item to toggle the checked option.
            item.setChecked(!item.isChecked());

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

    /**
     * If there is a selected report and this is clicked, then unselect the report. If there isn't a
     * currently selected report, navigate to the {@link EODReportFragment} to create a new
     * report.
     */
    @Override
    public void add() {
        if (mViewModel.getSelectedReport().getValue() != null) {
            mViewModel.setSelectedReport(null);
        } else {
            // add new report.
            navigateSafe(mNavController, EODReportListFragmentDirections.openEODReport());
        }
    }

    /**
     * View the selected report in the {@link EODReportFragment}.
     */
    @Override
    public void view() {
        EODReport report = mViewModel.getSelectedReport().getValue();
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
        EODReport report = mViewModel.getSelectedReport().getValue();
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
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void refresh() {
        mNetworkRepository.getEODReports();
        mNetworkRepository.postEODReports();
        if (mAdapter != null) {
            List<EODReport> reports = mAdapter.snapshot().getItems();

            for (int i = 0; i < reports.size(); i++) {
                try {
                    EODReportViewHolder holder = (EODReportViewHolder) mBinding.eodReports.findViewHolderForLayoutPosition(i);
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
     * Open the {@link MapFragment} and zoom to where this particular report is located.
     */
    @Override
    public void openReportInMap() {
        EODReport report = mViewModel.getSelectedReport().getValue();
        if (report != null) {
            Timber.tag(DEBUG).d("Editing EOD report %s.", report.getId());
            EODReportListFragmentDirections.OpenEODReportInMap action = EODReportListFragmentDirections.openEODReportInMap();
            action.setEodReportId(report.getId());
            mViewModel.setSelectedReport(null);
            navigateSafe(mNavController, action);
        }
    }

    @Override
    protected void openReport(long id) {
        // Open the general message fragment and pass the id of the selected report to it.
        EODReportListFragmentDirections.OpenEODReport action = EODReportListFragmentDirections.openEODReport();
        action.setId(id);
        mViewModel.setSelectedReport(null);
        navigateSafe(mNavController, action);
    }

    /**
     * If the user confirms to delete the selected report, delete the report from the database and
     * reset the selected report in the view model to null.
     */
    @Override
    protected void deleteReport() {
        EODReport report = mViewModel.getSelectedReport().getValue();
        if (report != null) {
            mRepository.deleteEODReportById(report.getId());
            mViewModel.setSelectedReport(null);
        }
    }

    /**
     * The click callback for when a report is selected in the {@link RecyclerView}. Set the selected
     * report in the view model to be whatever report was selected in the list.
     */
    private final ReportClickCallback<EODReport> mClickCallback = new ReportClickCallback<EODReport>() {
        @Override
        public void onClick(EODReport report) {
            mViewModel.setSelectedReport(report);
        }
    };

    private final ClickCallback mRetryCallback = this::refresh;
}
