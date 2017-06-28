package com.kabouzeid.gramophone.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.gramophone.R;

public class ColorUtil {
    public static int getDefaultFooterColor(@NonNull Context ctx) {
        return ATHUtil.resolveColor(ctx, R.attr.defaultFooterColor);
    }

    public static int getAlbumArtistFooterColor(@NonNull Context ctx) {
        return ATHUtil.resolveColor(ctx, R.attr.cardBackgroundColor);
    }
}
