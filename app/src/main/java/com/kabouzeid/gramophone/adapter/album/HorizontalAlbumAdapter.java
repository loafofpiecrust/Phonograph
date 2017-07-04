package com.kabouzeid.gramophone.adapter.album;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;

import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.glide.SongGlideRequest;
import com.kabouzeid.gramophone.helper.HorizontalAdapterHelper;
import com.kabouzeid.gramophone.interfaces.CabHolder;
import com.kabouzeid.gramophone.model.Album;
import com.kabouzeid.gramophone.util.PhonographColorUtil;

import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class HorizontalAlbumAdapter extends AlbumAdapter {
    public static final String TAG = AlbumAdapter.class.getSimpleName();

    public HorizontalAlbumAdapter(@NonNull AppCompatActivity activity, ArrayList<Album> dataSet, boolean usePalette, @Nullable CabHolder cabHolder) {
        super(activity, dataSet, HorizontalAdapterHelper.LAYOUT_RES, usePalette, cabHolder);
    }

    @Override
    protected ViewHolder createViewHolder(View view, int viewType) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        HorizontalAdapterHelper.applyMarginToLayoutParams(activity, params, viewType);
        return new ViewHolder(view);
    }

    @Override
    protected void setColors(int color, ViewHolder holder) {
        if (holder.itemView != null) {
            CardView card=(CardView)holder.itemView;
            card.setCardBackgroundColor(color);
            if (holder.title != null) {
                    holder.title.setTextColor(MaterialValueHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color)));
            }
            if (holder.text != null) {
                    holder.text.setTextColor(MaterialValueHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color)));
            }
        }
    }

    @Override
    protected void loadAlbumCover(Album album, final ViewHolder holder) {
        if (holder.image == null) return;

        SongGlideRequest.Builder.from(activity, album.safeGetFirstSong())
                .withPlaceholder()
                .generatePalette(activity)
                .build(palette -> {
                    int std = ATHUtil.resolveColor(holder.image.getContext(), R.attr.cardBackgroundColor);
                    if (usePalette) {
                        setColors(PhonographColorUtil.getColor(palette, std), holder);
                    } else {
                        setColors(std, holder);
                    }
                })
                .into(holder.image);
//                .into(new PhonographColoredTarget(holder.image) {
//                    @Override
//                    public void onLoadCleared(Drawable placeholder) {
//                        super.onLoadCleared(placeholder);
//                        setColors(getAlbumArtistFooterColor(), holder);
//                    }
//
//                    @Override
//                    public void onColorReady(int color) {
//                        if (usePalette)
//                            setColors(color, holder);
//                        else
//                            setColors(getAlbumArtistFooterColor(), holder);
//                    }
//                });
    }

    @Override
    protected String getAlbumText(Album album) {
        return String.valueOf(album.getYear());
    }

    @Override
    public int getItemViewType(int position) {
        return HorizontalAdapterHelper.getItemViewtype(position, getItemCount());
    }
}
