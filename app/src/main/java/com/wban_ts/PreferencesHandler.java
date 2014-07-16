package com.wban_ts;

import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Created by simon.pfeifhofer on 16.07.2014.
 */
public class PreferencesHandler {

    private final static String mUserIdSettingsKey = "USER-ID";
    private final static String mActivityIdSettingsKey = "ACTIVITY-ID";

    private SharedPreferences mPreferences;


    public PreferencesHandler(SharedPreferences preferences){
        mPreferences = preferences;
    }

    public void SetUserId(String userId){

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(mUserIdSettingsKey, userId);
        editor.commit();

    }

    public String GetUserId(){

        return mPreferences.getString(mUserIdSettingsKey, "");

    }

    public void SetActivityId(String activityId) {

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(mActivityIdSettingsKey, activityId);
        editor.commit();

    }

    public String GetActivityId(){

        return mPreferences.getString(mActivityIdSettingsKey, "");

    }

}
