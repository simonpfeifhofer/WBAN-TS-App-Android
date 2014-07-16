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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by sip on 29.06.2014.
 */
public class SensorDataTransmitter {

    private final static String TAG = SensorDataTransmitter.class.getSimpleName();
    private final static String mUrl = "http://wbants.cloudapp.net/api/sensordata";

    private UUID mUserId;
    private PreferencesHandler mPreferenceHandler;

    public SensorDataTransmitter(PreferencesHandler preferenceHandler){
        mPreferenceHandler = preferenceHandler;
        EnsureUserId();
    }

    public void SendData(ProfileType type, Object value){
        String activityId = mPreferenceHandler.GetActivityId();
        if(activityId == ""){
            Log.i(TAG, "Data transmission suppressed. No activity running.");
            return;
        }
        AsyncPost asyncPost = new AsyncPost(mUrl, mUserId, activityId, type, value);
        new Thread(asyncPost).start();
    }

    private void EnsureUserId(){

        String userIdString = mPreferenceHandler.GetUserId();
        UUID userId;
        if(userIdString != ""){
           userId = UUID.fromString(userIdString);
        }
        else{
            userId = UUID.randomUUID();
            mPreferenceHandler.SetUserId(userId.toString());
        }
        mUserId = userId;

    }

}
