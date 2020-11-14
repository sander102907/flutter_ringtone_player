package io.inway.ringtone.player;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.database.Cursor;
import java.util.Map;
import java.util.HashMap;
import android.view.WindowManager;
import android.view.Window;
import android.app.*;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


/**
 * FlutterRingtonePlayerPlugin
 */
public class FlutterRingtonePlayerPlugin implements MethodCallHandler {
    private final Context context;

    public FlutterRingtonePlayerPlugin(Context context) {
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_ringtone_player");
        channel.setMethodCallHandler(new FlutterRingtonePlayerPlugin(registrar.context()));
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        try {
            final String methodName = call.method;

            if (methodName.equals("play")) {
                final RingtoneMeta meta = createRingtoneMeta(call);
                startRingtone(meta);
                result.success(null);
            } else if (methodName.equals("stop")) {
                stopRingtone();
                result.success(null);
            } else if (methodName.equals("setAlarmSound")) {
                setAlarmSound(getMethodCallArgument(call, "soundPath", String.class));
                result.success(null);
            } else if (methodName.equals("getDefaultAlarmSound")) {
                result.success(getDefaultAlarmSound());
            }
            else if (methodName.equals("getAlarmSounds")) {
                result.success(getAlarmSounds());
            } 
            else if (methodName.equals("checkSystemWritePermission")) {
                Class<?> activityClass = Class.forName(getMethodCallArgument(call, "alarmNotificationMeta", AlarmNotificationMeta.class).getActivityClassLaunchedByIntent());
                result.success(checkSystemWritePermission(activityClass));
            }
        } catch (Exception e) {
            result.error("Exception", e.getMessage(), null);
        }
    }

    private RingtoneMeta createRingtoneMeta(MethodCall call) {
        if (!call.hasArgument("android")) {
            throw new IllegalArgumentException("android argument is missing");
        }

        final RingtoneMeta meta = new RingtoneMeta();
        meta.setKind(getMethodCallArgument(call, "android", Integer.class));
        meta.setLooping(getMethodCallArgument(call, "looping", Boolean.class));
        meta.setAsAlarm(getMethodCallArgument(call, "asAlarm", Boolean.class));
        meta.setSoundPath(getMethodCallArgument(call, "soundPath", String.class));
        final Double volume = getMethodCallArgument(call, "volume", Double.class);
        if (volume != null) {
            meta.setVolume(volume.floatValue());
        }

        if (meta.getAsAlarm()) {
            final String alarmNotificationMetaKey = "alarmNotificationMeta";

            if (call.hasArgument(alarmNotificationMetaKey)) {
                final Map<String, Object> notificationMetaValues = getMethodCallArgument(call, alarmNotificationMetaKey, Map.class);
                final AlarmNotificationMeta notificationMeta = new AlarmNotificationMeta(notificationMetaValues);
                meta.setAlarmNotificationMeta(notificationMeta);
            } else {
                throw new IllegalArgumentException("if asAlarm=true you have to deliver '" + alarmNotificationMetaKey + "'");
            }
        }

        return meta;
    }

    private void startRingtone(RingtoneMeta meta) {
        final Intent intent = createServiceIntent();
        intent.putExtra(FlutterRingtonePlayerService.RINGTONE_META_INTENT_EXTRA_KEY, meta);

        if (meta.getAsAlarm()) {
            ContextCompat.startForegroundService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    private void stopRingtone() {
        final Intent intent = createServiceIntent();
        context.stopService(intent);
    }

    private <ArgumentType> ArgumentType getMethodCallArgument(MethodCall call, String key, Class<ArgumentType> argumentTypeClass) {
        return call.argument(key);
    }

    private Intent createServiceIntent() {
        return new Intent(context, FlutterRingtonePlayerService.class);
    }

    private void setAlarmSound(String soundPath) {
        try {
            Uri path = Uri.parse(soundPath);
            RingtoneManager.setActualDefaultRingtoneUri(
                context, RingtoneManager.TYPE_ALARM, path
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDefaultAlarmSound() {
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Ringtone ringtoneAlarm = RingtoneManager.getRingtone(context, alarm);
        return ringtoneAlarm.getTitle(context);
    }

    private HashMap<String, String> getAlarmSounds() {
        RingtoneManager ringtoneManager = new RingtoneManager(context);
        Cursor tonesCursor = ringtoneManager.getCursor();
        HashMap sounds = new HashMap<String, String>();
        if (tonesCursor.moveToFirst()) {
            do { 
                int id = tonesCursor.getInt(RingtoneManager.ID_COLUMN_INDEX);
                String uriString = tonesCursor.getString(RingtoneManager.URI_COLUMN_INDEX);
                Uri uri = Uri.parse(uriString + "/" + id);
                String name = tonesCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                sounds.put(name, uriString + '/' + id);
            } while (tonesCursor.moveToNext()); 
        }
        return sounds;
    }

    private boolean checkSystemWritePermission(Class<?> activityClass) {
        boolean retVal = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(context);
            Log.d("-", "Can Write Settings: " + retVal);
            if(retVal){
                Toast.makeText(context, "Write allowed :-)", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, "Write not allowed :-(", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, activityClass);
                System.out.println(Uri.parse("package:" + context.getPackageName()));
                System.out.println(context.getPackageName());
                // intent.setData();
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            }   
        }
        return retVal;
    }
}
