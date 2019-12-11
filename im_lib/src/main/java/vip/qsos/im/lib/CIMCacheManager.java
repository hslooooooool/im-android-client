package vip.qsos.im.lib;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

class CIMCacheManager {

    public static final String CIM_CONFIG_INFO = "CIM_CONFIG_INFO";

    public static final String KEY_ACCOUNT = "KEY_ACCOUNT";

    public static final String KEY_DEVICE_ID = "KEY_DEVICE_ID";

    public static final String KEY_MANUAL_STOP = "KEY_MANUAL_STOP";

    public static final String KEY_CIM_DESTROYED = "KEY_CIM_DESTROYED";

    public static final String KEY_CIM_SERVIER_HOST = "KEY_CIM_SERVIER_HOST";

    public static final String KEY_CIM_SERVIER_PORT = "KEY_CIM_SERVIER_PORT";

    public static final String KEY_CIM_CONNECTION_STATE = "KEY_CIM_CONNECTION_STATE";

    public static final String CONTENT_URI = "content://%s.cim.provider";


    public static void remove(Context context, String key) {
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(Uri.parse(String.format(CONTENT_URI, context.getPackageName())), key, null);
    }

    public static void putString(Context context, String key, String value) {

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("value", value);
        values.put("key", key);
        resolver.insert(Uri.parse(String.format(CONTENT_URI, context.getPackageName())), values);

    }

    public static String getString(Context context, String key) {
        String value = null;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Uri.parse(String.format(CONTENT_URI, context.getPackageName())), new String[]{key}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            value = cursor.getString(0);
        }
        closeQuietly(cursor);
        return value;
    }

    private static void closeQuietly(Cursor cursor) {
        try {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception ignore) {
        }
    }

    public static void putBoolean(Context context, String key, boolean value) {
        putString(context, key, Boolean.toString(value));
    }

    public static boolean getBoolean(Context context, String key) {
        String value = getString(context, key);
        return value != null && Boolean.parseBoolean(value);
    }

    public static void putInt(Context context, String key, int value) {
        putString(context, key, String.valueOf(value));
    }

    public static int getInt(Context context, String key) {
        String value = getString(context, key);
        return value == null ? 0 : Integer.parseInt(value);
    }

}
