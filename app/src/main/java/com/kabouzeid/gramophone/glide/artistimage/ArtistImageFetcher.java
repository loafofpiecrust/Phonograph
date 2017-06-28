package com.kabouzeid.gramophone.glide.artistimage;

import android.content.Context;
import android.renderscript.ScriptGroup;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.kabouzeid.gramophone.lastfm.rest.LastFMRestClient;
import com.kabouzeid.gramophone.lastfm.rest.model.LastFmArtist;
import com.kabouzeid.gramophone.util.LastFMUtil;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.Util;

import java.io.IOException;
import java.io.InputStream;

import retrofit2.Response;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistImageFetcher implements DataFetcher<InputStream> {
    public static final String TAG = ArtistImageFetcher.class.getSimpleName();
    private Context context;
    private final LastFMRestClient lastFMRestClient;
    private final ArtistImage model;
    private ModelLoader<GlideUrl, InputStream> urlLoader;
    private final int width;
    private final int height;
    private volatile boolean isCancelled;
    private DataFetcher<InputStream> urlFetcher;

    public ArtistImageFetcher(Context context, LastFMRestClient lastFMRestClient, ArtistImage model, ModelLoader<GlideUrl, InputStream> urlLoader, int width, int height) {
        this.context = context;
        this.lastFMRestClient = lastFMRestClient;
        this.model = model;
        this.urlLoader = urlLoader;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        // makes sure we never ever return null here
        return String.valueOf(model.artistName);
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        if (!MusicUtil.isArtistNameUnknown(model.artistName) && Util.isAllowedToAutoDownload(context)) {
            Response<LastFmArtist> response = null;
            try {
                response = lastFMRestClient.getApiService().getArtistInfo(model.artistName, model.skipOkHttpCache ? "no-cache" : null).execute();
            } catch (IOException e) {
                callback.onLoadFailed(e);
                return;
            }

            if (!response.isSuccessful()) {
                callback.onLoadFailed(new IOException("Request failed with code: " + response.code()));
                return;
            }

            LastFmArtist lastFmArtist = response.body();

            if (isCancelled) {
                callback.onLoadFailed(null);
                return;
            }

            GlideUrl url = new GlideUrl(LastFMUtil.getLargestArtistImageUrl(lastFmArtist.getArtist().getImage()));
            urlFetcher = urlLoader.buildLoadData(url, width, height, new Options()).fetcher;

            urlFetcher.loadData(priority, callback);
        }
        callback.onLoadFailed(null);
    }

    @Override
    public void cleanup() {
        if (urlFetcher != null) {
            urlFetcher.cleanup();
        }
    }

    @Override
    public void cancel() {
        isCancelled = true;
        if (urlFetcher != null) {
            urlFetcher.cancel();
        }
    }

    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @Override
    public DataSource getDataSource() {
        return urlFetcher.getDataSource();
    }
}
