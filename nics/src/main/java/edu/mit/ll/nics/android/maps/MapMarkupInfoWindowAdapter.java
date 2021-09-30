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
package edu.mit.ll.nics.android.maps;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.MapInfoWindowBinding;
import edu.mit.ll.nics.android.repository.SettingsRepository;

import static edu.mit.ll.nics.android.utils.UnitConverter.IMPERIAL;
import static edu.mit.ll.nics.android.utils.UnitConverter.NAUTICAL;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_TIME_FORMAT;

@AndroidEntryPoint
public class MapMarkupInfoWindowAdapter extends View implements InfoWindowAdapter {

    private final Activity mContext;
    private final GridLayout mGridLayout;
    private final SettingsRepository mSettings;
    private final MapInfoWindowBinding mBinding;

    public MapMarkupInfoWindowAdapter(Activity context,
                                      LifecycleOwner lifecycleOwner,
                                      SettingsRepository settings) {
        super(context);
        mContext = context;
        mSettings = settings;
        mBinding = MapInfoWindowBinding.inflate(LayoutInflater.from(mContext));
        mBinding.setLifecycleOwner(lifecycleOwner);
        mGridLayout = mBinding.mapInfoWindowGridLayout;
    }

    @Override
    public View getInfoContents(@NotNull Marker marker) {
        try {
            mGridLayout.removeAllViews();

            JsonObject data = new Gson().fromJson(marker.getTitle(), JsonObject.class);
            ImageView icon = mBinding.mapInfoWindowImage;

            if (data == null || data.size() == 0) {
                return null;
            }

            try {
                int iconId = data.remove("icon").getAsInt();
                icon.setImageResource(iconId);
                icon.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                icon.setVisibility(View.GONE);
            }

            try {
                String title = "";
                if (data.has("title")) {
                    title = data.remove("title").getAsString();
                }
                //if(data.get("type").getAsString().equals("sr") || data.get("type").getAsString().equals("dmgrpt") || data.get("type").getAsString().equals("ur")) {
                if (data.has("type")) {
                    if (data.get("type").getAsString().equals("sr") || data.get("type").getAsString().equals("eod")) {
                        TextView clickView = new TextView(mContext);
                        TextViewCompat.setTextAppearance(clickView, R.style.TextAppearance_AppCompat_Medium);
                        clickView.setTextColor(ContextCompat.getColor(mContext, R.color.holo_blue_dark));
                        clickView.setTypeface(null, Typeface.BOLD);
                        clickView.setPadding(0, 5, 0, 0);
                        clickView.setText(title);
                        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                        GridLayout.Spec colSpec = GridLayout.spec(0, 2, GridLayout.CENTER);
                        mGridLayout.addView(clickView, new GridLayout.LayoutParams(rowSpec, colSpec));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Set<Entry<String, JsonElement>> dataSet = data.entrySet();

            if (dataSet.size() > 1) {
                mGridLayout.setRowCount(dataSet.size() - 1);
            } else {
                mGridLayout.setRowCount(1);
            }

            for (Entry<String, JsonElement> entry : dataSet) {
                String key = entry.getKey();
                String value = null;

                if (key.equals(getResources().getString(R.string.markup_task_date)) || key.equals(getResources().getString(R.string.markup_timestamp))) {
                    value = DateFormat.format(NICS_TIME_FORMAT, new Date(entry.getValue().getAsLong())).toString();
                } else if (key.equals(getResources().getString(R.string.markup_elevation))) {
                    DecimalFormat formatter = new DecimalFormat("#.#######");
                    value = formatter.format(entry.getValue().getAsDouble()) + " " + getResources().getString(R.string.markup_meters);
                } else if (key.equals(getResources().getString(R.string.markup_course))) {
                    DecimalFormat formatter = new DecimalFormat("#.#######");
                    value = formatter.format(entry.getValue().getAsDouble());
                } else if (key.equals(getResources().getString(R.string.markup_distance))) {
                    String systemOfMeasurement = mSettings.getSelectedSystemOfMeasurement();
                    String units;

                    switch (systemOfMeasurement) {
                        case IMPERIAL:
                            units = getResources().getString(R.string.markup_miles);
                            break;
                        case NAUTICAL:
                            units = getResources().getString(R.string.markup_nautical_miles);
                            break;
                        default:
                            units = getResources().getString(R.string.markup_kilometers);
                            break;
                    }

                    if (entry.getValue().getAsDouble() != 0.0) {
                        DecimalFormat formatter = new DecimalFormat("#.#######");
                        value = formatter.format(entry.getValue().getAsDouble()) + " " + units;
                    }
                } else if (key.equals(getResources().getString(R.string.markup_area))) {
                    String systemOfMeasurement = mSettings.getSelectedSystemOfMeasurement();
                    String units;
                    switch (systemOfMeasurement) {
                        case IMPERIAL:
                            units = getResources().getString(R.string.markup_acres);
                            break;
                        case NAUTICAL:
                            units = getResources().getString(R.string.markup_nautical_miles_squared);
                            break;
                        default:
                            units = getResources().getString(R.string.markup_kilometers_squared);
                            break;
                    }

                    if (entry.getValue().getAsDouble() != 0.0) {
                        DecimalFormat formatter = new DecimalFormat("#.#######");
                        value = formatter.format(entry.getValue().getAsDouble()) + " " + units;
                    }
                } else {
                    if (entry.getValue() != null && !entry.getValue().isJsonNull()) {
                        value = entry.getValue().getAsString();
                    }
                }

                if (value != null && !value.isEmpty() && !key.equals("reportId") && !key.equals("type") && !key.equals("payload")) {
                    TextView titleView = new TextView(mContext);
                    titleView.setTypeface(null, Typeface.BOLD);
                    titleView.setTextColor(Color.BLACK);
                    titleView.setText(key);
                    titleView.setPadding(0, 0, 5, 0);

                    GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                    GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.LEFT);
                    mGridLayout.addView(titleView, new GridLayout.LayoutParams(rowSpec, colSpec));

                    TextView valueView = new TextView(mContext);
                    TextViewCompat.setTextAppearance(valueView, R.style.TextAppearance_AppCompat_Small);
                    valueView.setTextColor(Color.BLACK);

                    if (key.equals(getResources().getString(R.string.markup_area))) {
                        SpannableStringBuilder cs = new SpannableStringBuilder(value);

                        String systemOfMeasurement = mSettings.getSelectedSystemOfMeasurement();
                        char forIndexing = 'w';
                        switch (systemOfMeasurement) {
                            case IMPERIAL:
                                break;
                            case NAUTICAL:
                                forIndexing = 'i';
                                break;
                            default:
                                forIndexing = 'm';
                                break;
                        }

                        if (forIndexing != 'w') {
                            int index = value.indexOf(forIndexing);
                            cs.setSpan(new SuperscriptSpan(), index + 1, index + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            cs.setSpan(new RelativeSizeSpan(0.75f), index + 1, index + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            valueView.setText(cs);
                        } else {
                            valueView.setText(Html.fromHtml(value));
                        }
                    } else {
                        valueView.setText(Html.fromHtml(value));
                    }

                    valueView.setMaxWidth(350);
                    mGridLayout.addView(valueView);
                }
            }

            DecimalFormat df = new DecimalFormat("###.####");

            TextView latTitleView = new TextView(mContext);
            TextViewCompat.setTextAppearance(latTitleView, R.style.TextAppearance_AppCompat_Small);
            latTitleView.setTextColor(Color.BLACK);
            latTitleView.setTypeface(null, Typeface.BOLD);
            latTitleView.setText(R.string.latitude);

            TextView latValueView = new TextView(mContext);
            TextViewCompat.setTextAppearance(latValueView, R.style.TextAppearance_AppCompat_Small);
            latValueView.setTextColor(Color.BLACK);
            latValueView.setTypeface(null, Typeface.NORMAL);
            latValueView.setText(df.format(marker.getPosition().latitude));

            TextView lonTitleView = new TextView(mContext);
            TextViewCompat.setTextAppearance(lonTitleView, R.style.TextAppearance_AppCompat_Small);
            lonTitleView.setTextColor(Color.BLACK);
            lonTitleView.setTypeface(null, Typeface.BOLD);
            lonTitleView.setText(R.string.longitude);

            TextView lonValueView = new TextView(mContext);
            TextViewCompat.setTextAppearance(lonValueView, R.style.TextAppearance_AppCompat_Small);
            lonValueView.setTextColor(Color.BLACK);
            lonValueView.setTypeface(null, Typeface.NORMAL);
            lonValueView.setText(df.format(marker.getPosition().longitude));

            GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.BOTTOM);
            GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.LEFT);

            mGridLayout.addView(latTitleView, new GridLayout.LayoutParams(rowSpec, colSpec));
            mGridLayout.addView(latValueView);
            mGridLayout.addView(lonTitleView, new GridLayout.LayoutParams(rowSpec, colSpec));
            mGridLayout.addView(lonValueView);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return mBinding.getRoot();
    }

    @Override
    public View getInfoWindow(@NotNull Marker marker) {
        return null;
    }
}
