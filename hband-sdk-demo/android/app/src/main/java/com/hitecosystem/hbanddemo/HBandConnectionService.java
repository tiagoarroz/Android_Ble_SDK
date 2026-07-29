package com.hitecosystem.hbanddemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Mantém o processo BLE ativo enquanto existe uma sessão com a pulseira.
 *
 * <p>O serviço não envia comandos ao SDK: a serialização continua a pertencer
 * à bridge Capacitor. A sua única responsabilidade é reduzir suspensões do
 * processo em segundo plano e tornar a sessão visível ao utilizador.</p>
 */
public class HBandConnectionService extends Service {
    public static final String EXTRA_DEVICE_NAME = "deviceName";
    private static final String CHANNEL_ID = "hband_ble_connection";
    private static final int NOTIFICATION_ID = 9101;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceName = intent == null
            ? getString(R.string.hband_connected_device_default)
            : intent.getStringExtra(EXTRA_DEVICE_NAME);
        if (deviceName == null || deviceName.trim().isEmpty()) {
            deviceName = getString(R.string.hband_connected_device_default);
        }
        startForeground(NOTIFICATION_ID, buildNotification(deviceName));
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            getString(R.string.hband_connection_channel),
            NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.hband_connection_channel_description));
        NotificationManager notifications = getSystemService(NotificationManager.class);
        if (notifications != null) {
            notifications.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String deviceName) {
        Intent openApp = new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
            this,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hband)
            .setContentTitle(getString(R.string.hband_connection_title))
            .setContentText(getString(R.string.hband_connection_device, deviceName))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
}
