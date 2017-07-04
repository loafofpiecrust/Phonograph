package com.kabouzeid.gramophone.glide;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.GlideModule;
import com.kabouzeid.gramophone.glide.artistimage.ArtistImage;
import com.kabouzeid.gramophone.glide.artistimage.ArtistImageLoader;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCover;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCoverLoader;

import java.io.InputStream;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PhonographGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Registry registry) {
        registry.append(AudioFileCover.class, InputStream.class, new AudioFileCoverLoader.Factory());
        registry.append(ArtistImage.class, InputStream.class, new ArtistImageLoader.Factory(context));
    }
}
