package com.wban_ts;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by simon.pfeifhofer on 30.06.2014.
 */
public class AsyncPost implements Runnable {

    private final static String TAG = AsyncPost.class.getSimpleName();

    private String mUrl;
    private UUID mUserId;
    private ProfileType mType;
    private Object mValue;

    private static ConcurrentHashMap<ProfileType, ReentrantLock> mPostLocks = new ConcurrentHashMap<ProfileType, ReentrantLock>();

    static{
        mPostLocks.put(ProfileType.HRM, new ReentrantLock());
        mPostLocks.put(ProfileType.Location, new ReentrantLock());
    }

    public AsyncPost(String url, UUID userId, ProfileType type, Object value){
        this.mUrl = url;
        this.mUserId = userId;
        this.mType = type;
        this.mValue = value;
    }

    private void PerformPost(String url, JSONObject obj) throws ClientProtocolException, IOException {

        if (!mPostLocks.get(mType).tryLock()) {
            Log.i(TAG, String.format("Post not performed because of concurrent post of type %s", mType));
            return;
        }

        try {

            HttpParams myParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(myParams, 10000);
            HttpConnectionParams.setSoTimeout(myParams, 10000);
            HttpClient httpclient = new DefaultHttpClient(myParams);
            String json = obj.toString();

            HttpPost httppost = new HttpPost(url.toString());
            httppost.setHeader("Content-type", "application/json");

            StringEntity se = new StringEntity(obj.toString());
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httppost.setEntity(se);

            HttpResponse response = httpclient.execute(httppost);
            String responseString = "Response: " + EntityUtils.toString(response.getEntity());
            Log.i(TAG, responseString);

        }
        finally {

            mPostLocks.get(mType).unlock();

        }

    }

    public void run(){

        try {

            JSONObject body = new JSONObject();
            body.put("timestamp", new Date().getTime());
            body.put("userId", mUserId);
            body.put("profile", mType.toString());
            body.put("value", mValue);

            PerformPost(
                    mUrl,
                    body
            );

        }
        catch(JSONException e){
            Log.e(TAG,"JSON-Object cannot be build", e);
        }
        catch (ClientProtocolException e) {
            Log.e(TAG, "Error occurred posting data", e);
        }
        catch (IOException e) {
            Log.e(TAG, "Error occurred posting data", e);
        }
        catch (Exception e){
            Log.e(TAG, "Generic exception", e);
        }

    }

}
