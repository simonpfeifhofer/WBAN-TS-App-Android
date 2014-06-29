package com.wban_ts;

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
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
import java.net.URI;
import java.util.Date;
import java.util.UUID;

/**
 * Created by sip on 29.06.2014.
 */
public class SensorDataTransmitter {

    private final static String TAG = SensorDataTransmitter.class.getSimpleName();

    private final static String mUserIdSettingsKey = "USER-ID";
    private final static String mUrl = "http://wbants.cloudapp.net/api/sensordata";

    private UUID mUserId;

    public SensorDataTransmitter(SharedPreferences preferences){
        mUserId = EnsureUserId(preferences);
    }

    public void SendData(ProfileType type, Object value){

        AsyncPost asyncPost = new AsyncPost(type, value);
        new Thread(asyncPost).start();

    }

    private UUID EnsureUserId(SharedPreferences preferences){

        String userIdString = preferences.getString(mUserIdSettingsKey, "");
        UUID userId;
        if(userIdString != ""){
           userId = UUID.fromString(userIdString);
        }
        else{
            userId = UUID.randomUUID();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(mUserIdSettingsKey, userId.toString());
            editor.commit();
        }
        return userId;

    }

    public class AsyncPost implements Runnable {

        private ProfileType mType;
        private Object mValue;

        public AsyncPost(ProfileType type, Object value){
            this.mType = type;
            this.mValue = value;
        }

        private void PerformPost(String url, JSONObject obj) throws ClientProtocolException, IOException{

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

}
