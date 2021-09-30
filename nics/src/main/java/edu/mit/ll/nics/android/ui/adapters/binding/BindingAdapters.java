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
package edu.mit.ll.nics.android.ui.adapters.binding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.databinding.BindingAdapter;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.regex.Pattern;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.utils.anim.AnimationUtils;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.Utils.emptyCheck;
import static edu.mit.ll.nics.android.utils.anim.AnimationUtils.animateConstraintHeightPercent;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class BindingAdapters {

    @BindingAdapter("visible")
    public static void showHide(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("visibleAnimate")
    public static void showHideAnimate(View view, boolean show) {
        view.setVisibility(View.VISIBLE);
        view.animate().setDuration(200)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @BindingAdapter("image")
    public static void setImage(MaterialButton button, int id) {
        button.setBackgroundResource(id);
    }

    @BindingAdapter("icon")
    public static void setIcon(MaterialButton button, int id) {
        button.setIconResource(id);
    }

    @BindingAdapter("image")
    public static void setImage(ImageView view, Bitmap bitmap) {
        view.setImageBitmap(bitmap);
    }

    @BindingAdapter("typeface")
    public static void setTypeface(TextView v, int style) {
        v.setTypeface(null, style);
    }

    @BindingAdapter("error")
    public static void setError(TextInputEditText v, String error) {
        v.setError(error);
    }

    @BindingAdapter("animateFab")
    public static void toggleAnimateFab(FloatingActionButton fab, boolean show) {
        // Checks if the visibility is the same as the show action. If it is then don't show any animation.
        int visibility = fab.getVisibility();
        if ((visibility == View.INVISIBLE || visibility == View.GONE) == show) {
            AnimationUtils.toggleShow(fab, show);
        }
    }

    @BindingAdapter("rotateFab")
    public static void rotateFab(FloatingActionButton fab, boolean rotate) {
        AnimationUtils.rotateAnimation(fab, rotate ? 135.0f : 0f);

        fab.setBackgroundTintList(rotate ?
                ColorStateList.valueOf(ContextCompat.getColor(fab.getContext(), R.color.red500)) :
                ColorStateList.valueOf(ContextCompat.getColor(fab.getContext(), R.color.holo_blue)));
    }

    @BindingAdapter("autoCompleteAdapter")
    public static <T extends ListAdapter & Filterable> void setAdapter(MaterialAutoCompleteTextView v, T adapter) {
        v.setAdapter(adapter);
    }

    @BindingAdapter("backgroundColor")
    public static void setBackgroundColor(MaterialButton button, int color) {
        button.setBackgroundColor(color);
        button.getBackground().invalidateSelf();
    }

    @BindingAdapter("active")
    public static void setActive(ImageButton button, boolean isActive) {
        if (isActive) {
            button.getBackground().setColorFilter(null);
        } else {
            button.getBackground().setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.GRAY, BlendModeCompat.MODULATE));
        }
        button.getBackground().invalidateSelf();
    }

    @BindingAdapter("strokeWidth")
    public static void setStrokeWidth(MaterialCardView view, float dimension) {
        view.setStrokeWidth((int) dimension);
    }

    @BindingAdapter("strokeWidth")
    public static void setStrokeWidth(MaterialButton view, float dimension) {
        view.setStrokeWidth((int) dimension);
    }

    @BindingAdapter("strokeColor")
    public static void setStrokeColor(MaterialButton view, float color) {
        view.setStrokeColor(ColorStateList.valueOf((int) color));
    }

    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    @BindingAdapter({"badge", "badgeVisible", "badgeNumber"})
    public static void setBadge(View view, BadgeDrawable badge, boolean visible, int number) {
        badge.setNumber(number);
        badge.setVisible(visible);
        BadgeUtils.attachBadgeDrawable(badge, view);
    }

    @BindingAdapter({"url", "picasso", "loading"})
    public static void picasso(AppCompatImageView view, String url,
                               @NonNull Picasso picasso, CircularProgressIndicator loading) {
        try {
            if (!emptyCheck(url)) {
                Timber.tag(DEBUG).d("Loading image from %s using Picasso.", url);

                File file = new File(url);
                if (file.exists()) {
                    loading.setVisibility(View.VISIBLE);
                    picasso.load(file).into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                            loading.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.tag(DEBUG).e(e,"Error downloading image using Picasso.");
                            loading.setVisibility(View.GONE);
                            view.setImageResource(R.drawable.error_outline_red);
                        }
                    });
                } else {
                    loading.setVisibility(View.VISIBLE);
                    picasso.load(url).into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                            loading.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.tag(DEBUG).e(e, "Error downloading image using Picasso.");
                            loading.setVisibility(View.GONE);
                            view.setImageResource(R.drawable.error_outline_red);
                        }
                    });
                }
            }
        } catch (Exception e) {
            loading.setVisibility(View.GONE);
            Timber.tag(DEBUG).e(e, "Failed to load image using picasso.");
        }
    }

    @BindingAdapter("fullscreen")
    public static void fullscreen(View view, float percent) {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        animateConstraintHeightPercent(view, lp.matchConstraintPercentHeight, percent);
    }

    @BindingAdapter({"highlight", "searching", "outgoing"})
    public static void highlight(MaterialTextView view, String search, boolean searching, boolean outgoing) {
        String text = view.getText().toString();
        if (searching && !TextUtils.isEmpty(search) && !TextUtils.isEmpty(text)) {
            if (Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text).find()) {
                try {
                    int startPos = StringUtils.indexOfIgnoreCase(text, search);
                    int endPos = startPos + search.length();
                    SpannableString spanText = new SpannableString(text);

                    if (outgoing) {
                        spanText.setSpan(new BackgroundColorSpan(Color.BLACK), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spanText.setSpan(new ForegroundColorSpan(Color.WHITE), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        spanText.setSpan(new BackgroundColorSpan(Color.WHITE), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spanText.setSpan(new ForegroundColorSpan(Color.BLACK), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    view.setText(spanText);
                } catch (Exception e) {
                    Timber.tag(DEBUG).d(e, "Issue with highlighting text.");
                }
            }
        } else {
            view.setText(text);
        }
    }

    @BindingAdapter("textChangedListener")
    public static void bindTextWatcher(TextInputEditText editText, TextWatcher textWatcher) {
        editText.addTextChangedListener(textWatcher);
    }

    @BindingAdapter("alignParentEnd")
    public static void alignParentEnd(View v, boolean alignParentEnd) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();

        if (alignParentEnd) {
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }
        v.setLayoutParams(params);
    }
}
