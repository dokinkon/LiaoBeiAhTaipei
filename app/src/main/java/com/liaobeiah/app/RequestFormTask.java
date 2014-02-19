package com.liaobeiah.app;

import android.app.ProgressDialog;
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
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by dokinkon on 2/10/14.
 */

public class RequestFormTask extends AsyncTask<Bundle, Void, Void> {



    public interface Listener {
        public void onTaskFinish(int resultCode, ContentValues contentValues);
    }

    private Context _context;
    private SharedPreferences _preferences;
    private ContentValues _contentValues;
    private Listener _listener;

    private static final String TAG = "RequestFormTask";
    private ProgressDialog progressDialog;

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
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(_context, "Please Wait", "Sending...");
    }


    @Override
    protected Void doInBackground(Bundle... extras) {

        requestForm();
        sendMail();

        return null;
    }


    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        progressDialog.dismiss();
        progressDialog = null;
        if (_listener!=null) {
            _listener.onTaskFinish(0, _contentValues);
        }

    }

    private void requestForm() {
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

            File file = FileSystemHelper.getEventForm(_context, uuid);
            if (file.exists()) {
                file.delete();
            }
            OutputStream outputStream = new FileOutputStream(file);

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
    }

    private void sendMail() {

        try {
            Message message = createMessage(createSessionObject(), _contentValues);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private Message createMessage(Session session, ContentValues contentValues)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("dokinkon@gmail.com", "Chao-Chih Lin"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("dokinkon@gmail.com", "dokinkon@gmail.com"));
        message.setSubject("TEST2");

        Multipart multipart = new MimeMultipart();


        // 1. Set message body part.
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        try {
            messageBodyPart.setText("敬愛的警官\n\n" + "我要檢舉交通違規，請見附加檔案。\n");
            multipart.addBodyPart(messageBodyPart);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        // 2. Add Event Pictures.
        UUID uuid = UUID.fromString(contentValues.getAsString(FormConstants.UUID));

        int index;
        for (index = 0;index < 3;index++) {

            File file = FileSystemHelper.getEventPicture(_context, uuid, index);
            if (file.exists()) {
                FileDataSource fileDataSource = new FileDataSource(file);
                MimeBodyPart mimeBodyPart = new MimeBodyPart();
                try {
                    mimeBodyPart.setDataHandler(new DataHandler(fileDataSource));
                    mimeBodyPart.setFileName(fileDataSource.getName());
                    multipart.addBodyPart(mimeBodyPart);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }

            }
        }

        // 3. Add Form.docx
        File file = FileSystemHelper.getEventForm(_context, uuid);
        FileDataSource fileDataSource = new FileDataSource(file);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        try {
            mimeBodyPart.setDataHandler(new DataHandler(fileDataSource));
            mimeBodyPart.setFileName(fileDataSource.getName());
            multipart.addBodyPart(mimeBodyPart);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        try {
            message.setContent(multipart);
        } catch (MessagingException e) {
            e.printStackTrace();
        }


        return message;
    }

    private Session createSessionObject() {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.debug", "true");
        properties.put("mail.debug", "true");

        //SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = _preferences.getString("pref_email_address", "");
        final String password = _preferences.getString("pref_email_password", "");

        return Session.getInstance(properties, new Authenticator(){

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

}



























