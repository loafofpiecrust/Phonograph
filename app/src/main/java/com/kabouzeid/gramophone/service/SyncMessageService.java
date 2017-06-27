package com.kabouzeid.gramophone.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class SyncMessageService extends FirebaseMessagingService {
    public static final String TAG = SyncMessageService.class.getSimpleName();

    public SyncMessageService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        Map<String, String> data = msg.getData();
        if (data == null) {
            // Only process data messages.
            return;
        }

        // TODO: Use the notification bit for when users join or leave the connection.

    }
}
