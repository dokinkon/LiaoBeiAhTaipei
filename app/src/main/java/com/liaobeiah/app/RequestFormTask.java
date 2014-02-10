package com.liaobeiah.app;

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

/**
 * Created by dokinkon on 2/10/14.
 */
public class RequestFormTask extends AsyncTask<Bundle, Void, Void> {


    public interface Listener {
        public void onTaskFinish(int resultCode, File docxFile);
    }

    private Context _context;
    private SharedPreferences _preferences;
    private Bundle _extra;
    private Listener _listener;
    private File _docxFile;
    private static final String TAG = "RequestFormTask";

    RequestFormTask(Context context, Bundle extra) {
        _context = context;
        if (context==null) {
            throw new NullPointerException();
        }
        _preferences = PreferenceManager.getDefaultSharedPreferences(_context);
        _extra = extra;
    }

    RequestFormTask(Context context, Bundle extra, Listener listener) {
        _context = context;
        if (context==null) {
            throw new NullPointerException();
        }
        _preferences = PreferenceManager.getDefaultSharedPreferences(_context);
        _extra = extra;
        _listener = listener;
    }


    @Override
    protected Void doInBackground(Bundle... extras) {

        HttpClient httpClient = new DefaultHttpClient();

        try {

            // Build query parameters..
            List<NameValuePair> params = new LinkedList<NameValuePair>();

            String name = _preferences.getString("reporter_name", "");
            String address = _preferences.getString("address", "");
            String phone = _preferences.getString("phone", "");
            String email = _preferences.getString("email", "");
            String date = _extra.getString(FormConstants.DATE);
            String time = _extra.getString(FormConstants.TIME);
            String license = _extra.getString(FormConstants.VEHICLE_LICENSE);
            String eventLocation = _extra.getString(FormConstants.LOCATION);
            String eventType = _extra.getString(FormConstants.REASON);
            String comment = _extra.getString(FormConstants.COMMENT);

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



            params.add(new BasicNameValuePair("name", _preferences.getString("reporter_name", "")));
            params.add(new BasicNameValuePair("address", _preferences.getString("address", "")));
            params.add(new BasicNameValuePair("phone", _preferences.getString("phone", "")));
            params.add(new BasicNameValuePair("email", _preferences.getString("email", "")));
            params.add(new BasicNameValuePair("date", _extra.getString(FormConstants.DATE)));
            params.add(new BasicNameValuePair("time", _extra.getString(FormConstants.TIME)));
            params.add(new BasicNameValuePair("license", _extra.getString(FormConstants.VEHICLE_LICENSE)));
            params.add(new BasicNameValuePair("event-location", _extra.getString(FormConstants.LOCATION)));
            params.add(new BasicNameValuePair("event-type", _extra.getString(FormConstants.REASON)));
            params.add(new BasicNameValuePair("comment", _extra.getString(FormConstants.REASON)));

            String paramString = URLEncodedUtils.format(params, "utf-8");
            String urlString = "http://192.168.1.106:8080/hello.docx?" + paramString;
            HttpResponse response = httpClient.execute(new HttpGet(urlString));

            // Save response to file.
            InputStream inputStream = response.getEntity().getContent();


            File rootDir = _context.getExternalFilesDir(null);
            _docxFile = new File(rootDir, "test.docx");
            if (_docxFile.exists()) {
                _docxFile.delete();
            }
            OutputStream outputStream = new FileOutputStream(_docxFile);

            int read = 0;
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
