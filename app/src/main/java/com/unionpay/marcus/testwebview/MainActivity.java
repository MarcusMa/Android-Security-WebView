package com.unionpay.marcus.testwebview;

import android.app.Activity;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

public class MainActivity extends Activity {
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
    private URL mReqUrl;

    private String webViewUrl = "https://wappass.baidu.com/";

    private boolean continueFlag = true;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CookieSyncManager.createInstance(this);

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
        mWebView.setWebViewClient(new WebViewClient(){

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.e(TAG,"onReceivedSslError()");
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG,">>>>> shouldOverrideUrlLoading -> origin : "+ view.getUrl());
                Log.d(TAG,">>>>> shouldOverrideUrlLoading -> override : "+ url);
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (continueFlag){
                    return shouldInterceptRequestByURLConnection(view,url);
                }
                else{
                    return super.shouldInterceptRequest(view, url);
                }
            }
        });

        // mWebView.loadUrl("https://www.taobao.com");
        mWebView.loadUrl("https://www.baidu.com/");
    }

    public WebResourceResponse shouldInterceptRequestByURLConnection(WebView view,  String url){
        try {
            //设置SSLContext
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{myX509TrustManager}, null);
            mReqUrl = new URL(url);
            URLConnection urlConnection = mReqUrl.openConnection();
            // HttpURLConnection conn = (HttpURLConnection) rulConnection;
            HttpsURLConnection conn = (HttpsURLConnection)  urlConnection;

            //设置套接工厂
            conn.setSSLSocketFactory(sslcontext.getSocketFactory());

            // set request Method
            Log.d(TAG, "Request Method : " + "GET" + " / " + url);
            conn.setRequestMethod("GET");

            // set request Header
//            Map headers = request.getRequestHeaders();
//            if(null != headers){
//                Iterator<Map.Entry<String, String>> entries = headers.entrySet().iterator();
//                while (entries.hasNext()) {
//                    Map.Entry<String, String> entry = entries.next();
//                    conn.setRequestProperty(entry.getKey(),entry.getValue());
//                    Log.d(TAG, "Request Header : " + entry.getKey() + " : " + entry.getValue());
//                }
//            }

            // set request Body if the request is 'POST'
//            if(request.getMethod().toLowerCase().equals("post")){
//                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
//                dos.writeBytes(mJsListener.getBody());
//                dos.flush();
//                dos.close();
//                mJsListener.setMehod(null); // clear for next request
//                mJsListener.setmBody(null);
//                mJsListener.setUrl(null);
//            }

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

            if (null != mime){ // mine will be null, if throw a CertificateException in TrustManager
                // parse mime type from content-type string
                if (mime.contains(";")) {
                    mime = mime.split(";")[0].trim();
                }
                byte[] pageContents = IOUtils.readFully(conn.getInputStream());

                // convert the contents and return
                Map<String,String> tmpMap = new HashMap<String, String>();
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
                return new WebResourceResponse(mime,charset,isContents);
            }
        } catch (SSLHandshakeException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            continueFlag = false;
        }
        WebResourceResponse rep = null;
        try {
            rep = getDefalutErrorWebResourceResponse();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            rep = new WebResourceResponse(null,null,null);
        }
        return rep;
    }

    private static WebResourceResponse getDefalutErrorWebResourceResponse() throws UnsupportedEncodingException {
        String htmlString = "<p>Error: CertificateException </p>";
        InputStream in = new ByteArrayInputStream(htmlString.getBytes("UTF-8"));
        return new WebResourceResponse("text/html","utf-8",in);
    }

    private static TrustManager myX509TrustManager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            Log.d("X509TrustManager" , "checkClientTrusted()");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            Log.d("X509TrustManager" , "checkServerTrusted()");
            X509Certificate certificate = chain[0];
            X500Principal issuerPrincipal = certificate.getIssuerX500Principal();
            Log.d("X509TrustManager" , "issuer name :" + issuerPrincipal.getName());
            X500Principal subjectPrincipal = certificate.getSubjectX500Principal();
            Log.d("X509TrustManager" , "subject name :" + subjectPrincipal.getName());

            // throw new CertificateException("Error");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            Log.d("X509TrustManager" , "getAcceptedIssuers()");
            return null;
        }
    };
}
