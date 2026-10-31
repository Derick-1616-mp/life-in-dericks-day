package com.electrocat.musicplayer;

import android.Manifest;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "Music",
    permissions = {
        @Permission(alias = "audio", strings = { "android.permission.READ_MEDIA_AUDIO" }),
        @Permission(alias = "storage", strings = { Manifest.permission.READ_EXTERNAL_STORAGE })
    }
)
public class MediaScanPlugin extends Plugin {

    private String permAlias() {
        return Build.VERSION.SDK_INT >= 33 ? "audio" : "storage";
    }

    @PluginMethod
    public void scan(PluginCall call) {
        if (getPermissionState(permAlias()) != PermissionState.GRANTED) {
            requestPermissionForAlias(permAlias(), call, "permDone");
        } else {
            doScan(call);
        }
    }

    @PermissionCallback
    private void permDone(PluginCall call) {
        if (getPermissionState(permAlias()) == PermissionState.GRANTED) {
            doScan(call);
        } else {
            call.reject("denied");
        }
    }

    private void doScan(PluginCall call) {
        Cursor c = null;
        try {
            String[] proj = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME
            };
            String sel = MediaStore.Audio.Media.IS_RINGTONE + " = 0 AND "
                + MediaStore.Audio.Media.IS_NOTIFICATION + " = 0 AND "
                + MediaStore.Audio.Media.IS_ALARM + " = 0";
            c = getContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj, sel, null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC");
            JSArray songs = new JSArray();
            if (c != null) {
                while (c.moveToNext()) {
                    String path = c.getString(5);
                    if (path == null) continue;
                    String artist = c.getString(2);
                    if (artist != null && artist.equals("<unknown>")) artist = "";
                    JSObject o = new JSObject();
                    o.put("id", c.getLong(0));
                    o.put("title", c.getString(1));
                    o.put("artist", artist == null ? "" : artist);
                    o.put("duration", c.getLong(3));
                    o.put("size", c.getLong(4));
                    o.put("path", path);
                    o.put("name", c.getString(6));
                    songs.put(o);
                }
            }
            JSObject res = new JSObject();
            res.put("songs", songs);
            call.resolve(res);
        } catch (Exception e) {
            call.reject(e.getMessage() == null ? "scan failed" : e.getMessage());
        } finally {
            if (c != null) c.close();
        }
    }
}
