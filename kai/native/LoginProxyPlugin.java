package com.pixiv.login;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Strict TLS direct-only login, without MITM, proxies, or dynamic DNS. */
@CapacitorPlugin(name = "PixivLoginProxy")
public class LoginProxyPlugin extends Plugin {
    private static volatile PluginCall pendingLoginCall;

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject data = new JSObject();
        data.put("available", true);
        call.resolve(data);
    }

    @PluginMethod
    public void prepare(PluginCall call) {
        if (Boolean.TRUE.equals(call.getBoolean("useProxy", false))) {
            call.reject("KaiSafe: login proxy disabled");
            return;
        }
        JSObject data = new JSObject();
        data.put("mode", "direct");
        data.put("port", 0);
        data.put("proxySupported", false);
        call.resolve(data);
    }

    @PluginMethod
    public void openLogin(PluginCall call) {
        if (Boolean.TRUE.equals(call.getBoolean("useProxy", false))) {
            call.reject("KaiSafe: login proxy disabled");
            return;
        }
        String target = call.getString("url");
        if (target == null) {
            call.reject("Missing login URL");
            return;
        }
        Uri uri = Uri.parse(target);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"app-api.pixiv.net".equalsIgnoreCase(uri.getHost())
                || !"/web/v1/login".equals(uri.getPath())) {
            call.reject("KaiSafe refused non-Pixiv login URL");
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("No foreground activity");
            return;
        }
        if (pendingLoginCall != null) {
            call.reject("Another login is already in progress");
            return;
        }
        call.save();
        pendingLoginCall = call;
        try {
            Intent intent = new Intent(activity, LoginWebViewActivity.class);
            intent.putExtra(LoginWebViewActivity.EXTRA_URL, target);
            activity.startActivity(intent);
        } catch (Exception err) {
            pendingLoginCall = null;
            call.reject("Unable to open secure login");
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        call.resolve();
    }

    @PluginMethod
    public void refreshDns(PluginCall call) {
        call.reject("KaiSafe: external DNS override disabled");
    }

    @PluginMethod
    public void request(PluginCall call) {
        call.reject("KaiSafe: local MITM HTTP fallback disabled");
    }

    static void resolveLogin(String code) {
        PluginCall call = pendingLoginCall;
        pendingLoginCall = null;
        if (call != null) {
            JSObject result = new JSObject();
            result.put("code", code);
            call.resolve(result);
        }
    }

    static void cancelLogin() {
        PluginCall call = pendingLoginCall;
        pendingLoginCall = null;
        if (call != null) call.reject("cancelled", "user cancelled login");
    }
}
