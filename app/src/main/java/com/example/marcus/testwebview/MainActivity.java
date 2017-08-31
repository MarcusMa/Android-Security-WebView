package com.example.marcus.testwebview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
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
import org.apache.http.cookie.Cookie;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "myWebView";

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    //
    private static final String HOST = "172.20.143.41";
    // private static final String HOST = "192.168.0.104";
    private static final String PORT = "8083";
    private static final String DEFAULT_URL = HTTP + HOST + ":" + PORT + "/";

    private WebView mWebView;
    private Button mRetry;
    private EditText mAddress;

    private JsRequestListener mJsListener;
    private URL mReqUrl;
    private DefaultHttpClient mClient;
    private String webViewUrl = "https://wappass.baidu.com/";

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CookieSyncManager.createInstance(this);

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
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(mJsListener,"JsRequestListener");
        mWebView.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG,">>>>>>>>>>>>>>>>>>>>>> shouldOverrideUrlLoading >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                Log.d(TAG,">>>>>>>>>>>>>>>>>>>>>> origin : "+ view.getUrl()+" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                Log.d(TAG,">>>>>>>>>>>>>>>>>>>>>> override : "+ url +" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                Log.d(TAG,">>>>>>>>>>>>>>>>>>>>>> shouldOverrideUrlLoading >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                CookieManager cookieManager = CookieManager.getInstance();
                List<Cookie> cookies = mClient.getCookieStore().getCookies();
                //sync all the cookies in the httpclient with the webview by generating cookie string
                for (Cookie cookie : cookies){
                    String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
                    Log.e(TAG,"HttpClient Cookies: " + cookieString);
                    // cookieManager.setCookie(toUrl, cookieString);
                    // CookieSyncManager.getInstance().sync();
                }

                webViewUrl = url;
                return super.shouldOverrideUrlLoading(view, url);
                // view.loadUrl(url);
                // return true;
            }

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
                        "            JsRequestListener.record(lastRequestObject.method, lastRequestObject.url, body,document.cookie);\n" +
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
                Log.w(TAG, "method:" + request.getMethod() + " url: " + request.getUrl());
                Log.w(TAG, "form JsRequestListener:" + mJsListener.getMehod() + " /  "
                        + mJsListener.getUrl() + " /  " + mJsListener.getBody());
                Log.w(TAG, "form JsRequestListener cookies: " + mJsListener.cookies);
                //return shouldInterceptRequestByHttpClient(view,request);
                return shouldInterceptRequestByURLConnection(view,request);
                // return null;
            }
        });

        // mWebView.loadUrl("https://www.taobao.com");
        // mWebView.loadUrl("https://www.baidu.com/");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView,true);
        }
        // mWebView.loadUrl(webViewUrl);
        // mWebView.loadUrl(HTTP + HOST + ":" + PORT + "/");
        mWebView.loadUrl(DEFAULT_URL);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequestByURLConnection(WebView view, WebResourceRequest request){
        try {

            mReqUrl = new URL(request.getUrl().toString());
            URLConnection rulConnection = mReqUrl.openConnection();
            HttpURLConnection conn = (HttpURLConnection) rulConnection;
            // set request Method
            Log.d(TAG, "Request Method : " + request.getMethod());
            conn.setRequestMethod(request.getMethod().equalsIgnoreCase("post") ? "POST" : "GET");

            // set request Header
            Map headers = request.getRequestHeaders();
            if(null != headers){
                Iterator<Map.Entry<String, String>> entries = headers.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, String> entry = entries.next();
                    conn.setRequestProperty(entry.getKey(),entry.getValue());
                    Log.d(TAG, "Request Header : " + entry.getKey() + " : " + entry.getValue());
                }
            }

            // set request Body if the request is 'POST'
            if(request.getMethod().toLowerCase().equals("post")){
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(mJsListener.getBody());
                dos.flush();
                dos.close();
                mJsListener.setMehod(null); // clear for next request
                mJsListener.setmBody(null);
                mJsListener.setUrl(null);
            }

            // set request CooKie
            String cookies = CookieManager.getInstance().getCookie(webViewUrl);
            Log.d(TAG,"Request Cookie :" + cookies);
            conn.setRequestProperty("Cookie",cookies);


            Log.d(TAG,">>>>> Response >>>>>");

            // according "Set-Cookie" field to add cookies into CookieManager
            Map<String, List<String>> headFields = conn.getHeaderFields();
            List<String> cookieList = headFields.get("Set-Cookie");
            if( null != cookieList){
                CookieManager cookieManager = CookieManager.getInstance();
                for(String cookie: cookieList) {
                    Log.e(TAG,"Response > Set-Cookie : "+ cookie);
                    cookieManager.setCookie(webViewUrl, cookie);
                }
                CookieSyncManager.getInstance().sync();
            }

            // set WebResourceResponse to return
            String charset = conn.getContentEncoding() != null ? conn.getContentEncoding() : Charset.defaultCharset().displayName();
            String mime = conn.getContentType();
            // parse mime type from content-type string
            if (mime.contains(";")) {
                mime = mime.split(";")[0].trim();
            }
            byte[] pageContents = IOUtils.readFully(conn.getInputStream());

            // convert the contents and return
            Map<String,String> tmpMap = new HashMap<>();
            for(Map.Entry<String, List<String>> entry: headFields.entrySet()){
                String str = "";
                for( String tampering : entry.getValue()){
                    str = str + tampering;
                }
                tmpMap.put(entry.getKey(),str);
            }

            // convert the contents and return
            InputStream isContents = new ByteArrayInputStream(pageContents);
            String strContents = new String(pageContents, "UTF-8");

            Log.d(TAG," WebResourceResponse >");
            Log.d(TAG, " Mime : " + mime +" Charset : " + charset +
                    " Response Code : " + conn.getResponseCode() +
                    " Phase Reason : " + "OK" +
                    " Header : " + tmpMap.toString() +
                    " Content : " + strContents);
            return new WebResourceResponse(mime,charset,conn.getResponseCode(),"OK",tmpMap,isContents);
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // for any problem, return null to make the WebView load the resource itself.
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Deprecated
    public WebResourceResponse shouldInterceptRequestByHttpClient(WebView view, WebResourceRequest request){
        try {
            HttpResponse resp = null;
            if (request.getMethod().equalsIgnoreCase("post")){
                Log.i(TAG, ">>>>>>> POST >>>>>>>>>");
                Log.i(TAG, ">>>>>>> POST BODY >>>> " + mJsListener.getBody());
                HttpPost postRequest = null;
                postRequest = new HttpPost(request.getUrl().toString());
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                // 处理 JsListener 记录的 POST 参数
                String[] paramsValuePairs = mJsListener.getBody().split("&");
                for(String tmp :paramsValuePairs){
                    String[] keyValue = tmp.split("=");
                    if(keyValue.length == 2){
                        nameValuePairs.add(new BasicNameValuePair(keyValue[0].trim(),keyValue[1].trim()));
                    }
                }
                // 清除 JsListener 中的记录
                mJsListener.setmBody(null);
                HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs, "utf-8");
                postRequest.setEntity(entity);
                resp = mClient.execute(postRequest);
            } else {
                Log.i(TAG, ">>>>>>> GET >>>>>>>>>");
                HttpGet getRequest = new HttpGet(request.getUrl().toString());
                resp = mClient.execute(getRequest);
            }

            if(null == resp){
                return null; // 交由WebView自身处理
            }

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Header[] headers = resp.getAllHeaders();
                String charset = Charset.defaultCharset().displayName();
                String mime = null;
                List<String> cookieList = new ArrayList<String>();

                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].getName().equalsIgnoreCase("content-type")) {
                        mime = headers[i].getValue();
                    }
                    if (headers[i].getName().equalsIgnoreCase("set-cookie")){
                        cookieList.add(headers[i].getValue());
                    }
                }

                if (cookieList.size()>0){
                    Log.e("cookie","**********************Set-Cookie***********************");
                    CookieManager cookieManager = CookieManager.getInstance();
                    // cookieManager.setAcceptCookie(true);
                    CookieManager.getInstance().setAcceptThirdPartyCookies(view,true);
                    for(String cookie: cookieList) {
                        Log.e("cookie","************ Set-Cookie : " + cookie);
                        cookieManager.setCookie(webViewUrl, cookie);
                    }
                    CookieSyncManager.getInstance().sync();
                    Log.e("cookie","************ For Url : " + request.getUrl().toString());
                    Log.e("cookie","**********************Set-Cookie***********************");
                }


                if (mime.contains(";")) { // 如果mime 是这种形式 text/javascript;charset=utf-8
                    String[] tmp = mime.split(";");
                    mime = tmp[0].trim(); // 获取Mime
                    if (mime.contains("charset")||mime.contains("Charset")){
                        charset = tmp[1].trim().split("=")[1].trim(); //获取Charset
                    }
                }
                Log.e("Client:", "mine :" + mime + " Charset: " + charset );
                String content = EntityUtils.toString(resp.getEntity());
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
