package com.example.marcus.testwebview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

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
    private URL mReqUrl;
    private OkHttpClient mOkHttpClient;
    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mJsListener = new JsRequestListener();
        mOkHttpClient = new OkHttpClient();
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

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Log.i(TAG, "url: " + request.getUrl());
                Log.i(TAG, "form JsRequestListener:" + mJsListener.getMehod()+ "  "
                        + mJsListener.getUrl() + "  " + mJsListener.getBody());
                if (mJsListener.isPost()){
                    Log.i(TAG,">>>>>>>POST>>>>>>>>>");
                }

                try {
                    /** do request by other httpclient **/
                    mReqUrl = new URL(request.getUrl().toString());
                    URLConnection rulConnection = mReqUrl.openConnection();
                    HttpURLConnection conn = (HttpURLConnection) rulConnection;
                    Map headers = request.getRequestHeaders();
                    if(null != headers){
                        Iterator<Map.Entry<String, String>> entries = headers.entrySet().iterator();
                        while (entries.hasNext()) {
                            Map.Entry<String, String> entry = entries.next();
                            conn.setRequestProperty(entry.getKey(),entry.getValue());
                            Log.d(TAG, entry.getKey() + " : " + entry.getValue());
                        }
                    }

                    conn.setRequestMethod(mJsListener.isPost() ? "POST" : "GET");

                    if(mJsListener.isPost()){
                        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                        //dos.writeBytes(mJsListener.getBody());
                        String requestString = "name=marcus";
                        byte[] requestStringBytes = requestString.getBytes("utf-8");
                        dos.write(requestStringBytes);
                        dos.flush();
                        dos.close();
                        mJsListener.setMehod(null);
                    }

                    String charset = conn.getContentEncoding() != null ? conn.getContentEncoding() : Charset.defaultCharset().displayName();
                    String mime = conn.getContentType();
                    // Parse mime type from contenttype string
                    if (mime.contains(";")) {
                        mime = mime.split(";")[0].trim();
                    }
                    byte[] pageContents = IOUtils.readFully(conn.getInputStream());

                    // Convert the contents and return
                    InputStream isContents = new ByteArrayInputStream(pageContents);
                    Log.e("Client:","mine :" + mime +" Charset: " + charset + " isContents:" + isContents);
                    //return  super.shouldInterceptRequest(view,url);
                    return new WebResourceResponse(mime, charset, isContents);

                } catch (ProtocolException e) {
                    e.printStackTrace();

                    return null;
                } catch (IOException e) {
                    e.printStackTrace();

                    return null;
                } catch (Exception e) {
                    return null;
                }
                // return super.shouldInterceptRequest(view, request);
            }

        });
        // mWebView.loadUrl("https://www.baidu.com/");
        mWebView.loadUrl(HTTP + HOST + ":" + PORT + "/");
    }

}
