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

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import com.google.android.material.card.MaterialCardView;

@BindingMethods({
        @BindingMethod(type = MaterialCardView.class, attribute = "checkedValue", method = "setCheckedValue")
})
public class CardViewBindingAdapter {

    @InverseBindingAdapter(attribute = "checkedValue", event = "checkedAttrChanged")
    public static boolean getCheckedValue(MaterialCardView view) {
        return view.isChecked();
    }

    @BindingAdapter("checkedValue")
    public static void setCheckedValue(MaterialCardView view, boolean isChecked) {
        if (view.isChecked() != isChecked) {
            view.setChecked(isChecked);
        }
    }

    @BindingAdapter(value = {"onCheckedChanged", "checkedAttrChanged"}, requireAll = false)
    public static void setListeners(MaterialCardView cardView, MaterialCardView.OnCheckedChangeListener listener,
                                    InverseBindingListener attrChange) {
        if (listener == null && attrChange == null) {
            cardView.setOnCheckedChangeListener(listener);
        } else {
            cardView.setOnCheckedChangeListener((card, isChecked) -> {
                if (listener != null) {
                    listener.onCheckedChanged(card, isChecked);
                }
                attrChange.onChange();
            });
        }
    }
}
