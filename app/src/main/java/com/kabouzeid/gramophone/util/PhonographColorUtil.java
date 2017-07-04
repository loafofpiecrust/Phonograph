package com.kabouzeid.gramophone.util;

import android.graphics.Bitmap;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import com.kabouzeid.appthemehelper.util.ColorUtil;

import java.util.Comparator;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PhonographColorUtil {

    @Nullable
    public static Palette generatePalette(Bitmap bitmap) {
        if (bitmap == null) return null;
        return Palette.from(bitmap).generate();
    }

    @ColorInt
    public static int getColor(@Nullable Palette palette, int fallback) {
        if (palette != null) {
            if (!palette.getSwatches().isEmpty()) {
                Palette.Swatch res = palette.getVibrantSwatch();
                if (res == null || ColorUtil.isColorLight(res.getRgb())) {
                    res = palette.getDarkVibrantSwatch();
                }
                if (res == null || ColorUtil.isColorLight(res.getRgb())) {
                    for (Palette.Swatch swatch : palette.getSwatches()) {
                        int rgb = swatch.getRgb();
                        if (!ColorUtil.isColorLight(rgb) && (res == null || swatch.getPopulation() > res.getPopulation())) {
                            res = swatch;
                        }
                    }
                }
                if (res == null) {
                    return fallback;
                } else {
                    return res.getRgb();
                }
//                return Collections.max(palette.getSwatches(), SwatchComparator.getInstance()).getRgb();
            }
        }
        return fallback;
    }

    private static class SwatchComparator implements Comparator<Palette.Swatch> {
        private static SwatchComparator sInstance;

        static SwatchComparator getInstance() {
            if (sInstance == null) {
                sInstance = new SwatchComparator();
            }
            return sInstance;
        }

        @Override
        public int compare(Palette.Swatch lhs, Palette.Swatch rhs) {
            return lhs.getPopulation() - rhs.getPopulation();
        }
    }

    @ColorInt
    public static int shiftBackgroundColorForLightText(@ColorInt int backgroundColor) {
        while (ColorUtil.isColorLight(backgroundColor)) {
            backgroundColor = ColorUtil.darkenColor(backgroundColor);
        }
        return backgroundColor;
    }

    @ColorInt
    public static int shiftBackgroundColorForDarkText(@ColorInt int backgroundColor) {
        while (!ColorUtil.isColorLight(backgroundColor)) {
            backgroundColor = ColorUtil.lightenColor(backgroundColor);
        }
        return backgroundColor;
    }
}
