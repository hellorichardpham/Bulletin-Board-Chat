package com.pham.richard.bulletinboard;

import android.text.format.DateUtils;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Richard W Pham on 4/26/2015.
 */

public class MsgInfo {
    String msg;
    String longitude;
    String latitude;
    String msgid;
    String app_id;
    String userid;
    String ts;
    String dest;
    boolean conversation;



    public MsgInfo() {}


    public String getTimedMessage() {
        return ts + "\n" + msg;
    }

    //Used for Debugging purposes
    public String toString() {
        return "msg |" + msg + "| userid |" + userid + "| dest |" + dest + "| conversation |" + conversation;
    }
}