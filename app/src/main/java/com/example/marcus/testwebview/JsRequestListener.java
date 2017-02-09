package com.example.marcus.testwebview;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Created by marcus on 17/2/9.
 */

public class JsRequestListener {
    private static final String TAG = "JsRequestListener";
    private String mMehod;
    private String mUrl;
    private String mBody;

    @JavascriptInterface
    public void record(String method, String url, String body){
        Log.d(TAG, method + "  "  + url + "  " + body);
        this.setMehod(method);
        this.setUrl(url);
        this.setmBody(body);
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


