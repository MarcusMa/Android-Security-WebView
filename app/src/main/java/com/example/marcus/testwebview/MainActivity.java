package com.example.marcus.testwebview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "myWebView";

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    //
//    private static final String HOST = "172.20.143.41";
    private static final String HOST = "192.168.0.104";
    private static final String PORT = "8089";
    private static final String DEFAULT_URL = HTTP + HOST + ":" + PORT + "/";

    private WebView mWebView;
    private Button mRetry;
    private EditText mAddress;

    private JsRequestListener mJsListener;
    private URL mReqUrl;
    private DefaultHttpClient mClient;
    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mJsListener = new JsRequestListener();
        mClient = new DefaultHttpClient();
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

                /** 在页面加载完毕后注入的JS代码,改变XMLHttpRequest的运行机制 */
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
                Log.i(TAG, "form JsRequestListener:" + mJsListener.getMehod() + "  "
                        + mJsListener.getUrl() + "  " + mJsListener.getBody());
                // return shouldInterceptRequestByHttpClient(view,request);
                return shouldInterceptRequestByHttpClientByURLConnection(view,request);
                //return null;
            }
        });


        mWebView.loadUrl("https://www.baidu.com/");
        // mWebView.loadUrl(HTTP + HOST + ":" + PORT + "/");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequestByHttpClientByURLConnection(WebView view, WebResourceRequest request){
        try {
            if(request.getMethod().toLowerCase().equals("get")){
                return null;
            }

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
                dos.writeBytes(mJsListener.getBody());
                dos.flush();
                dos.close();
                mJsListener.setMehod(null); // clear for next request
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
            String strContents = new String(pageContents, "UTF-8");
            Log.e("Client:","mine :" + mime +" Charset: " + charset + " isContents:" + strContents);
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequestByHttpClient(WebView view, WebResourceRequest request){
        try {
            HttpResponse resp = null;
            if (mJsListener.isPost()) {
                Log.i(TAG, ">>>>>>>POST>>>>>>>>>");
                HttpPost postRequest = null;
                postRequest = new HttpPost(request.getUrl().toString());
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("", "jake"));
                HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, "utf-8");
                postRequest.setEntity(entity);
                resp = mClient.execute(postRequest);
            } else {
                HttpGet getRequest = new HttpGet(request.getUrl().toString());
                resp = mClient.execute(getRequest);
            }
            if(null == resp){
                return null;
            }
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Header[] headers = resp.getAllHeaders();
                String charset = Charset.defaultCharset().displayName(), mime = null;
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].getName().toLowerCase().equals("content-type")) {
                        Log.d(TAG,">>>>>>mine:" + mime);
                        mime = headers[i].getValue();
                        break;
                    }
                }
                if (mime.contains(";")) {
                    mime = mime.split(";")[0].trim();
                }
                //InputStream in = resp.getEntity().getContent();
                Log.e("Client:", "mine :" + mime + " Charset: " + charset );
                String content = EntityUtils.toString(resp.getEntity());

                //return  super.shouldInterceptRequest(view,url);
                return new WebResourceResponse(mime, charset, new ByteArrayInputStream(content.getBytes("UTF-8")));
            }
            else{
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
