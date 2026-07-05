package com.zos.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class BackgroundService extends Service {

    private static final String CHANNEL_ID = "z-os-background";
    private static final int NOTIFICATION_ID = 1;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Z-OS Background", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Keeps Z-OS running");
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                // Ignore - not critical
            }
        }

        // Acquire partial wake lock (keeps CPU running when screen off)
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Z-OS::BackgroundService");
                wakeLock.acquire(60 * 60 * 1000L); // 1 hour max, auto-released
            }
        } catch (Exception e) {
            // Ignore - not critical
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Z-OS")
                    .setContentText("Running in background")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build();
            } else {
                notification = new Notification.Builder(this)
                    .setContentTitle("Z-OS")
                    .setContentText("Running in background")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build();
            }
            
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            // If foreground service fails, just run as regular service
        }
        
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
