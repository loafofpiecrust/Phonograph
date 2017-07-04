package com.kabouzeid.gramophone.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.github.florent37.glidepalette.GlidePalette;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCover;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongGlideRequest {
    public static final DiskCacheStrategy DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.RESOURCE;
    public static final int DEFAULT_ERROR_IMAGE = R.drawable.default_album_art;
    public static final int DEFAULT_ANIMATION = android.R.anim.fade_in;
    public static final int DEFAULT_PALETTE_PROFILE = GlidePalette.Profile.VIBRANT;

    public static class Builder {
        final RequestManager requestManager;
        final Song song;
        boolean ignoreMediaStore;
        boolean withFallback = false;

        public static Builder from(@NonNull Context ctx, Song song) {
            return new Builder(Glide.with(ctx), song).checkIgnoreMediaStore(ctx);
        }

        public static Builder from(@NonNull RequestManager req, Song song) {
            return new Builder(req, song);
        }

        private Builder(@NonNull RequestManager requestManager, Song song) {
            this.requestManager = requestManager;
            this.song = song;
        }

        public PaletteBuilder generatePalette(Context context) {
            return new PaletteBuilder(this, context);
        }

        public BitmapBuilder asBitmap() {
            return new BitmapBuilder(this);
        }

        public Builder checkIgnoreMediaStore(Context context) {
            return ignoreMediaStore(PreferenceUtil.getInstance(context).ignoreMediaStoreArtwork());
        }

        public Builder ignoreMediaStore(boolean ignoreMediaStore) {
            this.ignoreMediaStore = ignoreMediaStore;
            return this;
        }

        public Builder withPlaceholder() {
            return withPlaceholder(true);
        }
        public Builder withPlaceholder(boolean val) {
            this.withFallback = val;
            return this;
        }

        public RequestBuilder<Drawable> build() {
            //noinspection unchecked
            RequestOptions opt = new RequestOptions();
            if (withFallback) {
                opt = opt.fallback(DEFAULT_ERROR_IMAGE);
            }
            return createBaseRequest(requestManager, song, ignoreMediaStore)
                    .apply(opt
                        .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                        .error(DEFAULT_ERROR_IMAGE)
                        .fitCenter()
                        .priority(Priority.NORMAL)
                        .signature(createSignature(song)))
                    .transition(new DrawableTransitionOptions().crossFade(250));
        }
    }

    public static class BitmapBuilder {
        private final Builder builder;

        public BitmapBuilder(Builder builder) {
            this.builder = builder;
        }

        public RequestBuilder<Bitmap> build() {
            //noinspection unchecked
            RequestOptions opt = new RequestOptions();
            if (builder.withFallback) {
                opt = opt.placeholder(DEFAULT_ERROR_IMAGE);
            }
            return createBitmapRequest(builder.requestManager, builder.song, builder.ignoreMediaStore)
//                    .asBitmap()
                    .apply(opt
                        .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                        .error(DEFAULT_ERROR_IMAGE)
                        .fitCenter()
//                        .animate(DEFAULT_ANIMATION)
                        .signature(createSignature(builder.song)))
                    .transition(new BitmapTransitionOptions().crossFade(250));
        }
    }

    public static class PaletteBuilder {
        final Context context;
        private final Builder builder;

        public PaletteBuilder(Builder builder, Context context) {
            this.builder = builder;
            this.context = context;
        }

//        public RequestBuilder<Drawable> build() {
//            //noinspection unchecked
//
//            return createBaseRequest(builder.requestManager, builder.song, builder.ignoreMediaStore)
//                    .apply(new RequestOptions()
//                            .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
//                            .error(DEFAULT_ERROR_IMAGE)
//                            .signature(createSignature(builder.song)))
//                    .transition(new DrawableTransitionOptions().crossFade());
//
////                    .asBitmap()
////                    .transcode(new BitmapPaletteTranscoder(context), BitmapPaletteWrapper.class)
//        }

        public RequestBuilder<Drawable> build(GlidePalette.CallBack cb) {
            RequestOptions opt = new RequestOptions();
            if (builder.withFallback) {
                opt = opt.placeholder(DEFAULT_ERROR_IMAGE);
            }
            return createBaseRequest(builder.requestManager, builder.song, builder.ignoreMediaStore)
                    .listener(GlidePalette.with(getSongResource(builder.song, builder.ignoreMediaStore).toString())
                            .use(DEFAULT_PALETTE_PROFILE)
                            .intoCallBack(cb)
                            .crossfade(true, 250))
                    .apply(opt
                            .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                            .error(DEFAULT_ERROR_IMAGE)
                            .signature(createSignature(builder.song)))
                    .transition(new DrawableTransitionOptions().crossFade(250));
        }
    }

    public static RequestBuilder<Drawable> createBaseRequest(RequestManager requestManager, Song song, boolean ignoreMediaStore) {
        return requestManager.load(getSongResource(song, ignoreMediaStore));
    }

    public static RequestBuilder<Bitmap> createBitmapRequest(RequestManager requestManager, Song song, boolean ignoreMediaStore) {
        return requestManager.asBitmap().load(getSongResource(song, ignoreMediaStore));
    }

    public static Object getSongResource(Song song, boolean ignoreMediaStore) {
        if (ignoreMediaStore) {
            return new AudioFileCover(song.data);
        } else {
            return MusicUtil.getMediaStoreAlbumCoverUri(song.albumId);
        }
    }

    public static Key createSignature(Song song) {
        return new MediaStoreSignature("", song.dateModified, 0);
    }
}
