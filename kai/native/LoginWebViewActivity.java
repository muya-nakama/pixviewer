package com.pixiv.login;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/** Direct Pixiv OAuth WebView with strict certificate validation. */
public final class LoginWebViewActivity extends Activity {
    public static final String EXTRA_URL = "url";
    private static final String CALLBACK_HOST = "app-api.pixiv.net";
    private static final String CALLBACK_PATH = "/web/v1/users/auth/pixiv/callback";
    private WebView webView;
    private boolean codeHandled;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            finish();
            return;
        }
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        try {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        } catch (Exception ignored) {}

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String uri) {
                return handleUrl(uri);
            }
            @Override
            public void onPageStarted(WebView view, String uri, Bitmap favicon) {
                if (handleUrl(uri)) return;
                super.onPageStarted(view, uri, favicon);
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                Toast.makeText(LoginWebViewActivity.this, "Pixiv TLS verification failed", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request != null && request.isForMainFrame()) {
                    Toast.makeText(LoginWebViewActivity.this, "Pixiv login page failed", Toast.LENGTH_SHORT).show();
                }
                super.onReceivedError(view, request, error);
            }
        });
        setContentView(webView);
        webView.loadUrl(url);
    }

    private boolean handleUrl(String target) {
        if (target == null || codeHandled) return codeHandled;
        Uri uri;
        try {
            uri = Uri.parse(target);
        } catch (Exception err) {
            return false;
        }
        String code = extractCode(uri);
        if (code == null) return false;
        codeHandled = true;
        LoginProxyPlugin.resolveLogin(code);
        finish();
        return true;
    }

    private static String extractCode(Uri uri) {
        String scheme = uri.getScheme();
        boolean valid = "pixiv".equals(scheme) || "pixiv-inner".equals(scheme)
                || ("https".equalsIgnoreCase(scheme)
                    && CALLBACK_HOST.equalsIgnoreCase(uri.getHost())
                    && CALLBACK_PATH.equals(uri.getPath()));
        if (!valid) return null;
        String code = uri.getQueryParameter("code");
        if (code != null && !code.isEmpty()) return code;
        String fragment = uri.getFragment();
        if (fragment == null) return null;
        try {
            return Uri.parse("https://local.pixiv.login/?" + fragment).getQueryParameter("code");
        } catch (Exception err) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (!codeHandled) LoginProxyPlugin.cancelLogin();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
