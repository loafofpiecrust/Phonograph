package com.kabouzeid.gramophone.service;

import android.app.Activity;
import android.content.Context;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kabouzeid.gramophone.loader.SongLoader;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static com.kabouzeid.gramophone.helper.SearchQueryHelper.ALBUM;
import static com.kabouzeid.gramophone.helper.SearchQueryHelper.AND;
import static com.kabouzeid.gramophone.helper.SearchQueryHelper.ARTIST;
import static com.kabouzeid.gramophone.helper.SearchQueryHelper.TITLE;

public class SyncService extends FirebaseMessagingService {
    public static final String TAG = SyncService.class.getSimpleName();
    private static int msgId = 0;
    private static long responseTime = 0;
    public static String deviceId = "nobody";

    public enum Command {
        Play,
        Pause,
        Stop,
        Next,
        Previous,
        Seek,
        QueueRemove,
        QueueSetPos,
        QueueNext,
        QueueAdd,
        QueueNow,
        QueueMove,
        QueueClear,
        QueueRequest,
        TransferSong,
        RequestSong,
        UserJoined,
        UserLeft;

        public static Command parse(String s) {
            return Command.values()[Integer.parseInt(s)];
        }

        public String toString() {
            return String.valueOf(this.ordinal());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void sendMessage(final Context ctx, final Command cmd, Object... args) {
        Log.d(TAG, "sync: sending message out!");
        final String instanceIdToken = "/topics/" + PreferenceUtil.getInstance(ctx).getSyncChannel();
        final String url = "https://fcm.googleapis.com/fcm/send";
        final JSONObject data = new JSONObject();
        try {
            data.put("cmd", cmd.ordinal());
            data.put("args", new JSONArray(args));
            data.put("sender", deviceId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        StringRequest myReq = new StringRequest(Request.Method.POST,url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(ctx, "Bingo Success", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "sync: msg success");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String msg = error.getMessage();
                        error.printStackTrace();
                        Toast.makeText(ctx, "Oops error", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "sync: msg failure!");
                    }
                }) {

            @Override
            public byte[] getBody() throws com.android.volley.AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("to", instanceIdToken);
                    params.put("data", data);
                    params.put("priority", "high");
                    params.put("time_to_live", 600);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return params.toString().getBytes();
            };

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "key=AIzaSyB-Vm0Kspls57a2527vD5oSwiETHr2Y3qs");
                headers.put("Content-Type", getBodyContentType());
                return headers;
            }
        };

        responseTime = System.currentTimeMillis();
        Volley.newRequestQueue(ctx).add(myReq);
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        Map<String, String> data = msg.getData();
        String sender = data.get("sender");
        if (data == null || (sender != null && sender.equals(deviceId))) {
            // Only process data messages.
            return;
        }
        long t = msg.getSentTime() - System.currentTimeMillis();

        MusicService.getInstance().setSyncedQueue(false);
        if (data.containsKey("cmd")) {
            Command cmd = Command.parse(data.get("cmd"));
            JSONArray args = null;
            try {
                args = new JSONArray(data.get("args"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, msg.getFrom() + ": " + cmd + ", took " + t + "ms");
            MusicService music = MusicService.getInstance();
            switch (cmd) {
                case UserJoined: {
                    try {
                        String name = args.getString(0);
                        music.runOnUiThread(() ->
                                Toast.makeText(music, name + " joined", Toast.LENGTH_SHORT).show());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } break;
                case UserLeft: {
                    try {
                        String name = args.getString(0);
                        music.runOnUiThread(() ->
                                Toast.makeText(music, name + " left", Toast.LENGTH_SHORT).show());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } break;
                case Play: {
                    music.play();
                } break;
                case Pause: {
                    music.pause();
                } break;
                case Next: {
                    music.playNextSong(true);
                } break;
                case Previous: {
                    music.playPreviousSong(true);
                } break;
                case Seek: try {
                    int pos = args.getInt(0);
                    music.seek(pos);
                } catch (JSONException e) {
                    e.printStackTrace();
                } break;
                case QueueSetPos: try {
                    int pos = args.getInt(0) + music.getPosition();
                    music.playSongAt(pos);
                } catch (JSONException e) {
                    e.printStackTrace();
                } break;
                case QueueClear: {
                    music.clearQueue();
                } break;
                case QueueMove: try {
                    int from = args.getInt(0) + music.getPosition();
                    int to = args.getInt(1) + music.getPosition();
                    music.moveSong(from, to);
                } catch (JSONException e) {
                    e.printStackTrace();
                } break;
                case QueueAdd: {
                    Log.d(TAG, "Queue add msg");
                    try {
                        // TODO: Multiple songs, as [{"song": "Name", ...}, {}, ...]
                        String title = args.getString(0);
                        String artist = args.getString(1);
                        String album = args.getString(2);

                        Song song = SongLoader.getSong(SongLoader.makeSongCursor(
                                this,
                                ARTIST + AND + ALBUM + AND + TITLE,
                                new String[]{artist.toLowerCase(), album.toLowerCase(), title.toLowerCase()}
                        ));

                        if (song != Song.EMPTY_SONG) {
                            // Queue the song
                            if (args.length() > 3) {
                                int pos = args.getInt(3) + music.getPosition();
                                music.addSong(pos, song);
                            } else {
                                music.addSong(song);
                            }
                        } else {
                            // We don't have the song!
                            // Request to download it.
                            if (args.length() > 3) {
                                int pos = args.getInt(3);
                                sendMessage(this, Command.RequestSong, title, artist, album, pos);
                            } else {
                                sendMessage(this, Command.RequestSong, title, artist, album);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } break;
                case RequestSong: {
                    try {
                        String title = args.getString(0);
                        String artist = args.getString(1);
                        String album = args.getString(2);
                        int pos = -1;
                        if (args.length() > 3) {
                            pos = args.getInt(3);
                        }

                        Song song = SongLoader.getSong(SongLoader.makeSongCursor(
                                this,
                                ARTIST + AND + ALBUM + AND + TITLE,
                                new String[]{artist.toLowerCase(), album.toLowerCase(), title.toLowerCase()}
                        ));


                        // Host a TCP server to transfer the file.
                        ServerSocket server = new ServerSocket(8080);

                        Log.d(TAG, server.getInetAddress().getHostName());
                        sendMessage(this, Command.TransferSong, title, artist, album, server.getInetAddress().getHostName(), 8080);
                        server.setSoTimeout(10000);
                        while (!server.isClosed()) {
                            final Socket client = server.accept();
                            if (!client.isConnected()) {
                                server.close();
                                break;
                            }
                            OutputStream out = new BufferedOutputStream(client.getOutputStream());
//                        InputStream in = client.getInputStream();
                            new Thread(() -> {
                                File file = new File(MusicUtil.getSongFileUri(song.id).getPath());
                                try {
                                    out.write((int) file.length());
                                    FileReader reader = new FileReader(file);
                                    while (reader.ready()) {
                                        out.write(reader.read());
                                    }
                                    out.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } break;
                case TransferSong: {
                    try {
                        String title = args.getString(0);
                        String artist = args.getString(1);
                        String album = args.getString(2);
                        String ip = args.getString(3);
                        int port = args.getInt(4);

                        Socket client = new Socket();
                        client.connect(new InetSocketAddress(ip, port));

                        File file = new File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                                title + " - " + artist + " - " + album
                        );

                        InputStream in = new BufferedInputStream(client.getInputStream());
//                    OutputStream out = client.getOutputStream();
                        FileWriter writer = new FileWriter(file);
                        int size = in.read();
                        for (int i = 0; i < size; i += 4) {
                            writer.write(in.read());
                        }
                        in.close();
                        client.close();


                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } break;
            }

        }
        MusicService.getInstance().setSyncedQueue(true);

        // TODO: Use the notification bit for when users join or leave the connection.
    }
}
