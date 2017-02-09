package com.example.marcus.testwebview;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "myWebView";

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    //
    private static final String HOST = "172.20.143.41";
    private static final String PORT = "8080";
    private static final String DEFAULT_URL = HTTP + HOST + ":" + PORT + "/";

    private WebView mWebView;
    private Button mRetry;
    private EditText mAddress;

    private JsRequestListener mJsListener;
    private DefaultHttpClient mHttpClient;
    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mJsListener = new JsRequestListener();

        mAddress = (EditText) findViewById(R.id.addressEt);
        mAddress.setText(DEFAULT_URL);
        mRetry = (Button) findViewById(R.id.retryBtn);
        mRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(mAddress.getText().toString().trim());
            }
        });
        mWebView = (WebView) findViewById(R.id.testWebView);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.addJavascriptInterface(mJsListener,"JsRequestListener");
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                String jsString = "var test1 = function(){var input = document.getElementById(\"userinput\");\n" +
                        "    input.setAttribute('value',\"not from test1()\");}";
                String jsString2 = "lastRequestObject = {};\n" +
                        "    XMLHttpRequest.prototype.reallyOpen = XMLHttpRequest.prototype.open;\n" +
                        "    XMLHttpRequest.prototype.open = function(method, url, async, user, password) {\n" +
                        "        console.log(method + \" : \" + url);\n" +
                        "        lastRequestObject['method'] = method;\n" +
                        "        lastRequestObject['url'] = url;\n" +
                        "        this.reallyOpen(method, url, async, user, password);\n" +
                        "    };\n" +
                        "    XMLHttpRequest.prototype.reallySend = XMLHttpRequest.prototype.send;\n" +
                        "    XMLHttpRequest.prototype.send = function(body) {\n" +
                        "        if (typeof(JsRequestListener) !== 'undefined') {\n" +
                        "            JsRequestListener.record(lastRequestObject.method, lastRequestObject.url, body);\n" +
                        "        }\n" +
                        "        console.log(\"body : \" + body);\n" +
                        "        lastXmlhttpRequestPrototypeMethod = null;\n" +
                        "        this.reallySend(body);\n" +
                        "    };";
                view.loadUrl("javascript:" + jsString2);
                super.onPageFinished(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.i(TAG, "url: " + url);
                Log.i(TAG, "form JsRequestListener:" + mJsListener.getMehod()+ "  "
                        + mJsListener.getUrl() + "  " + mJsListener.getBody());
                if (mJsListener.isPost()){
                    Log.i(TAG,">>>>>>>POST>>>>>>>>>");
                }
                return super.shouldInterceptRequest(view, url);
            }
        });
        mWebView.loadUrl("https://www.baidu.com/");
        // mWebView.loadUrl(HTTP + HOST + ":" + PORT + "/");
    }
}
