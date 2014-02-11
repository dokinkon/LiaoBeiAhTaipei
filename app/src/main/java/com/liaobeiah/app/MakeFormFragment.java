package com.liaobeiah.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.Calendar;
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
    public static final String ARG_ITEM_ID = "item_id";

    private static final String TAG = "MakeFormFragment";

    /**
     * The dummy content this fragment is presenting.
     */
    // TODO remove this field.
    //private DummyContent.DummyItem mItem;


    private ImageView[] _imageViews;
    private String[] _pictureFilePaths;
    //private UUID _formUuid;


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

    //public void setUUID(UUID uuid) {
        //_formUuid = uuid;

    //}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
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

        Calendar calendar = Calendar.getInstance();
        TextView dateView = (TextView)rootView.findViewById(R.id.textViewDate);
        dateView.setText(""
            + calendar.get(Calendar.YEAR) + "年"
            + (calendar.get(Calendar.MONTH) + 1) + "月"
            + calendar.get(Calendar.DAY_OF_MONTH) + "日");

        TextView timeView = (TextView)rootView.findViewById(R.id.textViewTime);
        timeView.setText(""
            + calendar.get(Calendar.HOUR_OF_DAY) + "時"
            + calendar.get(Calendar.MINUTE) + "分");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(rootView.getContext(),
            R.array.reasons, android.R.layout.simple_list_item_1);

        Spinner spinner = (Spinner)rootView.findViewById(R.id.spinner_reason);
        spinner.setAdapter(adapter);

        // Setup police mail spinner
        Map<String, String> map = ResourceUtils.getHashMapResource(getActivity(), R.xml.police_email);

        //String[] keys = new String[10];
        //keys = map.keySet().toArray(keys);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(
                getActivity() ,android.R.layout.simple_list_item_1);


        for (String key : map.keySet()) {
            adapter2.add(key);
        }

        //adapter = ArrayAdapter.createFromResource(rootView.getContext(),
           // R.array.receiver, android.R.layout.simple_list_item_1);

        spinner = (Spinner)rootView.findViewById(R.id.spinner_receiver);
        spinner.setAdapter(adapter2);

        return rootView;
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

        outState.putString(FormConstants.PIC_URI_1, _pictureFilePaths[0]);
        //outState.putString(FormConstants.PIC_URI_1, _pictureFilePaths[2]);
        //outState.putString(FormConstants.PIC_URI_1, _pictureFilePaths[0]);

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

    /*
    public void setPictureFilePath(int index, String filePath) {
        _pictureFilePaths[index] = filePath;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

        _imageViews[index].setImageBitmap(bitmap);

    }*/

    /*
    public String getPictureFilePath(int index) {
        return _pictureFilePaths[index];
    }
    */
}
