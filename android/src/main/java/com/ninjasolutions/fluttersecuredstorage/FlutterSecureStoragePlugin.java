package com.ninjasolutions.fluttersecuredstorage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.ninjasolutions.fluttersecuredstorage.ciphers.StorageCipher;
import com.ninjasolutions.fluttersecuredstorage.ciphers.StorageCipher18Implementation;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

@SuppressLint("ApplySharedPref")
public class FlutterSecureStoragePlugin implements MethodCallHandler {

    private final SharedPreferences preferences;
    private final Charset charset;
    private final StorageCipher storageCipher;
    private static final String ELEMENT_PREFERENCES_KEY_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIHNlY3VyZSBzdG9yYWdlCg";
    private static final String SHARED_PREFERENCES_NAME = "FlutterSecureStorage";

    public static void registerWith(Registrar registrar) {
        try {
            FlutterSecureStoragePlugin plugin = new FlutterSecureStoragePlugin(registrar.context());
            final MethodChannel channel = new MethodChannel(registrar.messenger(), "plugins.indoor.solutions/flutter_secured_storage");
            channel.setMethodCallHandler(plugin);
        } catch (Exception e) {
            Log.e("FlutterSecureStoragePl", "Registration failed", e);
        }
    }

    private FlutterSecureStoragePlugin(Context context) throws Exception {
        preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        charset = Charset.forName("UTF-8");

        StorageCipher18Implementation.moveSecretFromPreferencesIfNeeded(preferences, context);
        storageCipher = new StorageCipher18Implementation(context);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            switch (call.method) {
                case "write": {
                    String key = getKeyFromCall(call);
                    Map arguments = (Map) call.arguments;

                    String value = (String) arguments.get("value");
                    write(key, value);
                    result.success(null);
                    break;
                }
                case "read": {
                    String key = getKeyFromCall(call);

                    String value = read(key);
                    result.success(value);
                    break;
                }
                case "readAll": {
                    Map<String, String> value = readAll();
                    result.success(value);
                    break;
                }
                case "delete": {
                    String key = getKeyFromCall(call);

                    delete(key);
                    result.success(null);
                    break;
                }
                case "deleteAll": {
                    deleteAll();
                    result.success(null);
                    break;
                }
                default:
                    result.notImplemented();
                    break;
            }

        } catch (Exception e) {
            result.error("Exception encountered", call.method, e);
        }
    }

    private String getKeyFromCall(MethodCall call) {
        Map arguments = (Map) call.arguments;
        String rawKey = (String) arguments.get("key");
        String key = addPrefixToKey(rawKey);
        return key;
    }

    private Map<String, String> readAll() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> raw = (Map<String, String>) preferences.getAll();

        Map<String, String> all = new HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey().replaceFirst(ELEMENT_PREFERENCES_KEY_PREFIX + '_', "");
            String rawValue = entry.getValue();
            String value = decodeRawValue(rawValue);

            all.put(key, value);
        }
        return all;
    }

    private void deleteAll() {
        SharedPreferences.Editor editor = preferences.edit();

        editor.clear();
        editor.commit();
    }

    private void write(String key, String value) throws Exception {
        byte[] result = storageCipher.encrypt(value.getBytes(charset));
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(key, Base64.encodeToString(result, 0));
        editor.commit();
    }

    private String read(String key) throws Exception {
        String encoded = preferences.getString(key, null);

        return decodeRawValue(encoded);
    }

    private void delete(String key) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(key);
        editor.commit();
    }

    private String addPrefixToKey(String key) {
        return ELEMENT_PREFERENCES_KEY_PREFIX + "_" + key;
    }

    private String decodeRawValue(String value) throws Exception {
        if (value == null) {
            return null;
        }
        byte[] data = Base64.decode(value, 0);
        byte[] result = storageCipher.decrypt(data);

        return new String(result, charset);
    }
}
