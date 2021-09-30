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
import androidx.databinding.adapters.ListenerUtil;

import com.google.android.material.tabs.TabLayout;

import edu.mit.ll.nics.android.R;

@BindingMethods({
        @BindingMethod(type = TabLayout.class, attribute = "android:currentTab", method = "setCurrentTab")
})
public class TabLayoutBindingAdapter {

    @InverseBindingAdapter(attribute = "android:currentTab", event = "android:currentTabAttrChanged")
    public static TabLayout.Tab getCurrentTab(TabLayout view) {
        return view.getTabAt(view.getSelectedTabPosition());
    }

    @BindingAdapter("android:currentTab")
    public static void setCurrentTab(TabLayout view, TabLayout.Tab tab) {
        if ((view.getTabAt(view.getSelectedTabPosition())) != tab) {
            view.selectTab(tab);
        }
    }

    @BindingAdapter(value = {"android:onTabSelected", "android:onTabUnselected", "android:onTabReselected", "android:currentTabAttrChanged"}, requireAll=false)
    public static void setListener(TabLayout view, OnTabSelected selected, OnTabUnselected unselected, OnTabReselected reselected, InverseBindingListener attrChange) {
        TabLayout.OnTabSelectedListener newListener;
        if (selected == null && unselected == null && reselected == null && attrChange == null) {
            newListener = null;
        } else {
            newListener = new TabLayout.OnTabSelectedListener() {

                /**
                 * Called when a tab enters the selected state.
                 *
                 * @param tab The tab that was selected
                 */
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (selected != null) {
                        selected.onTabSelected(tab);
                    }
                    attrChange.onChange();
                }

                /**
                 * Called when a tab exits the selected state.
                 *
                 * @param tab The tab that was unselected
                 */
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    if (unselected != null) {
                        unselected.onTabUnselected(tab);
                    }
                    attrChange.onChange();
                }

                /**
                 * Called when a tab that is already selected is chosen again by the user. Some applications may
                 * use this action to return to the top level of a category.
                 *
                 * @param tab The tab that was reselected.
                 */
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    if (reselected != null) {
                        reselected.onTabReselected(tab);
                    }
                    attrChange.onChange();
                }
            };
        }

        TabLayout.OnTabSelectedListener oldListener = ListenerUtil.trackListener(view, newListener,
                R.id.onTabSelectedListener);
        if (oldListener != null) {
            view.removeOnTabSelectedListener(oldListener);
        }
        if (newListener != null) {
            view.addOnTabSelectedListener(newListener);
        }
    }

    public interface OnTabSelected {
        void onTabSelected(TabLayout.Tab v);
    }

    public interface OnTabUnselected {
        void onTabUnselected(TabLayout.Tab v);
    }

    public interface OnTabReselected {
        void onTabReselected(TabLayout.Tab v);
    }
}
