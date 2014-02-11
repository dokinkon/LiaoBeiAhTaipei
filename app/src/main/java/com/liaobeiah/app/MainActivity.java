package com.liaobeiah.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends ActionBarActivity
        implements ItemListFragment.Callbacks, RequestFormTask.Listener {


    private static int REQUEST_MAKE_FORM = 23;
    private static String TAG = "MainActivity";

    private DatabaseHelper _databaseHelper;
    private SQLiteDatabase _database;
    private ItemListFragment _itemListFragment;
    private PlaceholderFragment _placeHolderFragment;
    private ProgressDialog _progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);


        if (savedInstanceState == null) {


        }


        _databaseHelper = new DatabaseHelper(this);
        _database = _databaseHelper.getWritableDatabase();
        _itemListFragment = new ItemListFragment();
        _placeHolderFragment = new PlaceholderFragment();

        if (_database.isOpen()) {
            Cursor cursor = _database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
            Log.i(TAG, "QUERY OK :" + cursor.getCount());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, _itemListFragment)
                    .commit();

            if ( cursor.getCount() == 0 ) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new PlaceholderFragment())
                        .commit();
            }

        } else {
            Log.e(TAG, "Database not open!");
        }

    }

    private void refreshContent() {
        if (_database.isOpen()) {
            Cursor cursor = _database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
            Log.i(TAG, "QUERY OK :" + cursor.getCount());

            if ( cursor.getCount() > 0 ) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, _itemListFragment)
                        .commitAllowingStateLoss();

            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, _placeHolderFragment)
                        .commitAllowingStateLoss();

            }

        } else {
            Log.e(TAG, "Database not open!");
        }
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
            submit(contentValues);
        }
    }

    class ItemOperationListener implements DialogInterface.OnClickListener {
        //private DatabaseHelper _databaseHelper;
        private long _rowId;


        ItemOperationListener(DatabaseHelper databaseHelper, long rowId) {
            //_databaseHelper = databaseHelper;
            _rowId = rowId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if ( which == 0) {

            } else if (which == 1) {
                // try to remove item from database...
                //SQLiteDatabase database = _databaseHelper.getWritableDatabase();
                if ( _database.isOpen()) {

                    // delete reference pictures
                    Cursor cursor = _database.rawQuery("select * from " + FormConstants.TABLE_NAME + " where "
                            + FormConstants._ID + " = " + _rowId, null);

                    cursor.moveToFirst();

                    String[] picFields = new String[3];
                    picFields[0] = FormConstants.PIC_URI_1;
                    picFields[1] = FormConstants.PIC_URI_2;
                    picFields[2] = FormConstants.PIC_URI_3;

                    int i = 0;
                    for (i=0;i<3;i++) {
                        int index = cursor.getColumnIndex(picFields[i]);
                        String filePath = cursor.getString(index);
                        if (filePath != null) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                file.delete();
                            }
                        }
                    }

                    _database.delete(FormConstants.TABLE_NAME, FormConstants._ID + " = " + _rowId, null);
                    refreshContent();

                } else {
                    Log.e(TAG, "Failed to open databse!");

                }
            }
        }
    }

    @Override
    public void onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {

        //Toast.makeText(this, "POS:" + pos + " id:" + id, Toast.LENGTH_SHORT).show();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(R.array.form_item_options, new ItemOperationListener(_databaseHelper, id));
        builder.show();
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
                clearPictureFolder();
                Toast.makeText(this, "移除資料庫成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "移除資料庫失敗", Toast.LENGTH_SHORT).show();
            }
            refreshContent();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void clearPictureFolder() {

        File picDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] files = picDir.listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    private void addForm() {
        Intent detailIntent = new Intent(this, MakeFormActivity.class);
        detailIntent.putExtra(MakeFormFragment.ARG_ITEM_ID, 0);
        startActivityForResult(detailIntent, REQUEST_MAKE_FORM);
    }

    public void onClick(View view) {
        if (view.getId() == R.id.textViewWelcom) {
            addForm();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void insertOrUpdateToDatabase(ContentValues contentValues) {
        _database.insert(FormConstants.TABLE_NAME, null, contentValues);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if ( requestCode == REQUEST_MAKE_FORM) {
            if ( resultCode == RESULT_OK ) {

                ContentValues contentValues = data.getParcelableExtra(FormConstants.CONTENT_VALUE);
                insertOrUpdateToDatabase(contentValues);

                Cursor cursor = _database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
                _itemListFragment.changeCursor(cursor);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, _itemListFragment)
                        .commitAllowingStateLoss ();

                // start request for making form.
                _progressDialog = new ProgressDialog(this);
                _progressDialog.setMessage("Please Wait");
                _progressDialog.show();
                new RequestFormTask(this, contentValues, this).execute();
            } else {
                Toast.makeText(getBaseContext(), "你人真好!", Toast.LENGTH_LONG).show();

            }
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


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
    }

}
