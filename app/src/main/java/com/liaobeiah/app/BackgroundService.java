package com.liaobeiah.app;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Handler;
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

//import java.util.logging.Handler;

/**
 * Created by dokinkon on 2/19/14.
 */
public class BackgroundService extends IntentService {

    public static final String FORM_UUID = "Uuid";
    public static final String MY_NAME = "MyName";
    public static final String MY_ADDRESS = "MyAddress";
    public static final String MY_PHONE = "MyPhone";
    public static final String MY_MAIL = "MyMail";
    public static final String MY_MAIL_PWD = "MyMailPassword";

    public static final String VEHICLE_LICENSE = "VehicleLicense";
    public static final String EVENT_DATE = "EventDate";
    public static final String EVENT_TIME = "EventTime";
    public static final String EVENT_TYPE = "EventType";
    public static final String EVENT_LOCATION = "EventLocation";
    public static final String EVENT_COMMENT = "EventComment";

    private static final String TAG = "BackgroundService";

    private static final int MSG_REQ_START = 0;
    private static final int MSG_REQ_CANCEL = 1;
    private static final int MSG_REQ_FINISH = 2;
    private Handler _handler;

    private int _requestId = 0;



    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public BackgroundService() {
        super("BackgroundSerivice");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        _requestId = 0;
        _handler = new Handler() {
            public void handleMessage(android.os.Message message) {
                switch (message.what) {
                    case MSG_REQ_START:
                        notificateRequestStart(message.arg1);
                        break;
                    case MSG_REQ_FINISH:
                        notificateRequestFinish(message.arg1);
                        break;
                }

            }
        };
    }

    private void notificateRequestStart(int requestId) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("交通糾察")
                .setContentText("上傳檢舉表單中...")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .build();

        notificationManager.notify(requestId, notification);
    }

    private void notificateRequestFinish(int requestId) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("交通糾察")
                .setContentText("上傳檢舉表單成功")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(false)
                .build();

        notificationManager.notify(requestId, notification);
    }




    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");

        _requestId++;

        android.os.Message message = new android.os.Message();
        message.what = MSG_REQ_START;
        message.arg1 = _requestId;
        _handler.sendMessage(message);

        downloadForm(intent);

        try {
            sendForm(intent);
            message = new android.os.Message();
            message.what = MSG_REQ_FINISH;
            message.arg1 = _requestId;
            _handler.sendMessage(message);

            Log.i(TAG, "Send OK!");
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


    }

    private void downloadForm(Intent intent) {
        HttpClient httpClient = new DefaultHttpClient();

        try {

            // Build query parameters..
            List<NameValuePair> params = new LinkedList<NameValuePair>();

            String name = intent.getStringExtra(MY_NAME);
            String address = intent.getStringExtra(MY_ADDRESS);
            String phone = intent.getStringExtra(MY_PHONE);
            String email = intent.getStringExtra(MY_MAIL);
            String date = intent.getStringExtra(EVENT_DATE);
            String time = intent.getStringExtra(EVENT_TIME);
            String license = intent.getStringExtra(VEHICLE_LICENSE);
            String eventLocation = intent.getStringExtra(EVENT_LOCATION);
            String eventType = intent.getStringExtra(EVENT_TYPE);
            String comment = intent.getStringExtra(EVENT_COMMENT);

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
            UUID uuid = UUID.fromString(intent.getStringExtra(FORM_UUID));

            File file = FileSystemHelper.getEventForm(this, uuid);
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

    private void sendForm(Intent intent) throws UnsupportedEncodingException, MessagingException {

        // 1. create session object.
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.debug", "true");
        //properties.put("mail.debug", "true");

        //SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = intent.getStringExtra(MY_MAIL); //_preference.getString("pref_email_address", "");
        final String password = intent.getStringExtra(MY_MAIL_PWD); //_preferences.getString("pref_email_password", "");

        Session session = Session.getInstance(properties, new Authenticator(){

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // 2. create message.
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
        UUID uuid = UUID.fromString(intent.getStringExtra(FORM_UUID));

        int index;
        for (index = 0;index < 3;index++) {

            File file = FileSystemHelper.getEventPicture(this, uuid, index);
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
        File file = FileSystemHelper.getEventForm(this, uuid);
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

        Transport.send(message);
    }
}
