package com.example.marcus.testwebview;

import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

/**
 * Created by marcus on 17/2/9.
 */

public class JsRequestListener {
    private static final String TAG = "JsRequestListener";
    private String mMehod;
    private String mUrl;
    private String mBody;
    public String cookies;

    @JavascriptInterface
    public void record(String method, String url, String body, String cookies) throws UnsupportedEncodingException {
        Log.d(TAG, method + "  "  + url + "  " + body);
        Log.d(TAG," cookies : " + cookies);
        this.setMehod(method);
        this.setUrl(url);
        if(null != body){
            // this.setmBody(URLDecoder.decode(body,"utf-8"));
            this.setmBody(body);
        }else{
            this.setmBody(null);
        }
        this.cookies = cookies;
    }

    public String getMehod() {
        return mMehod;
    }

    public void setMehod(String mMehod) {
        this.mMehod = mMehod;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    public String getBody() {
        return mBody;
    }

    public void setmBody(String mBody) {
        this.mBody = mBody;
    }

    public boolean isPost(){
        if(null != mMehod){
            return mMehod.toLowerCase().equals("post");
        }
        return false;
    }
}


