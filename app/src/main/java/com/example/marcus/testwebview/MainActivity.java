package com.example.marcus.testwebview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String HTTP = "http://";
    private static final String HOST = "192.168.0.108";
    private static final String PORT = "8080";
    private WebView mWebView;
    private Button mRetry;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRetry = (Button) findViewById(R.id.retryBtn);
        mRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(HTTP + HOST + ":" + PORT + "/");
            }
        });
        mWebView = (WebView) findViewById(R.id.testWebView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        // mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                String jsString = "var test1 = function(){var input = document.getElementById(\"userinput\");\n" +
                        "    input.setAttribute('value',\"not from test1()\");}";
                view.loadUrl("javascript:" + jsString);
                super.onPageFinished(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.i("myWebView", "url: " + url);
                return super.shouldInterceptRequest(view, url);
            }
        });
        mWebView.loadUrl("https://www.baidu.com/");
        // mWebView.loadUrl(HTTP + HOST + ":" + PORT + "/");
    }
}
