package com.liaobeiah.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.vpon.ads.VponBanner;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.UUID;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link com.liaobeiah.app.MakeFormActivity}
 * in two-pane mode (on tablets) or a {@link MakeFormActivity}
 * on handsets.
 */
public class MakeFormFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    //public static final String ARG_ITEM_ID = "item_id";

    private static final String TAG = "MakeFormFragment";



    private ImageView[] _imageViews;
    private EditText _dateView;
    private EditText _timeView;
    private EditText _licenseView;
    private EditText _locationView;
    private String[] _pictureFilePaths;
    private EditText _eventTypeEdit;
    private Spinner _evnetTypeSpinner;

    private EditText _commentEdit;

    private EditText _receiverEdit;
    private Spinner _receiverSpinner;

    private VponBanner vponBanner = null;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MakeFormFragment() {

    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_make_form, container, false);

        _pictureFilePaths = new String[3];
        int i = 0;
        for (i = 0;i<3;i++) {
            _pictureFilePaths[i] = "";
        }

        _imageViews = new ImageView[3];
        _imageViews[0] = (ImageView)rootView.findViewById(R.id.imageView1);
        _imageViews[1] = (ImageView)rootView.findViewById(R.id.imageView2);
        _imageViews[2] = (ImageView)rootView.findViewById(R.id.imageView3);


        _dateView = (EditText)rootView.findViewById(R.id.editTextDate);
        _timeView = (EditText)rootView.findViewById(R.id.editTextTime);

        _commentEdit = (EditText)rootView.findViewById(R.id.editTextComment);

        _licenseView = (EditText)rootView.findViewById(R.id.editTextVehicleLicense);
        _locationView = (EditText)rootView.findViewById(R.id.editTextLocation);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.reasons, android.R.layout.simple_list_item_1);


        // Get Event Type
        _eventTypeEdit = (EditText)rootView.findViewById(R.id.editTextEventType);

        _evnetTypeSpinner = (Spinner)rootView.findViewById(R.id.spinner_reason);
        _evnetTypeSpinner.setAdapter(adapter);





        // Setup police mail spinner
        Map<String, String> map = ResourceUtils.getHashMapResource(getActivity(), R.xml.police_email);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(
                getActivity() ,android.R.layout.simple_list_item_1);


        for (String key : map.keySet()) {
            adapter2.add(key);
        }

        _receiverSpinner = (Spinner)rootView.findViewById(R.id.spinner_receiver);
        _receiverSpinner.setAdapter(adapter2);
        _receiverEdit = (EditText)rootView.findViewById(R.id.editTextReceiver);

        /*
        RelativeLayout adBannerLayout = (RelativeLayout) rootView.findViewById(R.id.adLayout);
        VponBanner vponBanner = new VponBanner(getActivity(), "8a80818243dca272014423f2acd83c6d", VponAdSize.SMART_BANNER, VponPlatform.TW);
        VponAdRequest adRequest = new VponAdRequest();

        HashSet<String> testDeviceImeiSet = new HashSet<String>();
        testDeviceImeiSet.add(MakeFormFragment.getImei(getActivity())); //填入你那台手機的imei
        adRequest.setTestDevices(testDeviceImeiSet);
        //設定可以auto refresh去要banner
        adRequest.setEnableAutoRefresh(true);
        //開始取得banner
        vponBanner.loadAd(adRequest);
        adBannerLayout.addView(vponBanner);
*/
        return rootView;
    }

    public static String getImei(Context context) {

        try
        {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = telephonyManager.getDeviceId();
            return imei;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        Log.i(TAG, "onActivityCreated");

        if (savedInstance != null ) {

            //setPictureFilePath(0, savedInstance.getString(FormConstants.PIC_URI_1));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    public void reloadEventThumbnail(UUID uuid, int index) {
        File file = FileSystemHelper.getEventThumbnail(getActivity(), uuid, index);
        if (file.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPurgeable = true;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            _imageViews[index].setImageBitmap(bitmap);
        }
    }

    private String getDateAsString(Calendar calendar) {
        return "" + calendar.get(Calendar.YEAR) + "年"
                + (calendar.get(Calendar.MONTH) + 1) + "月"
                + calendar.get(Calendar.DAY_OF_MONTH) + "日";
    }

    private String getTimeAsStirng(Calendar calendar) {
        return ""
                + calendar.get(Calendar.HOUR_OF_DAY) + "時"
                + calendar.get(Calendar.MINUTE) + "分";
    }

    public void restoreUIState(ContentValues contentValues) {

        Log.i(TAG, "restoreUIState");
        Integer state = contentValues.getAsInteger(FormConstants.STATE);
        String date = contentValues.getAsString(FormConstants.DATE);
        if (date == null) {
            date = getDateAsString(new GregorianCalendar());
        }


        String time = contentValues.getAsString(FormConstants.TIME);
        if (time == null) {
            time = getTimeAsStirng(new GregorianCalendar());
        }

        String license = contentValues.getAsString(FormConstants.VEHICLE_LICENSE);
        String location = contentValues.getAsString(FormConstants.LOCATION);
        String eventType = contentValues.getAsString(FormConstants.REASON);
        String comment = contentValues.getAsString(FormConstants.COMMENT);
        String receiver = contentValues.getAsString(FormConstants.RECEIVER);


        UUID uuid = UUID.fromString(contentValues.getAsString(FormConstants.UUID));
        for (int i=0;i<3;i++ ) {
            reloadEventThumbnail(uuid, i);
        }

        if (state == FormConstants.STATE_DRAFT) {
            for (int i=0;i<3;i++) {
                _imageViews[i].setClickable(true);
            }

            _dateView.setClickable(true);
            _dateView.setFocusable(false);
            _dateView.setFocusableInTouchMode(false);

            _timeView.setClickable(true);
            _timeView.setFocusable(false);
            _timeView.setFocusableInTouchMode(false);

            _commentEdit.setClickable(true);
            _commentEdit.setFocusable(true);
            _commentEdit.setFocusableInTouchMode(true);

            _licenseView.setClickable(true);
            _licenseView.setFocusable(true);
            _licenseView.setFocusableInTouchMode(true);

            _locationView.setClickable(true);
            _locationView.setFocusable(true);
            _locationView.setFocusableInTouchMode(true);

            _eventTypeEdit.setVisibility(View.GONE);
            _receiverEdit.setVisibility(View.GONE);



        } else {

            for (int i=0;i<3;i++) {
                _imageViews[i].setClickable(false);
            }

            _dateView.setClickable(false);
            _dateView.setOnClickListener(null);
            _dateView.setFocusable(false);
            _dateView.setFocusableInTouchMode(false);


            _timeView.setClickable(false);
            _timeView.setFocusable(false);
            _timeView.setOnClickListener(null);
            _timeView.setFocusableInTouchMode(false);

            _commentEdit.setClickable(false);
            _commentEdit.setFocusable(false);
            _commentEdit.setFocusableInTouchMode(false);

            _licenseView.setClickable(false);
            _licenseView.setFocusable(false);
            _licenseView.setFocusableInTouchMode(false);

            _locationView.setClickable(false);
            _locationView.setFocusable(false);
            _locationView.setFocusableInTouchMode(false);

            _evnetTypeSpinner.setVisibility(View.GONE);
            _receiverSpinner.setVisibility(View.GONE);

        }

        _dateView.setText(date);
        _timeView.setText(time);
        _licenseView.setText(license);
        _locationView.setText(location);
        _eventTypeEdit.setText(eventType);
        _commentEdit.setText(comment);
        _receiverEdit.setText(receiver);
    }

    public void setDate(Calendar calendar) {
        _dateView.setText(getDateAsString(calendar));
    }

    public void setTime(Calendar calendar) {
        _timeView.setText(getTimeAsStirng(calendar));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vponBanner != null) {
            //離開時 "千萬"記得要呼叫vponBanner的 destroy
            vponBanner.destroy();
            vponBanner = null;
        }
    }

}



















