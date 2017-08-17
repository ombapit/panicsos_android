package com.ombapit.alarmbutton;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebsiteActivity extends AppCompatActivity {

    ProgressDialog loading;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_website);

        WebView webview = (WebView)findViewById(R.id.website_oku);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(getString(R.string.web_oku));

        webview.setWebViewClient(new WebViewClient() {
            private ProgressDialog mProgress;
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                // TODO show you progress image
                super.onPageStarted(view, url, favicon);
                loading = ProgressDialog.show(WebsiteActivity.this, "Loading Website", "Mohon tunggu ...",true,true);
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                // TODO hide your progress image
                super.onPageFinished(view, url);
                loading.dismiss();
            }
        });
    }
}
