package com.kabouzeid.gramophone.helper;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.kabouzeid.gramophone.loader.SongLoader;
import com.kabouzeid.gramophone.model.Song;

import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SearchQueryHelper {
    public static final String TITLE = "lower(" + MediaStore.Audio.AudioColumns.TITLE + ") = ?";
    public static final String ALBUM = "lower(" + MediaStore.Audio.AudioColumns.ALBUM + ") = ?";
    public static final String ARTIST = "lower(" + MediaStore.Audio.AudioColumns.ARTIST + ") = ?";
    public static final String AND = " AND ";

    @NonNull
    public static ArrayList<Song> getSongs(@NonNull final Context context, @NonNull final Bundle extras) {
        final String query = extras.getString(SearchManager.QUERY, null);
        final String artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null);
        final String albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null);
        final String titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null);

        ArrayList<Song> songs = new ArrayList<>();

        if (artistName != null && albumName != null && titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST + AND + ALBUM + AND + TITLE, new String[]{artistName.toLowerCase(), albumName.toLowerCase(), titleName.toLowerCase()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        if (artistName != null && titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST + AND + TITLE, new String[]{artistName.toLowerCase(), titleName.toLowerCase()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        if (albumName != null && titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ALBUM + AND + TITLE, new String[]{albumName.toLowerCase(), titleName.toLowerCase()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        if (artistName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST, new String[]{artistName.toLowerCase()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        if (albumName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ALBUM, new String[]{albumName.toLowerCase()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        if (titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, TITLE, new String[]{titleName.toLowerCase()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }


        songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST, new String[]{query.toLowerCase()}));
        if (!songs.isEmpty()) {
            return songs;
        }

        songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ALBUM, new String[]{query.toLowerCase()}));
        if (!songs.isEmpty()) {
            return songs;
        }

        songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, TITLE, new String[]{query.toLowerCase()}));
        if (!songs.isEmpty()) {
            return songs;
        }

        return SongLoader.getSongs(context, query);
    }
}
