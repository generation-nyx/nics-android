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

import android.graphics.Color;

public class ColorUtils {

    public static final int[] WHITE = new int[]{255, 255, 255, 255};
    public static final int[] BLACK = new int[]{255, 0, 0, 0};

    public static int[] parseRGBAColorArray(String rgb) {
        return parseRGBAColorArray(rgb, 1);
    }

    public static int[] parseRGBAColorArray(String rgb, double opacity) {
        return colorToIntArray(parseRGBAColor(rgb, opacity));
    }

    public static int parseRGBAColor(String rgb, double opacity) {
        if (rgb == null || rgb.length() == 0) {
            rgb = "#FFFFFF";
        }

        int color;
        try {
            color = Color.parseColor(rgb);
        } catch (Exception e) {
            color = Color.WHITE;
        }
        return androidx.core.graphics.ColorUtils.setAlphaComponent(color, (int) (opacity * 255));
    }

    public static int colorArrayToInt(int[] colors) {
        return Color.argb(colors[0], colors[1], colors[2], colors[3]);
    }

    public static int[] colorStringToIntArray(String color, double opacity) {
        return colorToIntArray(parseRGBAColor(color, opacity));
    }

    public static int[] colorStringToIntArray(String color) {
        return colorStringToIntArray(color, 1);
    }

    public static int[] colorToIntArray(int color) {
        return new int[]{Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color)};
    }

    public static int[] strokeToFillColors(int[] color) {
        // fill color is a 40% transparent stroke color
        return new int[]{(int) (color[0] * 0.4), color[1], color[2], color[3]};
    }

    public static int strokeToFillColor(int color) {
        // fill color is a 40% transparent stroke color
        return androidx.core.graphics.ColorUtils.setAlphaComponent(color, (int) (0.4 * 255));
    }

    public static String colorToHexString(int[] color) {
        if (color != null) {
            return String.format("#%06X", 0xFFFFFF & Color.argb(color[0], color[1], color[2], color[3]));
        } else {
            return StringUtils.EMPTY;
        }
    }
}
