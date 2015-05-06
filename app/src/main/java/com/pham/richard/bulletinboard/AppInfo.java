package com.pham.richard.bulletinboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by Richard W Pham on 5/4/2015.
 */
public class AppInfo {

    public static final String PREF_USERID = "userid";

    private static AppInfo instance = null;

    protected AppInfo() {
        // Exists only to defeat instantiation.
    }
    // Here are some values we want to keep global.
    public String userid = "Rich";

    public static AppInfo getInstance(Context context) {
        if (instance == null) {
            instance = new AppInfo();
            // Creates a userid, if I don't have one.
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            instance.userid = settings.getString(PREF_USERID, null);
            if (instance.userid == null) {
                // We need to create a userid.
                SecureRandom random = new SecureRandom();
                instance.userid = new BigInteger(130, random).toString(32);
                System.out.println("*** I'm in getInstance. userid is " + instance.userid);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_USERID, instance.userid);
                editor.commit();
            }
        }
        return instance;
    }
}