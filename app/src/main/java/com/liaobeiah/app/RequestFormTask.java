package com.liaobeiah.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by dokinkon on 2/10/14.
 */
public class RequestFormTask extends AsyncTask<Bundle, Void, Void> {


    public interface Listener {
        public void onTaskFinish(int resultCode, File docxFile);
    }

    private Context _context;
    private SharedPreferences _preferences;
    private ContentValues _contentValues;
    private Listener _listener;
    private File _docxFile;
    private static final String TAG = "RequestFormTask";

    RequestFormTask(Context context, ContentValues contentValues, Listener listener) {
        _context = context;
        if (context==null) {
            throw new NullPointerException();
        }
        _preferences = PreferenceManager.getDefaultSharedPreferences(_context);
        _contentValues = contentValues;
        _listener = listener;
    }


    @Override
    protected Void doInBackground(Bundle... extras) {

        HttpClient httpClient = new DefaultHttpClient();

        try {

            // Build query parameters..
            List<NameValuePair> params = new LinkedList<NameValuePair>();

            String name = _preferences.getString("pref_name", "");
            String address = _preferences.getString("pref_address", "");
            String phone = _preferences.getString("pref_phone", "");
            String email = _preferences.getString("pref_email", "");
            String date = _contentValues.getAsString(FormConstants.DATE);
            String time = _contentValues.getAsString(FormConstants.TIME);
            String license = _contentValues.getAsString(FormConstants.VEHICLE_LICENSE);
            String eventLocation = _contentValues.getAsString(FormConstants.LOCATION);
            String eventType = _contentValues.getAsString(FormConstants.REASON);
            String comment = _contentValues.getAsString(FormConstants.COMMENT);

            Log.i(TAG, "NAME = " + name);
            Log.i(TAG, "PHONE = " + phone);
            Log.i(TAG, "ADDRESS = " + address);
            Log.i(TAG, "E-MAIL = " + email);
            Log.i(TAG, "DATE = " + date);
            Log.i(TAG, "TIME = " + time);
            Log.i(TAG, "LICENSE = " + license);
            Log.i(TAG, "LOCATION = " + eventLocation);
            Log.i(TAG, "EVENT-TYPE = " + eventType);
            Log.i(TAG, "COMMENT = " + comment);

            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("address", address));
            params.add(new BasicNameValuePair("phone", phone));
            params.add(new BasicNameValuePair("email", email));
            params.add(new BasicNameValuePair("date", date));
            params.add(new BasicNameValuePair("time", time));
            params.add(new BasicNameValuePair("license", license));
            params.add(new BasicNameValuePair("event-location", eventLocation));
            params.add(new BasicNameValuePair("event-type", eventType));
            params.add(new BasicNameValuePair("comment", comment));


            String paramString = URLEncodedUtils.format(params, "utf-8");
            String urlString = "http://doktestjetty.herokuapp.com/hello.docx?" + paramString;
            HttpResponse response = httpClient.execute(new HttpGet(urlString));

            // Save response to file.
            InputStream inputStream = response.getEntity().getContent();
            UUID uuid = UUID.fromString(_contentValues.getAsString(FormConstants.UUID));

            _docxFile = FileSystemHelper.getEventForm(_context, uuid);
            if (_docxFile.exists()) {
                _docxFile.delete();
            }
            OutputStream outputStream = new FileOutputStream(_docxFile);

            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    protected void onPostExecute(Void result) {
        if (_listener!=null) {
            _listener.onTaskFinish(0, _docxFile);
        }

    }
}
