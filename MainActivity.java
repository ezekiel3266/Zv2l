package com.zlauncher.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> chooser;
    private PermissionRequest pendingPerm;
    private static final int REQ_FILE = 11, REQ_LOC = 12, REQ_CAM = 13;
    private static final String APP_HOST = "appassets.androidplatform.net";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback cb) { cb.invoke(o, true, false); }
            // BatCam: let the bundled page (and only it) use the camera, after Android's own CAMERA prompt
            @Override public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(new Runnable() { public void run() {
                    boolean cam = false;
                    for (String res : r.getResources()) if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res)) cam = true;
                    if (!cam || !APP_HOST.equals(r.getOrigin().getHost())) { r.deny(); return; }
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        if (pendingPerm != null) pendingPerm.deny();
                        pendingPerm = r;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
                    } else r.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                }});
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest r) { if (pendingPerm == r) pendingPerm = null; }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try { startActivityForResult(p.createIntent(), REQ_FILE); }
                catch (Exception e) { chooser = null; return false; }
                return true;
            }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        immersive();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) immersive(); }

    @Override protected void onActivityResult(int req, int res, Intent d) {
        if (req == REQ_FILE && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d));
            chooser = null;
        } else super.onActivityResult(req, res, d);
    }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_CAM && pendingPerm != null) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) pendingPerm.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else pendingPerm.deny();
            pendingPerm = null;
        }
    }
    // Back closes the camera lightbox / settings / expanded card; a launcher never exits on Back.
    @Override public void onBackPressed() { web.evaluateJavascript("window.zlBack&&window.zlBack()", null); }
    // Release the camera when another app (maps, calls) is on top; music keeps playing because the WebView is not paused.
    @Override protected void onPause() { super.onPause(); web.evaluateJavascript("window.zlHidden&&window.zlHidden(true)", null); }
    @Override protected void onResume() { super.onResume(); web.onResume(); web.evaluateJavascript("window.zlHidden&&window.zlHidden(false)", null); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
