package com.liaobeiah.app;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;

/**
 * Created by dokinkon on 1/29/14.
 */
public class MainFragment extends Fragment {
    public MainFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        ListView listView = (ListView)rootView.findViewById(R.id.main_list_view);


        DatabaseHelper helper = new DatabaseHelper(getActivity());
        SQLiteDatabase database = helper.getReadableDatabase();
        if (database.isOpen()) {
            Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
            if (cursor.getCount() > 0) {
                String[] from = new String[]{ FormConstants.THUMBNAIL_URI_0, FormConstants.VEHICLE_LICENSE,
                        FormConstants.REASON, FormConstants.DATE };

                int[] to = new int[]{ R.id.imageViewPic, R.id.textViewLicense, R.id.textViewReason,
                        R.id.textViewDate};

                // FIXME
                // SimpleCursorAdapter require API11, but current min API is 10.
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.form_item,
                        cursor, from, to);

                listView.setAdapter(adapter);
                RelativeLayout layout = (RelativeLayout)rootView.findViewById(R.id.main_relative_layout);
                layout.setVisibility(View.INVISIBLE);
            } else {
                listView.setVisibility(View.INVISIBLE);

            }
        }



        return rootView;
    }

}
