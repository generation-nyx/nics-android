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
package edu.mit.ll.nics.android.utils;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.databinding.InverseMethod;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

import static edu.mit.ll.nics.android.utils.constants.NICS.DEBUG;

public class Converter {

    @InverseMethod("toDouble")
    public static String toString(TextInputEditText view, Double oldValue, Double value) {
        NumberFormat numberFormat = getNumberFormat(view);

        if (value != null) {
            try {
                Number number = numberFormat.parse(view.getText().toString());

                if (number != null) {
                    double parsed = number.doubleValue();
                    if (parsed == value) {
                        return view.getText().toString();
                    }
                }
            } catch (ParseException e) {
                Timber.tag(DEBUG).e("Old Value is broken for text view.");
            }

            return numberFormat.format(value);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static Double toDouble(TextInputEditText view, Double oldValue, String value) {
        NumberFormat numberFormat = getNumberFormat(view);

        if (value.isEmpty()) {
            return null;
        } else {
            try {
                Number number = numberFormat.parse(value);
                if (number != null) {
                    return number.doubleValue();
                }
            } catch (ParseException e) {
                Resources resources = view.getResources();
                String errStr = "Invalid Number Formatting";
                view.setError(errStr);
            }

            return oldValue;
        }
    }

    private static NumberFormat getNumberFormat(View view) {
        Resources resources = view.getResources();
        Locale locale = resources.getConfiguration().locale;
        NumberFormat format = NumberFormat.getNumberInstance(locale);

        if (format instanceof DecimalFormat) {
            format.setGroupingUsed(false);
        }

        format.setMaximumFractionDigits(6);

        return format;
    }

    @InverseMethod("toDateFull")
    public static String timestampToDateFull(TextView view, double oldValue, double value) {
        return timestampToDate(view, oldValue, value, DateFormat.FULL);
    }

    public static double toDateFull(TextView view, double oldValue, String value) {
        return toDate(view, oldValue, value,  DateFormat.FULL);
    }

    @InverseMethod("toDateMedium")
    public static String timestampToDateMedium(TextView view, double oldValue, double value) {
        return timestampToDate(view, oldValue, value, DateFormat.MEDIUM);
    }

    public static double toDateMedium(TextView view, double oldValue, String value) {
        return toDate(view, oldValue, value,  DateFormat.MEDIUM);
    }

    private static String timestampToDate(TextView view, double oldValue, double value, int format) {
        DateFormat dateFormat = getDateFormat(view, format);

        try {
            Date date = dateFormat.parse(view.getText().toString());

            if (date != null) {
                double parsed = date.getTime();
                if (parsed == value) {
                    return view.getText().toString();
                }
            }
        } catch (ParseException e) {
            Timber.tag(DEBUG).e("Old Value is broken for text view.");
        }

        return dateFormat.format(value);
    }

    private static double toDate(TextView view, double oldValue, String value, int format) {
        DateFormat dateFormat = getDateFormat(view, format);

        try {
            Date date = dateFormat.parse(value);
            if (date != null) {
                return date.getTime();
            }
        } catch (ParseException e) {
            String errStr = "Invalid Date Formatting";
            view.setError(errStr);
        }

        return oldValue;
    }

    private static DateFormat getDateFormat(View view, int dateFormat) {
        Resources resources = view.getResources();
        Locale locale;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locale = resources.getConfiguration().getLocales().get(0);
        } else {
            locale = resources.getConfiguration().locale;
        }
        return DateFormat.getDateTimeInstance(dateFormat, dateFormat, locale);
    }
}
