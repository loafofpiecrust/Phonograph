package com.kabouzeid.gramophone.glide.audiocover;


import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class AudioFileCoverLoader implements ModelLoader<AudioFileCover, InputStream> {
    @Override
    public LoadData<InputStream> buildLoadData(AudioFileCover model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model), new AudioFileCoverFetcher(model));
    }

    @Override
    public boolean handles(AudioFileCover o) {
        return true;
    }


    public static class Factory implements ModelLoaderFactory<AudioFileCover, InputStream> {
        @Override
        public ModelLoader<AudioFileCover, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new AudioFileCoverLoader();
        }

        @Override
        public void teardown() {
        }
    }
}

