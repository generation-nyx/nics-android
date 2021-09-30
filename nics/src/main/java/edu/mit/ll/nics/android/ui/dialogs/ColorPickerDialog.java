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

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import dagger.hilt.android.AndroidEntryPoint;
import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.databinding.DialogColorPickerBinding;
import edu.mit.ll.nics.android.utils.ExtensionsKt;
import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.Intents.PICK_COLOR_REQUEST;
import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

@AndroidEntryPoint
public class ColorPickerDialog extends AppDialog implements OnTouchListener {

    private int mColor;
    private NavController mNavController;
    private DialogColorPickerBinding mBinding;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Get the navigation controller for this view to use for navigating between the panels.
        mNavController = NavHostFragment.findNavController(this);

        mBinding.setLifecycleOwner(getViewLifecycleOwner());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.dialog_color_picker, null, false);

        mBinding.colorPickerImageView.setOnTouchListener(this);

        return new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.color_picker_title))
                .setIcon(R.drawable.nics_logo)
                .setView(mBinding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        mBinding = null;
        super.onDestroyView();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.color_picker_image_view) {
            int action = event.getAction();
            switch (action) {
                case (MotionEvent.ACTION_DOWN):
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    try {
                        mColor = findColor((ImageView) v, x, y);
                        ExtensionsKt.setNavigationResult(mNavController, PICK_COLOR_REQUEST, mColor);
                        dismiss();
                    } catch (NullPointerException e) {
                        return false;
                    }
                    break;
                case (MotionEvent.ACTION_UP):
                    v.performClick();
                    break;
            }
        }

        return true;
    }

    private static int findColor(ImageView view, int x, int y) throws NullPointerException {
        int red = 0;
        int green = 0;
        int blue = 0;
        int color;

        int offset = 1; // 3x3 Matrix
        int pixelsNumber = 0;

        int xImage;
        int yImage;

        // Get the bitmap from the view.
        Bitmap bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();

        // Calculate the target in the bitmap.
        xImage = (int) (x * ((double) bitmap.getWidth() / (double) view.getWidth()));
        yImage = (int) (y * ((double) bitmap.getHeight() / (double) view.getHeight()));

        // Average of pixels color around the center of the touch.
        for (int i = xImage - offset; i <= xImage + offset; i++) {
            for (int j = yImage - offset; j <= yImage + offset; j++) {
                try {
                    color = bitmap.getPixel(i, j);
                    red += Color.red(color);
                    green += Color.green(color);
                    blue += Color.blue(color);
                    pixelsNumber += 1;
                } catch (Exception e) {
                    Timber.tag(DEBUG).w("Error picking color!");
                }
            }
        }

        if (pixelsNumber != 0) {
            red = red / pixelsNumber;
            green = green / pixelsNumber;
            blue = blue / pixelsNumber;
        }

        Timber.tag(DEBUG).e("Color: %s, %s, %s", red, green, blue);

        return Color.rgb(red, green, blue);
    }

    public int getColor() {
        return mColor;
    }
}
