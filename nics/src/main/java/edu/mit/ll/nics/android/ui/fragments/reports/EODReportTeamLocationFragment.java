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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.FragmentEodTeamLocationBinding;
import edu.mit.ll.nics.android.interfaces.DestinationResponse;
import edu.mit.ll.nics.android.ui.fragments.TabFragment;
import edu.mit.ll.nics.android.ui.viewmodel.EODReportViewModel;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.Utils.navigateSafe;
import static edu.mit.ll.nics.android.utils.Utils.removeSafe;
import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_LOCATION_REQUEST;

@AndroidEntryPoint
public class EODReportTeamLocationFragment extends TabFragment {

    private EODReportViewModel mViewModel;
    private FragmentEodTeamLocationBinding mBinding;

    public static EODReportTeamLocationFragment newInstance() {
        return new EODReportTeamLocationFragment();
    }

    /**
     * Bind to the layout for this fragment.
     *
     * The layout resource file for this fragment is located at
     * nics/src/main/res/layout/fragment_eod_team_location.xml.
     */
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_eod_team_location, container, false);
        return mBinding.getRoot();
    }

    /**
     * Initialize the shared {@link EODReportViewModel} for this fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mLifecycleOwner = getViewLifecycleOwner();

        // Initialize dropdowns with options.
        initEODTeamDropdown();
        initEODCantonDropdown();
        initEODTownDropdown(unskoSanski());

        // Get the EOD shared view model.
        mViewModel = new ViewModelProvider(requireParentFragment()).get(EODReportViewModel.class);

        // Bind all variables to the xml.
        mBinding.setLifecycleOwner(mLifecycleOwner);
        mBinding.setViewModel(mViewModel);
        mBinding.setFragment(this);

        subscribeToModel();
    }

    private void subscribeToModel() {
        // Listen for responses from the map point selector dialog.
        subscribeToDestinationResponse(R.id.EODReportFragment, PICK_LOCATION_REQUEST, (DestinationResponse<LatLng>) location -> {
            if (location != null) {
                mViewModel.setLatitude(String.valueOf(location.latitude));
                mViewModel.setLongitude(String.valueOf(location.longitude));
            }
            removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_LOCATION_REQUEST);
        });

        mViewModel.getSelectedCanton().observe(mLifecycleOwner, this::refreshTownDropdown);
    }

    /**
     * Unbind from all xml layouts.
     */
    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        removeSafe(mNavBackStackEntry.getSavedStateHandle(), PICK_LOCATION_REQUEST);
        super.onDestroy();
    }

    private void initEODTeamDropdown() {
        List<String> options = Arrays.asList(
                getString(R.string.eod_categorytype_none),
                getString(R.string.eod_team_bihac_a),
                getString(R.string.eod_team_busovaca_a),
                getString(R.string.eod_team_livno_a),
                getString(R.string.eod_team_gorazde_a),
                getString(R.string.eod_team_mostar_a),
                getString(R.string.eod_team_sarajevo_a),
                getString(R.string.eod_team_tuzla_a),
                getString(R.string.eod_team_zepce_a),
                getString(R.string.eod_team_fucz)
        );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, R.layout.dropdown_item, options);
        mBinding.setEodTeamAdapter(adapter);
        mBinding.executePendingBindings();
    }

    private void initEODCantonDropdown() {
        List<String> options = Arrays.asList(
                getString(R.string.eod_canton_unsko_sanski),
                getString(R.string.eod_canton_posavski),
                getString(R.string.eod_canton_tuzlanski),
                getString(R.string.eod_canton_zenicko_dobojski),
                getString(R.string.eod_canton_bosansko_podrinjski),
                getString(R.string.eod_canton_srednjo_bosanski),
                getString(R.string.eod_canton_hercegovacko_neretvanski),
                getString(R.string.eod_canton_zapadno_hercegovacki),
                getString(R.string.eod_canton_sarajevo),
                getString(R.string.eod_canton_kanton_10)
        );
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, R.layout.dropdown_item, options);
        mBinding.setEodCantonAdapter(adapter);
        mBinding.executePendingBindings();
    }

    private void initEODTownDropdown(List<String> options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity, R.layout.dropdown_item, options);

        if (mViewModel != null) {
            mViewModel.setSelectedTown(adapter.getItem(0));
        }

        mBinding.setEodTownAdapter(adapter);
        mBinding.executePendingBindings();
    }

    // TODO probably a better way to do this, but this way we can get the strings from resources.
    private void refreshTownDropdown(String canton) {
        if (!emptyCheck(canton)) {
            if (canton.equals(getString(R.string.eod_canton_unsko_sanski))) {
                initEODTownDropdown(unskoSanski());
            } else if (canton.equals(getString(R.string.eod_canton_posavski))) {
                initEODTownDropdown(posavski());
            } else if (canton.equals(getString(R.string.eod_canton_tuzlanski))) {
                initEODTownDropdown(tuzlanski());
            } else if (canton.equals(getString(R.string.eod_canton_zenicko_dobojski))) {
                initEODTownDropdown(zenickoDobojski());
            } else if (canton.equals(getString(R.string.eod_canton_bosansko_podrinjski))) {
                initEODTownDropdown(bosanskoPodrinjski());
            } else if (canton.equals(getString(R.string.eod_canton_srednjo_bosanski))) {
                initEODTownDropdown(srednjoBosanski());
            } else if (canton.equals(getString(R.string.eod_canton_hercegovacko_neretvanski))) {
                initEODTownDropdown(hercegovackoNeretvanski());
            } else if (canton.equals(getString(R.string.eod_canton_zapadno_hercegovacki))) {
                initEODTownDropdown(zapadnoHercegovacki());
            } else if (canton.equals(getString(R.string.eod_canton_sarajevo))) {
                initEODTownDropdown(sarajevo());
            } else if (canton.equals(getString(R.string.eod_canton_kanton_10))) {
                initEODTownDropdown(kanton10());
            }
        }
    }

    private List<String> unskoSanski() {
        return Arrays.asList(
                getString(R.string.eod_town_bosanska_krupa),
                getString(R.string.eod_town_bosanski_petrovac),
                getString(R.string.eod_town_buzim),
                getString(R.string.eod_town_grad_bihac),
                getString(R.string.eod_town_grad_cazin),
                getString(R.string.eod_town_kljuc),
                getString(R.string.eod_town_sanski_most),
                getString(R.string.eod_town_velika_kladusa)
        );
    }

    private List<String> posavski() {
        return Arrays.asList(
                getString(R.string.eod_town_domaljevac_samac),
                getString(R.string.eod_town_odzak),
                getString(R.string.eod_town_orasje)
        );
    }

    private List<String> tuzlanski() {
        return Arrays.asList(
                getString(R.string.eod_town_banovici),
                getString(R.string.eod_town_celic),
                getString(R.string.eod_town_doboj_istok),
                getString(R.string.eod_town_gracanica),
                getString(R.string.eod_town_grad_tuzla),
                getString(R.string.eod_town_gradacac),
                getString(R.string.eod_town_kalesija),
                getString(R.string.eod_town_kladanj),
                getString(R.string.eod_town_lukavac),
                getString(R.string.eod_town_sapna),
                getString(R.string.eod_town_srebrenik),
                getString(R.string.eod_town_teocak),
                getString(R.string.eod_town_zivinice)
        );
    }

    private List<String> zenickoDobojski() {
        return Arrays.asList(
                getString(R.string.eod_town_breza),
                getString(R.string.eod_town_doboj_jug),
                getString(R.string.eod_town_grad_zenica),
                getString(R.string.eod_town_kakanj),
                getString(R.string.eod_town_maglaj),
                getString(R.string.eod_town_olovo),
                getString(R.string.eod_town_tesanj),
                getString(R.string.eod_town_usora),
                getString(R.string.eod_town_vares),
                getString(R.string.eod_town_visoko),
                getString(R.string.eod_town_zavidovici),
                getString(R.string.eod_town_zepce)
        );
    }

    private List<String> bosanskoPodrinjski() {
        return Arrays.asList(
                getString(R.string.eod_town_foca_fbih),
                getString(R.string.eod_town_grad_gorazde),
                getString(R.string.eod_town_pale_fbih)
        );
    }

    private List<String> srednjoBosanski() {
        return Arrays.asList(
                getString(R.string.eod_town_bugojno),
                getString(R.string.eod_town_busovaca),
                getString(R.string.eod_town_dobretici),
                getString(R.string.eod_town_donji_vakuf),
                getString(R.string.eod_town_fojnica),
                getString(R.string.eod_town_gornji_vakuf_uskoplje),
                getString(R.string.eod_town_jajce),
                getString(R.string.eod_town_kiseljak),
                getString(R.string.eod_town_kresevo),
                getString(R.string.eod_town_novi_travnik),
                getString(R.string.eod_town_travnik),
                getString(R.string.eod_town_vitez)
        );
    }

    private List<String> hercegovackoNeretvanski() {
        return Arrays.asList(
                getString(R.string.eod_town_capljina),
                getString(R.string.eod_town_citluk),
                getString(R.string.eod_town_grad_mostar),
                getString(R.string.eod_town_jablanica),
                getString(R.string.eod_town_konjic),
                getString(R.string.eod_town_neum),
                getString(R.string.eod_town_prozor),
                getString(R.string.eod_town_ravno),
                getString(R.string.eod_town_stolac)
        );
    }

    private List<String> zapadnoHercegovacki() {
        return Arrays.asList(
                getString(R.string.eod_town_grad_siroki_brijeg),
                getString(R.string.eod_town_grude),
                getString(R.string.eod_town_ljubuski),
                getString(R.string.eod_town_posusje)
        );
    }

    private List<String> sarajevo() {
        return Arrays.asList(
                getString(R.string.eod_town_centar_sarajevo),
                getString(R.string.eod_town_hadzici),
                getString(R.string.eod_town_ilidza),
                getString(R.string.eod_town_ilijas),
                getString(R.string.eod_town_novi_grad_sarajevo),
                getString(R.string.eod_town_novo_sarajevo),
                getString(R.string.eod_town_stari_grad_sarajevo),
                getString(R.string.eod_town_trnovo),
                getString(R.string.eod_town_vogosca)
        );
    }

    private List<String> kanton10() {
        return Arrays.asList(
                getString(R.string.eod_town_banovici),
                getString(R.string.eod_town_celic),
                getString(R.string.eod_town_doboj_istok),
                getString(R.string.eod_town_gracanica),
                getString(R.string.eod_town_grad_tuzla),
                getString(R.string.eod_town_gradacac),
                getString(R.string.eod_town_kalesija),
                getString(R.string.eod_town_kladanj),
                getString(R.string.eod_town_lukavac),
                getString(R.string.eod_town_sapna),
                getString(R.string.eod_town_srebrenik),
                getString(R.string.eod_town_teocak),
                getString(R.string.eod_town_zivinice)
        );
    }


    public void setMyLocation() {
        if (Double.isNaN(mPreferences.getMDTLatitude()) || Double.isNaN(mPreferences.getMDTLongitude())) {
            Snackbar.make(requireView(), getString(R.string.cant_find_your_location), Snackbar.LENGTH_SHORT).show();
        } else {
            mViewModel.setLatitude(String.valueOf(mPreferences.getMDTLatitude()));
            mViewModel.setLongitude(String.valueOf(mPreferences.getMDTLongitude()));
        }
    }

    public void openInMap() {
        LatLng latLng = null;

        try {
            String latitude = mViewModel.getLatitude().getValue();
            String longitude = mViewModel.getLongitude().getValue();

            latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
        } catch (Exception ignored) {
        }

        EODReportFragmentDirections.OpenLocationSelector action = EODReportFragmentDirections.openLocationSelector();
        action.setSelectionMode(true);
        action.setSelectionPoint(latLng);
        navigateSafe(mNavController, action);
    }

    @Override
    public void refresh() {

    }

    @Override
    public String getTabTitle(Context context) {
        return context.getString(R.string.eod_team_section_displayname);
    }

    @Override
    public String getTabContentDescription(Context context) {
        return context.getString(R.string.eod_team_section_displayname);
    }
}
