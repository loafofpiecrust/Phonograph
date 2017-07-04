package com.kabouzeid.gramophone.service;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.DoubleConsumer;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kabouzeid.gramophone.loader.SongLoader;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.util.PreferenceUtil;
import com.kabouzeid.gramophone.util.Util;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.client.strategy.RequestStrategyImplSequential;
import com.turn.ttorrent.common.Torrent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.kabouzeid.gramophone.helper.SearchQueryHelper.ALBUM;
import static com.kabouzeid.gramophone.helper.SearchQueryHelper.AND;
import static com.kabouzeid.gramophone.helper.SearchQueryHelper.ARTIST;
import static com.kabouzeid.gramophone.helper.SearchQueryHelper.TITLE;

public class SyncService extends FirebaseMessagingService {
    public static final String TAG = SyncService.class.getSimpleName();
    private static String deviceId = UUID.randomUUID().toString();
    private static Map<Object, BiConsumer<Command, JSONArray>> msgReceivers = new HashMap<>();
    private static List<User> otherUsers = new ArrayList<>();

    public class User {
        String deviceId;
        String displayName;

        User(String device, String name) {
            this.deviceId = device;
            this.displayName = name;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof User)) {
                return false;
            }
            User user = (User) other;
            return user.deviceId.equals(deviceId);
        }
    }

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
        UserLeft,
        QueueUpdate,
        AlbumCheck,
        ArtistCheck,
        AlbumStatus,
        ArtistStatus,
        Ping;

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

    public static void sendMessage(Context ctx, Command cmd, Object... args) {
        sendMessage(ctx, "/topics/" + PreferenceUtil.getInstance(ctx).getSyncChannel(), cmd, args);
    }

    public static void sendMessage(final Context ctx, final String dest, final Command cmd, Object... args) {
        // TODO: Queue the message to send out when we are back online?
        if (!Util.isOnline(ctx)) {
            return;
        }

        Log.d(TAG, "sync: sending message out!");
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
                response -> {
//                    Toast.makeText(ctx, "Bingo Success", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "sync: msg success");
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(ctx, "Sync error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {

            @Override
            public byte[] getBody() throws com.android.volley.AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("to", dest);
                    params.put("data", data);
                    params.put("priority", "high");
                    params.put("time_to_live", 600);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, params.toString());
                return params.toString().getBytes();
            };

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "key=AIzaSyB-Vm0Kspls57a2527vD5oSwiETHr2Y3qs");
                headers.put("Content-Type", getBodyContentType());
                return headers;
            }
        };

        Volley.newRequestQueue(ctx).add(myReq);
    }

    public static void addReceiver(Object key, BiConsumer<Command, JSONArray> f) {
        msgReceivers.put(key, f);
    }
    public static void removeReceiver(Object key) {
        msgReceivers.remove(key);
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
                Log.d(TAG, msg.getFrom() + ": " + cmd + ", took " + t + "ms");
                Log.d(TAG, args.toString(2));
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (BiConsumer<Command, JSONArray> f : msgReceivers.values()) {
                f.accept(cmd, args);
            }

            MusicService music = MusicService.getInstance();
            switch (cmd) {
                case UserJoined: {
                    try {
                        String name = args.getString(0);
                        music.runOnUiThread(() ->
                                Toast.makeText(music, name + " joined", Toast.LENGTH_SHORT).show());
//                        music.addSyncedUser(sender);
                        otherUsers.add(new User(sender, name));

                        // Send the queue, from the previous song to end of the queue.
                        List<Song> q = music.getPlayingQueue();
                        sendMessage(this, sender, Command.QueueUpdate, new JSONArray(Stream.of(q)
                                .skip(music.getPosition())
                                .map(song -> {
                                    JSONObject obj = new JSONObject();
                                    try {
                                        obj.put("title", song.title);
                                        obj.put("artist", song.artistName);
                                        obj.put("album", song.albumName);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    return obj;
                                }).collect(Collectors.toList())));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } break;
                case UserLeft: {
                    try {
                        String name = args.getString(0);
                        music.runOnUiThread(() ->
                                Toast.makeText(music, name + " left", Toast.LENGTH_SHORT).show());

//                        MusicService.getInstance().getSyncedUsers().remove(sender);
                        otherUsers.remove(new User(sender, name));
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
                case Stop:
                    break;
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
                case QueueRemove:
                    break;
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
                case QueueNext:
                    break;
                case QueueAdd: {
                    Log.d(TAG, "Queue add msg");
                    try {
                        // TODO: Multiple songs, as [{"song": "Name", ...}, {}, ...]
                        JSONArray songs = args.getJSONArray(0);
                        for (int i = 0; i < songs.length(); i += 3) {
                            String title = songs.getString(i);
                            String artist = songs.getString(i + 1);
                            String album = songs.getString(i + 2);
                            Log.d(TAG, title + " - " + artist);

                            Song song = SongLoader.getSong(SongLoader.makeSongCursor(
                                    this,
                                    ARTIST + AND + ALBUM + AND + TITLE,
                                    new String[]{artist.toLowerCase(), album.toLowerCase(), title.toLowerCase()}
                            ));

                            if (!song.equals(Song.EMPTY_SONG)) {
                                // Queue the song
                                if (args.length() > 1) {
                                    int pos = args.getInt(1) + music.getPosition();
                                    music.addSong(pos, song);
                                } else {
                                    music.addSong(song);
                                }
                            } else {
                                // We don't have the song!
                                // Request to download it.
                                Log.d(TAG, "Don't have '" + title + "', can I download?");
                                if (args.length() > 1) {
                                    int pos = args.getInt(1);
                                    sendMessage(this, Command.RequestSong, title, artist, album, pos);
                                } else {
                                    sendMessage(this, Command.RequestSong, title, artist, album);
                                }
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


//                        String publicIp = getPublicIp();

                        // server side
                        new Thread(() -> {
                            try {
                                // Make torrent tracker
//                                Tracker tracker = new Tracker(new InetSocketAddress(6969));
//                                tracker.start();
//                                Log.d(TAG, "Made tracker!");

                                java.io.File songFile = new java.io.File(song.data);
                                java.io.File tempFile = new java.io.File(
                                        getDataDir().getAbsolutePath() + "/" + songFile.getName());
                                copy(songFile, tempFile);
//                                songFile.setWritable(true, false);

                                Torrent songTor = Torrent.create(
                                        tempFile,
                                        new URI("udp://tracker.opentrackr.org:1337"), // Use public tracker!
                                        "PhonographSync"
                                );
//                                tracker.announce(songTor);
                                Log.d(TAG, "Seeding '" + songFile.getName() + "'");

                                Client seeder = new Client(
                                        InetAddress.getLocalHost(),
                                        new SharedTorrent(songTor, tempFile.getParentFile(), true)
                                );
                                seeder.share(600);

                                sendMessage(this, SyncService.Command.TransferSong,
                                        title,
                                        artist,
                                        album,
                                        Base64.encodeToString(songTor.getEncoded(), Base64.DEFAULT));

                                seeder.waitForCompletion();
//                                tracker.stop();
                                Log.d(TAG, "Seeder done");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
//                        sendMessage(this, Command.TransferSong, title, artist, album, publicIp, 6969);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } break;
                case TransferSong: {
                    try {
                        String title = args.getString(0);
                        String artist = args.getString(1);
                        String album = args.getString(2);
//                        String ip = args.getString(3);
//                        int port = args.getInt(4);

                        // client side
                        byte[] torData = Base64.decode(args.getString(3), Base64.DEFAULT);

                        SharedTorrent torrent = new SharedTorrent(
                                torData,
                                getDataDir(),
                                false,
                                new RequestStrategyImplSequential());
                        Client client = new Client(InetAddress.getLocalHost(), torrent);

                        client.download();
                        Log.d(TAG, "Torrent downloading");

                        client.addObserver((observable, data1) -> {
                            Client client1 = (Client) observable;
                            float progress = client1.getTorrent().getCompletion();
                            // Do something with progress.
                            Log.d(TAG, "Torrent progress: " + progress);
                        });

//                        client.waitForCompletion();
//                        Log.d(TAG, "Torrent completed!");

//                        MediaScannerConnection.scanFile(
//                                this,
//                                new String[]{file.getAbsolutePath()},
//                                null,
//                                (path, uri) -> {
//                                    // Now, retrieve the song instance!
//                                    Song song = SongLoader.getSong(SongLoader.makeSongFileCursor(
//                                            this,
//                                            file.getPath()
//                                    ));
//                                    // Add to queue where it's supposed to go.
//
//                                });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } break;

                case QueueUpdate: {
                    try {
                        JSONArray otherQueue = args.getJSONArray(0);
                        List<Song> queue = music.getPlayingQueue();
                        int currPos = music.getPosition();
                        int i = 0;
                        for (; i < otherQueue.length(); ++i) {
                            JSONObject otherSong = otherQueue.getJSONObject(i);
                            int relativeIdx = currPos + i;
                            Song song = queue.get(relativeIdx);
                            if (song.title.equals(otherSong.getString("title"))
                                    && song.artistName.equals(otherSong.getString("artist"))
                                    && song.albumName.equals(otherSong.getString("album"))) {
                                // Song matches, do nothing
                            } else {
                                // Song different, replace all from here.
                                break;
                            }
                        }
                        for (; i < otherQueue.length(); ++i) {
                            JSONObject otherSong = otherQueue.getJSONObject(i);
                            int relativeIdx = currPos + i;

                            Song song = SongLoader.getSong(SongLoader.makeSongCursor(
                                    this,
                                    ARTIST + AND + ALBUM + AND + TITLE,
                                    new String[] {
                                        otherSong.getString("artist"),
                                        otherSong.getString("album"),
                                        otherSong.getString("title")
                                    }
                            ));

                            if (relativeIdx <= queue.size() - 1) {
                                queue.set(relativeIdx, song);
                            } else {
                                queue.add(song);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } break;

                case AlbumCheck: try {
                    String album = args.getString(0);
                    Song first = SongLoader.getSong(SongLoader.makeSongCursor(
                            this, ALBUM,
                            new String[]{album.toLowerCase()}
                    ));

                    sendMessage(this, Command.AlbumStatus, !first.equals(Song.EMPTY_SONG));
                } catch (Exception e) {
                    e.printStackTrace();
                } break;

                case ArtistCheck: try {
                    String artist = args.getString(0);
                    Song first = SongLoader.getSong(SongLoader.makeSongCursor(
                            this, ARTIST,
                            new String[]{artist.toLowerCase()}
                    ));

                    sendMessage(this, Command.ArtistStatus, !first.equals(Song.EMPTY_SONG));
                } catch (Exception e) {
                    e.printStackTrace();
                } break;

                case Ping: {
                    try {
                        boolean response = args.getBoolean(0);
                        if (response) {
                            // Ping succeeded, we were the original sender.
                        } else {
                            sendMessage(this, Command.Ping, true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } break;

                case QueueNow:
                    break;
                case QueueRequest:
                    break;
            }

        }
        MusicService.getInstance().setSyncedQueue(true);
        // TODO: Use the notification bit for when users join or leave the connection.
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) { return inetAddress.getHostAddress().toString(); }
                }
            }
        } catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return null;
    }

    public static String getPublicIp() throws Exception {
        URL whatsmyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(whatsmyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }
}
