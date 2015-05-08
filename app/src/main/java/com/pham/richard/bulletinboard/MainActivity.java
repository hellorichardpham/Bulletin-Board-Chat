package com.pham.richard.bulletinboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;


import com.pham.richard.bulletinboard.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    Location lastLocation;

    private static final String LOG_TAG = "lclicker";

    // This is an id for my app, to keep the key space separate from other apps.
    private static final String MY_APP_ID = "luca_bboard";
    private static final String SERVER_URL_PREFIX = "https://hw3n-dot-luca-teaching.appspot.com/store/default/";

    // To remember the favorite account.
    public static final String PREF_ACCOUNT = "pref_account";

    // To remember the post we received.
    public static final String PREF_POSTS = "pref_posts";

    // Uploader.
    private ServerCall uploader;

    AppInfo appInfo;

    private ArrayList<MsgInfo> aList;

    ProgressBar spinner;

    private class MyAdapter extends ArrayAdapter<MsgInfo> {
        int resource;
        Context context;

        public MyAdapter(Context _context, int _resource, List<MsgInfo> items) {
            super(_context, _resource, items);
            resource = _resource;
            context = _context;
            this.context = _context;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LinearLayout newView;

            String w = getItem(position).getTimedMessage();
            // Inflate a new view if necessary.
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                String inflater = Context.LAYOUT_INFLATER_SERVICE;
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
                vi.inflate(resource, newView, true);
            } else {
                newView = (LinearLayout) convertView;
            }

            // Fills in the view.
            TextView tv = (TextView) newView.findViewById(R.id.itemText);
            tv.setMovementMethod(new ScrollingMovementMethod());
            tv.setText(w.toString());
            ImageView image = (ImageView) newView.findViewById(R.id.imageView);
            image.setVisibility(View.INVISIBLE);
            if (getItem(position).conversation) {
                image.setVisibility(View.VISIBLE);
            }
            // Set a listener for the whole list item.
            newView.setTag(w.toString());
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Only do it if I'm NOT clicking on myself
                    if (!getItem(position).userid.equals(appInfo.userid)) {
                        String destinationUserId = getItem(position).userid;
                        //When you click on a listElement, enter a new chatActivity
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra("userid", destinationUserId);
                        context.startActivity(intent);
                    }
                }
            });
            return newView;
        }
    }

    private MyAdapter aa;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aList = new ArrayList<MsgInfo>();
        aa = new MyAdapter(this, R.layout.list_element, aList);
        ListView myListView = (ListView) findViewById(R.id.listView);
        myListView.setAdapter(aa);
        TextView locationView = (TextView) findViewById(R.id.locationView);
        spinner = (ProgressBar) findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);
        appInfo = AppInfo.getInstance(this); //generate userID
        aa.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // First super, then do stuff.
        // Let us display the previous posts, if any.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String result = settings.getString(PREF_POSTS, null);
        if (result != null) {
            try {
                displayResult(result);
            } catch (Exception e) {
                // Removes settings that can't be correctly decoded.
                Log.w(LOG_TAG, "Failed to display old messages: " + result + " " + e);
                SharedPreferences.Editor editor = settings.edit();
                editor.remove(PREF_POSTS);
                editor.commit();
            }
        }
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        String lat = String.format("%.3f", lastLocation.getLatitude());
        String lng = String.format("%.3f", lastLocation.getLongitude());
        TextView locationView = (TextView) findViewById(R.id.locationView);
        String locationString = "Latitude: " + lat + " Longitude: "
                + lng + " Accuracy: " + lastLocation.getAccuracy() + " meters";
        clickRefresh(locationView);
        locationView.setText(locationString);
    }


    @Override
    protected void onPause() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
        // Stops the upload if any.
        if (uploader != null) {
            uploader.cancel(true);
            uploader = null;
        }

        super.onPause();
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            TextView locationView = (TextView) findViewById(R.id.locationView);
            String lat = String.format("%.3f", location.getLatitude());
            String lng = String.format("%.3f", location.getLongitude());
            lastLocation = location;
            String locationString = "Latitude: " + lat + " Longitude: "
                    + lng + " Accuracy: " + lastLocation.getAccuracy() + " meters";
            locationView.setText(locationString);
            //Only display the toast if up to the 1000th digit has changed to
            //Prevent toasts from displaying due to accuracy changes
            if (isBigLocationChange(lastLocation, location)) {
                int duration = Toast.LENGTH_SHORT;
                Context context = getApplicationContext();
                String locationToastString = "Location Changed:\n" + "Latitude: " + lat + " Longitude: "
                        + lng;
                Toast toast = Toast.makeText(context, locationToastString, duration);
                toast.show();
            }
        }

        //Problem: Toast would occur for slight location changes due to accuracy (.0000###)
        //where ### is only changing due to GPS signal. This method only checks if the
        //10000th place has changed as to stop unnecessary toasts showing
        public boolean isBigLocationChange(Location currentLocation, Location newLocation) {
            //Parse the coordinates down to 4 decimal places
            String currentLat = String.format("%.4f", currentLocation.getLatitude());
            String currentLng = String.format("%.4f", currentLocation.getLongitude());
            String newLat = String.format("%.4f", newLocation.getLatitude());
            String newLng = String.format("%.4f", newLocation.getLongitude());
            if (currentLat.equals(newLat) && currentLng.equals(newLng)) {
                return false;
            }
            return true;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    // Get the text we want to send.
    public void clickPost(View v) {
        EditText et = (EditText) findViewById(R.id.editText);
        String msg = et.getText().toString();
        spinner.setVisibility(v.VISIBLE);
        PostMessageSpec myCallSpec = new PostMessageSpec();

        myCallSpec.url = SERVER_URL_PREFIX + "put_local";
        myCallSpec.context = MainActivity.this;

        double latitude = lastLocation.getLatitude();
        double longitude = lastLocation.getLongitude();

        String msgid = UUID.randomUUID().toString();
        msgid = msgid.replace("-", "");

        String lat = Double.toString(latitude);
        String lng = Double.toString(longitude);
        HashMap<String, String> m = new HashMap<String, String>();
        m.put("msg", msg);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("msgid", msgid);
        m.put("app_id", MY_APP_ID);
        m.put("userid", appInfo.userid);
        m.put("dest", "public");
        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
        et.setText("");
    }

    public void clickRefresh(View v) {
        spinner.setVisibility(v.VISIBLE);
        PostMessageSpec myCallSpec = new PostMessageSpec();

        myCallSpec.url = SERVER_URL_PREFIX + "get_local";
        myCallSpec.context = MainActivity.this;
        String lat = Double.toString(lastLocation.getLatitude());
        String lng = Double.toString(lastLocation.getLongitude());

        HashMap<String, String> m = new HashMap<String, String>();
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("userid", appInfo.userid);
        myCallSpec.setParams(m);
        // Actual server call.
        if (uploader != null) {
            // There was already an upload in progress.
            uploader.cancel(true);
        }
        uploader = new ServerCall();
        uploader.execute(myCallSpec);
    }


    /**
     * This class is used to do the HTTP call, and it specifies how to use the result.
     */
    class PostMessageSpec extends ServerCallSpec {
        @Override
        public void useResult(Context context, String result) {
            if (result == null) {
                // Do something here, e.g. tell the user that the server cannot be contacted.
                Log.i(LOG_TAG, "The server call failed.");
            } else {
                // Translates the string result, decoding the Json.
                Log.i(LOG_TAG, "Received string: " + result);
                displayResult(result);
                // Stores in the settings the last messages received.
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PREF_POSTS, result);
                editor.commit();
            }
        }
    }


    private void displayResult(String result) {
        Gson gson = new Gson();
        MessageList ml = gson.fromJson(result, MessageList.class);
        //Clear the list of messages so it's a clean slate.
        aList.clear();
        //Iterate through the gson request. Create a
        //ListElement which is one line of the view.
        //Set the attributes of the ListElement using
        //the gson attributes. Then add to list.
        for (int i = 0; i < ml.messages.length; i++) {
            if (ml.messages[i].dest.equals("public")) {
                aList.add(ml.messages[i]);
            }
        }
        aa.notifyDataSetChanged();
        spinner.setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
