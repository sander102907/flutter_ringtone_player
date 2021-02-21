package io.inway.ringtone.player;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


public class FlutterRingtonePlayerService extends Service {
    public static final String RINGTONE_META_INTENT_EXTRA_KEY = "ringtone-meta";
    private static final String CHANNEL_ID = "flutter-ringtone-player-service-channel";

    private Ringtone ringtone;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final Bundle extras = intent.getExtras();
            if (extras == null) {
                throwInvalidArgumentsException();
            }

            final RingtoneMeta meta = (RingtoneMeta) extras.getSerializable(RINGTONE_META_INTENT_EXTRA_KEY);
            if (meta == null) {
                throwInvalidArgumentsException();
            }

            if (meta.getAsAlarm()) {
                startForeground(meta);
            } else {
                stopForeground(true);
            }

            stopRingtone();
            startRingtone(meta);
        } else {
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopRingtone();
        super.onDestroy();
    }

    private void throwInvalidArgumentsException() {
        throw new IllegalArgumentException("Invalid arguments given");
    }

    private void startForeground(RingtoneMeta ringtoneMeta) {
        createNotificationChannel();

        final AlarmNotificationMeta notificationMeta = ringtoneMeta.getAlarmNotificationMeta();
        validate(notificationMeta);

        int requestID = (int) System.currentTimeMillis();

        final Class<?> activityClass = getActivityClassLaunchedByNotificationIntent(ringtoneMeta);
        final Intent notificationIntent = new Intent(this, activityClass);
        final int iconDrawableResourceId = getResources().getIdentifier(notificationMeta.getIconDrawableResourceName(), "drawable", getPackageName());
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(iconDrawableResourceId)
                .setColor(notificationMeta.getColor())
                .setContentTitle(notificationMeta.getContentTitle())
                .setContentText(notificationMeta.getContentText())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSubText(notificationMeta.getSubText())
                .setFullScreenIntent(pendingIntent, true)
                .build();

        startForeground(1, notification);
    }

    private void stopRingtone() {
        if (ringtone != null) {
            ringtone.stop();
        }
        ringtone = null;
    }

    private void startRingtone(RingtoneMeta meta) {
        ringtone = getConfiguredRingtone(meta);
        ringtone.play();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Foreground service channel",
                    NotificationManager.IMPORTANCE_HIGH);

            serviceChannel.setLightColor(Color.RED);
            serviceChannel.enableLights(true);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            final NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void validate(AlarmNotificationMeta meta) {
        if (meta.getActivityClassLaunchedByIntent() == null || meta.getIconDrawableResourceName() == null) {
            throwInvalidArgumentsException();
        }
    }

    private Class<?> getActivityClassLaunchedByNotificationIntent(RingtoneMeta ringtoneMeta) {
        final String className = ringtoneMeta.getAlarmNotificationMeta().getActivityClassLaunchedByIntent();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class '" + className + "' not found");
        }
    }

    // private void unlockScreen() {
    //     Window window = getApplicationContext().getWindow();
    //     window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    //     window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    //     window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    // }

    private Ringtone getConfiguredRingtone(RingtoneMeta meta) {
        final Uri uri = getRingtoneUri(meta.getKind(), meta.getSoundPath());
        final Ringtone ringtone = RingtoneManager.getRingtone(this, uri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(meta.getLooping());
            if (meta.getVolume() != null) {
                ringtone.setVolume(meta.getVolume());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            ringtone.setAudioAttributes(aa);
        } else {
            ringtone.setStreamType(AudioManager.STREAM_ALARM);
        }

        return ringtone;
    }

    private Uri getRingtoneUri(int kind, String soundPath) {
        int ringtoneType = -1;

        try {
            Uri path = Uri.parse(soundPath);
            RingtoneManager.setActualDefaultRingtoneUri(
               getApplicationContext(), RingtoneManager.TYPE_RINGTONE, path
            );

            return path;

         } catch (Exception e) {
            e.printStackTrace();
         }
         
            switch (kind) {
                case 1:
                    ringtoneType = RingtoneManager.TYPE_ALARM;
                    break;

                case 2:
                    ringtoneType = RingtoneManager.TYPE_NOTIFICATION;
                    break;

                case 3:
                    ringtoneType = RingtoneManager.TYPE_RINGTONE;
                    break;

                default:
                    throwInvalidArgumentsException();
            }
        // }
        return RingtoneManager.getDefaultUri(ringtoneType);
    }
}
