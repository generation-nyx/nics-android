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

import android.widget.AdapterView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

@BindingMethods({
        @BindingMethod(type = MaterialAutoCompleteTextView.class, attribute = "selection", method = "setSelection")
})
public class AutoCompleteTextViewBindingAdapter {

    @InverseBindingAdapter(attribute = "selection", event = "selectionAttrChanged")
    public static String getSelection(MaterialAutoCompleteTextView view) {
        return view.getText().toString();
    }

    @BindingAdapter("selection")
    public static void setSelection(MaterialAutoCompleteTextView view, String value) {
        if (!view.getText().toString().equals(value)) {
            view.setText(value, false);
        }
    }

    @BindingAdapter("onItemSelected")
    public static void setListener(MaterialAutoCompleteTextView view,
                                   AdapterView.OnItemClickListener onItemSelected) {
        setListener(view, onItemSelected, null);
    }

    @BindingAdapter(value = {"onItemSelected", "selectionAttrChanged"}, requireAll = false)
    public static void setListener(MaterialAutoCompleteTextView view,
                                   AdapterView.OnItemClickListener listener,
                                   InverseBindingListener selectionAttrChanged) {
        if (listener == null && selectionAttrChanged == null) {
            view.setOnItemClickListener(null);
        } else {
            view.setOnItemClickListener((parent, v, position, id) -> {
                if (listener != null) {
                    listener.onItemClick(parent, v, position, id);
                }
                if (selectionAttrChanged != null) {
                    selectionAttrChanged.onChange();
                }
            });
        }
    }
}
