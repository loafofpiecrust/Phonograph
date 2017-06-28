package com.kabouzeid.gramophone.glide.artistimage;

import android.content.Context;
import android.support.annotation.Nullable;

import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.kabouzeid.gramophone.lastfm.rest.LastFMRestClient;
import com.kabouzeid.gramophone.service.MusicService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class ArtistImageLoader implements ModelLoader<ArtistImage, InputStream> {
    // we need these very low values to make sure our artist image loading calls doesn't block the image loading queue
    private static final int TIMEOUT = 500;

    private Context context;
    private LastFMRestClient lastFMClient;
    private ModelLoader<GlideUrl, InputStream> urlLoader;

    public ArtistImageLoader(Context context, LastFMRestClient lastFMRestClient, ModelLoader<GlideUrl, InputStream> urlLoader) {
        this.context = context;
        this.lastFMClient = lastFMRestClient;
        this.urlLoader = urlLoader;
    }

    public DataFetcher<InputStream> getResourceFetcher(ArtistImage model, int width, int height) {
        return new ArtistImageFetcher(context, lastFMClient, model, urlLoader, width, height);
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(ArtistImage model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model.artistName),
                new ArtistImageFetcher(MusicService.getInstance(), lastFMClient, model, urlLoader, width, height));
    }

    @Override
    public boolean handles(ArtistImage model) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<ArtistImage, InputStream> {
        private LastFMRestClient lastFMClient;
        private OkHttpUrlLoader.Factory okHttpFactory;

        public Factory(Context context) {
            okHttpFactory = new OkHttpUrlLoader.Factory(new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build());
            lastFMClient = new LastFMRestClient(LastFMRestClient.createDefaultOkHttpClientBuilder(context)
                    .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .build());
        }

        @Override
        public ModelLoader<ArtistImage, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new ArtistImageLoader(null, lastFMClient, okHttpFactory.build(multiFactory));
        }

        @Override
        public void teardown() {
            okHttpFactory.teardown();
        }
    }
}

