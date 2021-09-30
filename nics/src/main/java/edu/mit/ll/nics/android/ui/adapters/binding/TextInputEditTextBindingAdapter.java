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

import android.text.Editable;
import android.text.TextWatcher;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.adapters.ListenerUtil;

import com.google.android.material.textfield.TextInputEditText;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTrigger;
import edu.mit.ll.nics.android.utils.livedata.LiveDataTriggerEvent;

import static edu.mit.ll.nics.android.utils.StringUtils.EMPTY;

@BindingMethods({
        @BindingMethod(type = TextInputEditText.class, attribute = "text", method = "setTextValue"),
        @BindingMethod(type = TextInputEditText.class, attribute = "textEvent", method = "setTextEvent")
})
public class TextInputEditTextBindingAdapter {

    @InverseBindingAdapter(attribute = "text", event = "textValueAttrChanged")
    public static String getTextValue(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : EMPTY;
    }

    @BindingAdapter("text")
    public static void setTextValue(TextInputEditText editText, String text) {
        if (text == null && editText.getText() != null && !editText.getText().toString().isEmpty()) {
            editText.setText(EMPTY);
        } else if (text != null && editText.getText() != null && !editText.getText().toString().equals(text)) {
            editText.setText(text);
        }
    }

    @BindingAdapter("onTextChanged")
    public static void setListener(TextInputEditText view, OnTextChanged onTextChanged) {
        setTextWatcher(view, null, onTextChanged, null, null);
    }

   @BindingAdapter(value = {"beforeTextChanged", "onTextChanged", "afterTextChanged",
           "textValueAttrChanged"}, requireAll = false)
    public static void setTextWatcher(TextInputEditText view, BeforeTextChanged before,
                                      OnTextChanged on, AfterTextChanged after,
                                      InverseBindingListener textAttrChanged) {
        TextWatcher newValue;
        if (before == null && after == null && on == null && textAttrChanged == null) {
            newValue = null;
        } else {
            newValue = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if (before != null) {
                        before.beforeTextChanged(s, start, count, after);
                    }
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (on != null) {
                        on.onTextChanged(s, start, before, count);
                    }
                    if (textAttrChanged != null) {
                        textAttrChanged.onChange();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (after != null) {
                        after.afterTextChanged(s);
                    }
                }
            };
        }
        TextWatcher oldValue = ListenerUtil.trackListener(view, newValue, R.id.textWatcher);
        if (oldValue != null) {
            view.removeTextChangedListener(oldValue);
        }
        if (newValue != null) {
            view.addTextChangedListener(newValue);
        }
    }
    public interface AfterTextChanged {
        void afterTextChanged(Editable s);
    }

    public interface BeforeTextChanged {
        void beforeTextChanged(CharSequence s, int start, int count, int after);
    }

    public interface OnTextChanged {
        void onTextChanged(CharSequence s, int start, int before, int count);
    }

    @InverseBindingAdapter(attribute = "textEvent", event = "textEventAttrChanged")
    public static LiveDataTriggerEvent<String> getTextEvent(TextInputEditText editText) {
        LiveDataTrigger trigger = (LiveDataTrigger) editText.getTag();

        if (trigger == null) {
            trigger = LiveDataTrigger.INPUT;
        }

        // Reset the tag back to input.
        editText.setTag(LiveDataTrigger.INPUT);

        return new LiveDataTriggerEvent<>(editText.getText() != null ? editText.getText().toString() : null, trigger);
    }

    @BindingAdapter("textEvent")
    public static void setTextEvent(TextInputEditText editText, LiveDataTriggerEvent<String> value) {
        if (value == null && editText.getText() != null && !editText.getText().toString().isEmpty()) {
            editText.setText(EMPTY);
        } else if (value != null && editText.getText() != null && !editText.getText().toString().equals(value.getData())) {
            // Set the tag to whatever triggered this event, then reset the tag to INPUT so that it defaults back when the user types something.
            editText.setTag(value.getTrigger());
            editText.setText(value.getData());
        }
    }

    @BindingAdapter(value = {"beforeTextChanged", "onTextChanged", "afterTextChanged", "textEventAttrChanged"}, requireAll = false)
    public static void setTextEventWatcher(TextInputEditText view, BeforeTextChanged before,
                                      OnTextChanged on, AfterTextChanged after,
                                      InverseBindingListener textAttrChanged) {
        TextWatcher newValue;
        if (before == null && after == null && on == null && textAttrChanged == null) {
            newValue = null;
        } else {
            newValue = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if (before != null) {
                        before.beforeTextChanged(s, start, count, after);
                    }
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (on != null) {
                        on.onTextChanged(s, start, before, count);
                    }
                    if (textAttrChanged != null) {
                        textAttrChanged.onChange();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (after != null) {
                        after.afterTextChanged(s);
                    }
                }
            };
        }
        TextWatcher oldValue = ListenerUtil.trackListener(view, newValue, R.id.textWatcher);
        if (oldValue != null) {
            view.removeTextChangedListener(oldValue);
        }
        if (newValue != null) {
            view.addTextChangedListener(newValue);
        }
    }
}
