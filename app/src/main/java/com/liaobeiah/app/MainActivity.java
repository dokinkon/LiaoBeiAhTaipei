package com.liaobeiah.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
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

public class MainActivity extends ActionBarActivity
        implements MainFragment.Callbacks, RequestFormTask.Listener {

    private static int REQUEST_MAKE_FORM = 23;
    private static int DIALOG_DELETE_FORM_CONFIRM = 44;
    private static String TAG = "MainActivity";
    private static String TAG_MAIN_FRAGMENT = "MainFragment";
    private ProgressDialog _progressDialog;

    private static final String username = "dokinkon@gmail.com";
    private static final String password = "qpnlnlrpkcznillt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment(), TAG_MAIN_FRAGMENT)
                    .commit();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onItemSelected(String id) {

    }

    @Override
    public void onTaskFinish(int resultCode, ContentValues contentValues) {
        if (_progressDialog!= null) {
            _progressDialog.dismiss();
            _progressDialog = null;
        }
        if (resultCode == 0) {
            sendMail(contentValues);
            //submit(contentValues);
        }
    }

    class ItemOperationListener implements DialogInterface.OnClickListener {

        private long _rowId;

        ItemOperationListener(long rowId) {
            _rowId = rowId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if ( which == 0) {
                viewOfEditForm(_rowId);

            } else if (which == 1) {
                //showDialog(DIALOG_DELETE_FORM_CONFIRM);
                MainFragment fragment = (MainFragment)getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
                fragment.deleteForm(_rowId);
            }
        }
    }

    @Override
    public void onItemClicked(AdapterView<?> parent, View view, int position, long id) {

        DatabaseHelper helper = new DatabaseHelper(this);
        SQLiteDatabase database = helper.getReadableDatabase();
        Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME + " where "
                + FormConstants._ID + " = " + id, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(FormConstants.STATE);
        int state = cursor.getInt(columnIndex);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (state == FormConstants.STATE_FINISH) {
            builder.setItems(R.array.finish_item_options, new ItemOperationListener(id));
        } else {
            builder.setItems(R.array.draft_item_options, new ItemOperationListener(id));
        }




        builder.show();
    }

    @Override
    public void onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_howto) {

            Intent intent = new Intent();
            intent.setClass(MainActivity.this, HowToActivity.class);
            startActivity(intent);
            return true;

            /*
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction ft = fragmentManager.beginTransaction();

            ft.replace(R.id.container, new ItemListFragment());
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
            return true;
            */


        } else if ( id == R.id.action_add_form) {

            addForm();
            return true;
        } else if (id == R.id.action_remove_database) {
            if (deleteDatabase("com.liaobeiah.form") ) {
                MainFragment fragment = (MainFragment)getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
                fragment.deleteAllEvents();

                //FileSystemHelper.deleteAllEvents(this);
                Toast.makeText(this, "移除資料庫成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "移除資料庫失敗", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void addForm() {
        Intent detailIntent = new Intent(this, MakeFormActivity.class);
        startActivityForResult(detailIntent, REQUEST_MAKE_FORM);
    }

    private void viewOfEditForm(long formId) {
        Intent detailIntent = new Intent(this, MakeFormActivity.class);
        detailIntent.putExtra("FormID", formId);
        startActivityForResult(detailIntent, REQUEST_MAKE_FORM);

    }



    public void onClick(View view) {
        if (view.getId() == R.id.textViewWelcom) {
            addForm();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if ( requestCode == REQUEST_MAKE_FORM) {
            if ( resultCode == MakeFormActivity.RESULT_SUBMIT ) {

                ContentValues contentValues = data.getParcelableExtra(FormConstants.CONTENT_VALUE);
                MainFragment fragment = (MainFragment)getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
                fragment.insertForm(contentValues);


                // start request for making form.
                _progressDialog = new ProgressDialog(this);
                _progressDialog.setMessage("產生檢舉表單中...");
                _progressDialog.show();
                new RequestFormTask(this, contentValues, this).execute();
            } else if ( resultCode == MakeFormActivity.RESULT_SAVE_DRAFT ) {
                ContentValues contentValues = data.getParcelableExtra(FormConstants.CONTENT_VALUE);
                MainFragment fragment = (MainFragment)getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
                fragment.insertForm(contentValues);

            } else if ( resultCode == RESULT_CANCELED ) {
                Toast.makeText(getBaseContext(), "你人真好!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    /*
    private void submit( ContentValues contentValues ) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dokinkon@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_CC, sharedPreferences.getString("pref_email", ""));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[違規檢舉] 車號: "
                + contentValues.getAsString(FormConstants.VEHICLE_LICENSE));

        String textContent = "敬愛的警官\n\n" + "我要檢舉交通違規，請見附加檔案。\n";

        emailIntent.putExtra(Intent.EXTRA_TEXT, textContent);

        // Android multiple email attachments using intent
        // http://stackoverflow.com/questions/2264622/android-multiple-email-attachments-using-intent
        ArrayList<Uri> uris = new ArrayList<Uri>();

        // Append Photos...
        UUID uuid = UUID.fromString(contentValues.getAsString(FormConstants.UUID));
        int index;
        for (index = 0;index < 3;index++) {

            File file = FileSystemHelper.getEventPicture(this, uuid, index);
            if (file.exists()) {
                Uri uri = Uri.fromFile(file);
                uris.add(uri);
            }
        }

        // Append Form...
        File file = FileSystemHelper.getEventForm(this, uuid);
        if (file.exists()) {
            Uri uri = Uri.fromFile(file);
            uris.add(uri);
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        try {
            startActivity(Intent.createChooser(emailIntent, "透過電子信箱傳送..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }*/

    private void sendMail(ContentValues contentValues) {
        Session session = createSessionObject();
        try {
            Message message = createMessage(session, contentValues);
            new SendMailTask().execute(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    /*
    private void sendMail(String email, String subject, String messageBody) {
        Session session = createSessionObject();

        try {
            Message message = createMessage(email, subject, messageBody, session);
            new SendMailTask().execute(message);
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }*/

    private Session createSessionObject() {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.debug", "true");
        properties.put("mail.debug", "true");

        return Session.getInstance(properties, new Authenticator(){
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
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


        return message;
    }

    /*
    private Message createMessage(String email, String subject, String messageBody, Session session)
        throws MessagingException, UnsupportedEncodingException {

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("dokinkon@gmail.com", "Chao-Chih Lin"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
        message.setSubject(subject);
        message.setText(messageBody);

        return message;
    }*/


    private class SendMailTask extends AsyncTask<Message, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait", "Sending...");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();

        }

        @Override
        protected Void doInBackground(Message...messages) {
            try {

                Transport.send(messages[0]);

            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    /*
    @Override
    protected Dialog onCreateDialog(int id, Bundle savedInstanced) {
        if (id == DIALOG_DELETE_FORM_CONFIRM) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("確定刪除此筆資料？")
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();

                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            dialog.dismiss();

                        }
                    });

            // Create the AlertDialog object and return it
            return builder.create();
        }
        return super.onCreateDialog(id, savedInstanced);
    }
    */

}
