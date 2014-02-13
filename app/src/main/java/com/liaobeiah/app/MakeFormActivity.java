package com.liaobeiah.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * An activity representing a single Item detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link MainActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link MakeFormFragment}.
 */
public class MakeFormActivity extends ActionBarActivity
        implements LocationListener{

    public static final int RESULT_VIEW_ONLY = 77;
    public static final int RESULT_SAVE_DRAFT = 78;
    public static final int RESULT_SUBMIT = 79;

    private static int CAMERA_RESULT = 25;
    private static int GALLERY_RESULT = 26;


    private static int DIALOG_UNFINISHED_CONFIRM = 1;
    private static int DIALOG_SUBMIT_CONFIRM = 2;

    private static String CONTENT_VALUES = "ContentValues";

    private static String TAG = "MakeFormActivity";
    private static String MAKE_FORM_FRAGMENT_TAG = "MakeFormFragmentTag";

    private ContentValues _contentValues;


    private int _currentImageIndex;


    private LocationManager _locationManager;
    private MakeFormFragment _makeFormFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        Log.i(TAG, "onCreate");

        _locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        _currentImageIndex = -1;

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            _makeFormFragment = new MakeFormFragment();
            //_formUuid = UUID.randomUUID();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, _makeFormFragment, MAKE_FORM_FRAGMENT_TAG)
                    .commit();


            if (getIntent().hasExtra("FormID")) {
                // User may edit/view form

                long formId = getIntent().getLongExtra("FormID", 0);
                DatabaseHelper helper = new DatabaseHelper(this);
                SQLiteDatabase database = helper.getReadableDatabase();
                Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME + " where "
                        + FormConstants._ID + " = " + formId, null);

                cursor.moveToFirst();
                _contentValues = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, _contentValues);

            } else {
                _contentValues = new ContentValues();
                _contentValues.put(FormConstants.UUID, UUID.randomUUID().toString());
                _contentValues.put(FormConstants.STATE, FormConstants.STATE_DRAFT);
            }

        } else {
            _makeFormFragment = (MakeFormFragment)getSupportFragmentManager().findFragmentByTag(MAKE_FORM_FRAGMENT_TAG);
            _contentValues = savedInstanceState.getParcelable(CONTENT_VALUES);
        }

        actionBarSetup();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionBarSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActionBar();

            if (isDraftState()) {
                ab.setTitle("編輯檢舉表單");
            } else {
                ab.setTitle("檢視[唯讀]");
            }
        }
    }

    private boolean isDraftState() {
        return _contentValues.getAsInteger(FormConstants.STATE) == FormConstants.STATE_DRAFT;
    }

    protected void onStart() {
        super.onStart();
        _makeFormFragment.restoreUIState(_contentValues);
    }

    protected void onResume () {
        super.onResume();
        if ( isDraftState() ) {
            _locationManager.requestSingleUpdate(new Criteria(), this, null);
        }
    }

    protected void onPause() {
        super.onPause();
        _locationManager.removeUpdates(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int state = _contentValues.getAsInteger(FormConstants.STATE);
        if (state == FormConstants.STATE_DRAFT) {
            getMenuInflater().inflate(R.menu.edit_form_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.view_form_menu, menu);
        }

        return true;
    }

    private void leave() {
        int state = _contentValues.getAsInteger(FormConstants.STATE);
        if (state == FormConstants.STATE_DRAFT) {
            showDialog(DIALOG_UNFINISHED_CONFIRM);
        } else {
            setResult(RESULT_VIEW_ONLY);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            leave();
            return true;
        } else if ( id == R.id.action_submit) {
            if (validateFields()) {
                showDialog(DIALOG_SUBMIT_CONFIRM);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        leave();
    }


    @Override
    public void onLocationChanged(Location location) {

        Geocoder gc = new Geocoder(this, Locale.TRADITIONAL_CHINESE);
        try {
            List<Address> lstAddress = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String returnAddress =lstAddress.get(0).getAddressLine(0);

            EditText editText = (EditText)findViewById(R.id.editTextLocation);
            editText.setText(returnAddress);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        collectFileds();
        outState.putParcelable(CONTENT_VALUES, _contentValues);
    }


    private void doAbortEditing() {
        Log.i(TAG, "doAbortEditing");
        // 1. remove pictures
        // FIXME!

    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle savedInstanced) {
        if (id == DIALOG_UNFINISHED_CONFIRM) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("檢舉表單尚未完成")
                    .setPositiveButton("存成草稿", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            saveAsDraft();
                        }
                    })
                    .setNegativeButton("饒他一馬", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            doAbortEditing();
                            dialog.dismiss();
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    })
                    .setNeutralButton("繼續編輯", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        } else if (id == DIALOG_SUBMIT_CONFIRM) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("所有欄位都正確無誤？")
                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();

                            submit();

                        }
                    })
                    .setNegativeButton("否", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();

                        }
                    });

            // Create the AlertDialog object and return it
            return builder.create();
        }
        return super.onCreateDialog(id, savedInstanced);
    }

    private void dispatchTakePictureIntent(int photoIndex) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go

            UUID uuid = UUID.fromString(_contentValues.getAsString(FormConstants.UUID));
            File photoFile = FileSystemHelper.getEventPicture(this, uuid, photoIndex);

            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, CAMERA_RESULT);
            }
        }
    }

    public void onImageViewClicked(View view){

        if (view.getId() == R.id.imageView1) {
            _currentImageIndex = 0;
        } else if (view.getId() == R.id.imageView2) {
            _currentImageIndex = 1;
        } else if (view.getId() == R.id.imageView3) {
            _currentImageIndex = 2;
        } else {
            _currentImageIndex = -1;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(R.array.test, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {


                if ( which == 0 ) {

                    Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, GALLERY_RESULT);
                } else {
                    dispatchTakePictureIntent(_currentImageIndex);
                }
            }
        });

        builder.show();
    }

    private boolean validateFields() {

        UUID uuid = UUID.fromString(_contentValues.getAsString(FormConstants.UUID));
        // 1. check picture
        File file = FileSystemHelper.getEventPicture(this, uuid, 0);
        if (!file.exists()) {
            Toast.makeText(this, "請提供違規照片", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 2. validate address field
        EditText editText = (EditText)findViewById(R.id.editTextLocation);
        String s = editText.getText().toString();
        if ( s ==null || s.equals("")) {
            Toast.makeText(this, "請輸入違規地點", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void collectFileds() {
        // Parse Event Location...
        EditText location = (EditText)findViewById(R.id.editTextLocation);
        _contentValues.put(FormConstants.LOCATION, location.getText().toString());

        // Parse Date value.
        EditText dateView = (EditText)findViewById(R.id.editTextDate);
        _contentValues.put(FormConstants.DATE, dateView.getText().toString());

        // Parse Time value
        EditText timeView = (EditText)findViewById(R.id.editTextTime);
        _contentValues.put(FormConstants.TIME, timeView.getText().toString());

        // Parse Reason field
        Spinner spinner = (Spinner)findViewById(R.id.spinner_reason);
        _contentValues.put(FormConstants.REASON, spinner.getSelectedItem().toString());

        // Parse Comment Field
        EditText comment = (EditText)findViewById(R.id.editTextComment);
        _contentValues.put(FormConstants.COMMENT, comment.getText().toString());

        // Parse Vehicle License field
        EditText editText = (EditText)findViewById(R.id.editTextVehicleLicense);
        _contentValues.put(FormConstants.VEHICLE_LICENSE, editText.getText().toString());

        // Parse Police Mail from Spinner...
        spinner = (Spinner)findViewById(R.id.spinner_receiver);
        _contentValues.put(FormConstants.RECEIVER, spinner.getSelectedItem().toString());
    }

    private void saveAsDraft() {
        collectFileds();
        _contentValues.put(FormConstants.STATE, FormConstants.STATE_DRAFT);
        Intent intent = new Intent();
        intent.putExtra(FormConstants.CONTENT_VALUE, _contentValues);

        setResult(RESULT_SAVE_DRAFT, intent);
        finish();
    }

    private void submit() {

        Log.i(TAG, "submit");
        collectFileds();

        _contentValues.put(FormConstants.STATE, FormConstants.STATE_FINISH);

        Intent intent = new Intent();
        intent.putExtra(FormConstants.CONTENT_VALUE, _contentValues);

        setResult(RESULT_SUBMIT, intent);
        finish();
    }

    public void onDateViewClicked(View view) {

        DatePickerDialog dialog = new DatePickerDialog(this, new PickDate(), 2014, 1, 26);
        dialog.show();
    }

    public void onTimeViewClicked(View view) {
        TimePickerDialog dialog = new TimePickerDialog(this, new PickTime(), 12, 0, false );
        dialog.show();
    }

    private Bitmap loadBitmap(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }


    private void generateThumbnail(UUID uuid, int index) {
        File srcImage = FileSystemHelper.getEventPicture(this, uuid, index);
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(srcImage.getAbsolutePath()),
                256, 256);

        try {
            OutputStream outputStream = new FileOutputStream(FileSystemHelper.getEventThumbnail(this, uuid, index));
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_RESULT && resultCode == RESULT_OK) {

            UUID uuid = UUID.fromString(_contentValues.getAsString(FormConstants.UUID));
            //String photoPath = FileSystemHelper.getEventPicture(this, uuid, _currentImageIndex ).getAbsolutePath();

            // Save Bitmap into file system.
            //String filePath = mCurrentPhotoPath;//(String)data.getExtras().get("data");  //savePicture(photo);
            //Toast.makeText(this, "FilePath=" + photoPath, Toast.LENGTH_LONG).show();
            generateThumbnail(uuid, _currentImageIndex);
            pushPictureAndThumbnail(_currentImageIndex);
            _makeFormFragment.reloadEventThumbnail(uuid, _currentImageIndex);

        } else if (requestCode == GALLERY_RESULT && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            //cursor.close();

            // Copy external picture into application directory.
            UUID uuid = UUID.fromString(_contentValues.getAsString(FormConstants.UUID));
            String dstPath = FileSystemHelper.getEventPicture(this, uuid, _currentImageIndex).getAbsolutePath();     //genPictureFilePath(_currentImageIndex);

            try {
                copy(new File(picturePath), new File(dstPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            _makeFormFragment.reloadEventThumbnail(uuid, _currentImageIndex);
        }
    }

    private void pushPictureAndThumbnail(int index) {

        String[] pictureKeys = new String[] {
            FormConstants.PIC_URI_0,
            FormConstants.PIC_URI_1,
            FormConstants.PIC_URI_2
        };

        String[] thumbnailKeys = new String[] {
                FormConstants.THUMBNAIL_URI_0,
                FormConstants.THUMBNAIL_URI_1,
                FormConstants.THUMBNAIL_URI_2
        };


        UUID uuid = UUID.fromString(_contentValues.getAsString(FormConstants.UUID));
        File file = FileSystemHelper.getEventPicture(this, uuid, index);
        if (file.exists()) {
            _contentValues.put(pictureKeys[index], file.getAbsolutePath());
        } else {
            _contentValues.put(pictureKeys[index], "");
        }

        file = FileSystemHelper.getEventThumbnail(this, uuid, index);
        if (file.exists()) {
            _contentValues.put(thumbnailKeys[index], file.getAbsolutePath());
        } else {
            _contentValues.put(thumbnailKeys[index], "");
        }
    }

    private class PickDate implements DatePickerDialog.OnDateSetListener {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            _makeFormFragment.setDate(new GregorianCalendar(year, monthOfYear, dayOfMonth));
        }

    }

    private class PickTime implements TimePickerDialog.OnTimeSetListener {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar calendar = new GregorianCalendar();
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            _makeFormFragment.setTime(calendar);

        }
    }
}



