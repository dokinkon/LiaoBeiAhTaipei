package com.liaobeiah.app;

import android.app.AlertDialog;
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

public class MainActivity extends ActionBarActivity
        implements ItemListFragment.Callbacks, RequestFormTask.Listener {


    private static int REQUEST_MAKE_FORM = 23;
    private static String TAG = "MainActivity";

    private DatabaseHelper _databaseHelper;
    private ItemListFragment _itemListFragment;
    private PlaceholderFragment _placeHolderFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FIXME
        //File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //Toast.makeText(this, path.getAbsolutePath(), Toast.LENGTH_LONG).show();

        if (savedInstanceState == null) {

            _databaseHelper = new DatabaseHelper(this);
            SQLiteDatabase database = _databaseHelper.getReadableDatabase();
            _itemListFragment = new ItemListFragment();
            _placeHolderFragment = new PlaceholderFragment();
            //refreshContent();

            if (database.isOpen()) {
                Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
                Log.i(TAG, "QUERY OK :" + cursor.getCount());

                if ( cursor.getCount() > 0 ) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.container, _itemListFragment)
                            .commit();

                } else {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.container, new PlaceholderFragment())
                            .commit();

                }

            } else {
                Log.e(TAG, "Database not open!");
            }
        }

    }

    private void refreshContent() {
        SQLiteDatabase database = _databaseHelper.getReadableDatabase();

        if (database.isOpen()) {
            Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
            Log.i(TAG, "QUERY OK :" + cursor.getCount());

            if ( cursor.getCount() > 0 ) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, _itemListFragment)
                        .commitAllowingStateLoss();

                _itemListFragment.refresh();

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
    public void onTaskFinish(int resultCode, File docxFile) {
        if (resultCode == 0) {
            submitFormViaMail(new Bundle());
        }
    }

    class ItemOperationListener implements DialogInterface.OnClickListener {
        private DatabaseHelper _databaseHelper;
        private long _rowId;


        ItemOperationListener(DatabaseHelper databaseHelper, long rowId) {
            _databaseHelper = databaseHelper;
            _rowId = rowId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if ( which == 0) {

            } else if (which == 1) {
                // try to remove item from database...
                SQLiteDatabase database = _databaseHelper.getWritableDatabase();
                if ( database.isOpen()) {

                    // delete reference pictures
                    Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME + " where "
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

                    database.delete(FormConstants.TABLE_NAME, FormConstants._ID + " = " + _rowId, null);
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


    private boolean insertOrUpdateToDatabase(Bundle bundle) {

        ContentValues contentValues = new ContentValues();
        contentValues.put(FormConstants.STATE, bundle.getInt(FormConstants.STATE, 0));
        contentValues.put(FormConstants.PIC_URI_1, bundle.getString(FormConstants.PIC_URI_1));
        contentValues.put(FormConstants.PIC_URI_2, bundle.getString(FormConstants.PIC_URI_2));
        contentValues.put(FormConstants.PIC_URI_3, bundle.getString(FormConstants.PIC_URI_2));
        contentValues.put(FormConstants.DATE, bundle.getString(FormConstants.DATE));
        contentValues.put(FormConstants.TIME, bundle.getString(FormConstants.TIME));
        contentValues.put(FormConstants.REASON, bundle.getString(FormConstants.REASON));
        contentValues.put(FormConstants.VEHICLE_LICENSE, bundle.getString(FormConstants.VEHICLE_LICENSE));
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        long row = database.insert(FormConstants.TABLE_NAME, null, contentValues);

        Toast.makeText(this, "ROW = " + row, Toast.LENGTH_SHORT).show();
        refreshContent();

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if ( requestCode == REQUEST_MAKE_FORM) {
            if ( resultCode == RESULT_OK ) {

                insertOrUpdateToDatabase(data.getExtras());

                // start request for making form.
                new RequestFormTask(this, new Bundle(data.getExtras()), this).execute();


                //submitFormViaMail(data.getExtras());
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


    private void submitFormViaMail( Bundle extras ) {



        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        new RequestFormTask(this, extras).execute();
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dokinkon@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_CC, sharedPreferences.getString("key_email", ""));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[違規檢舉] 車號: "
                + extras.getString(FormConstants.VEHICLE_LICENSE));

        String textContent = "敬愛的警官\n\n" + "我要檢舉交通違規\n" +
                "違規日期:" + extras.getString(FormConstants.DATE) + "\n" +
                "違規時間:" + extras.getString(FormConstants.TIME) + "\n" +
                "違規車牌:" + extras.getString(FormConstants.VEHICLE_LICENSE) + "\n"+
                "違規地點:" + extras.getString(FormConstants.LOCATION) + "\n" +
                "違規事由:" + extras.getString(FormConstants.REASON) + "\n" +
                "補充說明:" + extras.getString(FormConstants.COMMENT) + "\n\n" +
                "檢舉人姓名:" + sharedPreferences.getString("reporter_name", "") + "\n" +
                "聯絡電話:" + sharedPreferences.getString("phone", "") + "\n" +
                "聯絡地址:" + sharedPreferences.getString("address", "");

        emailIntent.putExtra(Intent.EXTRA_TEXT, textContent);

        // Android multiple email attachments using intent
        // http://stackoverflow.com/questions/2264622/android-multiple-email-attachments-using-intent
        ArrayList<Uri> uris = new ArrayList<Uri>();
        String[] filePaths = new String[3];
        filePaths[0] = extras.getString(FormConstants.PIC_URI_1);
        filePaths[1] = extras.getString(FormConstants.PIC_URI_2);
        filePaths[2] = extras.getString(FormConstants.PIC_URI_3);

        int index = 0;
        for (index = 0;index < 3;index++) {

            if ( filePaths[index] != null && filePaths[index] !="" ) {
                File attachmentFile = new File(filePaths[index]);
                Uri uri = Uri.fromFile(attachmentFile);
                uris.add(uri);
            }
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        try {
            startActivity(Intent.createChooser(emailIntent, "透過電子信箱傳送..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

}
