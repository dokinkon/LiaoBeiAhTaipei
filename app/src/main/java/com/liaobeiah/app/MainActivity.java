package com.liaobeiah.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity
        implements MainFragment.Callbacks, RequestFormTask.Listener {

    private static int REQUEST_MAKE_FORM = 23;
    private static int DIALOG_DELETE_FORM_CONFIRM = 44;
    private static final int DIALOG_SHOW_RULE = 45;

    private static String TAG = "MainActivity";
    private static String TAG_MAIN_FRAGMENT = "MainFragment";
    private static String FIRST_LAUNCH_KEY = "FirstLaunchKey";

    //private static final String username = "dokinkon@gmail.com";
    //private static final String password = "qpnlnlrpkcznillt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains(FIRST_LAUNCH_KEY)) {
            showDialog(DIALOG_SHOW_RULE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRST_LAUNCH_KEY, true);
            editor.commit();
        }

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

        // TODO
        // Remove this.
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

    private void scheduleTaskQueue(ContentValues contentValues) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, BackgroundService.class);

        intent.putExtra(BackgroundService.FORM_UUID, contentValues.getAsString(FormConstants.UUID));
        intent.putExtra(BackgroundService.MY_NAME, preferences.getString("pref_name", ""));
        intent.putExtra(BackgroundService.MY_ADDRESS, preferences.getString("pref_address", ""));
        intent.putExtra(BackgroundService.MY_PHONE, preferences.getString("pref_phone", ""));
        intent.putExtra(BackgroundService.MY_MAIL, preferences.getString("pref_email_address", ""));
        intent.putExtra(BackgroundService.MY_MAIL_PWD, preferences.getString("pref_email_password", ""));
        intent.putExtra(BackgroundService.EVENT_DATE, contentValues.getAsString(FormConstants.DATE));
        intent.putExtra(BackgroundService.EVENT_TIME, contentValues.getAsString(FormConstants.TIME));
        intent.putExtra(BackgroundService.EVENT_TYPE, contentValues.getAsString(FormConstants.REASON));
        intent.putExtra(BackgroundService.EVENT_LOCATION, contentValues.getAsString(FormConstants.LOCATION));
        intent.putExtra(BackgroundService.EVENT_COMMENT, contentValues.getAsString(FormConstants.COMMENT));
        intent.putExtra(BackgroundService.VEHICLE_LICENSE, contentValues.getAsString(FormConstants.VEHICLE_LICENSE));

        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if ( requestCode == REQUEST_MAKE_FORM) {
            if ( resultCode == MakeFormActivity.RESULT_SUBMIT ) {

                ContentValues contentValues = data.getParcelableExtra(FormConstants.CONTENT_VALUE);
                MainFragment fragment = (MainFragment)getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
                fragment.insertForm(contentValues);
                scheduleTaskQueue(contentValues);

            } else if ( resultCode == MakeFormActivity.RESULT_SAVE_DRAFT ) {
                ContentValues contentValues = data.getParcelableExtra(FormConstants.CONTENT_VALUE);
                MainFragment fragment = (MainFragment)getSupportFragmentManager().findFragmentByTag(TAG_MAIN_FRAGMENT);
                fragment.insertForm(contentValues);

            } else if ( resultCode == RESULT_CANCELED ) {
                Toast.makeText(getBaseContext(), "你人真好!", Toast.LENGTH_SHORT).show();
            }

        }
    }

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
        } else if ( id == DIALOG_SHOW_RULE ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("歡迎使用交通糾察");
            builder.setMessage("依\"道路交通管理事件規定\"，" +
                    "警方需要檢舉人的聯絡方式。\n\n" +
                    "另外此APP需要透過Gmail來寄送違規事証給警方，因此需要您提供Gmail帳號及密碼。\n");

            builder.setPositiveButton("前往設定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);

                }
            });

            return builder.create();
        }
        return super.onCreateDialog(id, savedInstanced);
    }


}
