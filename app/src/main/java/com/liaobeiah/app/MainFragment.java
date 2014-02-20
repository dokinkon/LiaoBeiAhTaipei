package com.liaobeiah.app;


import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.UUID;

/**
 * Created by dokinkon on 1/29/14.
 */
public class MainFragment extends Fragment {


    private static final String TAG = "MainFragment";

    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String id);

        public void onItemClicked(AdapterView<?> parent, View view, int position, long id);

        public void onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id) {
        }

        public void onItemClicked(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    private SimpleCursorAdapter _adapter = null;

    public MainFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i(TAG, "onAttach");

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }



        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        requery(rootView);


        ListView listView = (ListView)rootView.findViewById(R.id.main_list_view);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> av, View v,
                                           int position, long id) {


                mCallbacks.onItemLongClick(av, v, position, id);
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                mCallbacks.onItemClicked(parent, view, position, id);
            }
        });

        return rootView;
    }

    public long insertForm(ContentValues contentValues) {
        DatabaseHelper helper = new DatabaseHelper(getActivity());
        SQLiteDatabase database = helper.getWritableDatabase();
        //database.insert(FormConstants.TABLE_NAME, null, contentValues);
        long id = database.replace(FormConstants.TABLE_NAME, null, contentValues);

        if (id==-1) {
            Log.e(TAG, "Failed to replaceForm");
        } else {
            contentValues.put(FormConstants._ID, id);
        }

        requery(getView());
        return id;
    }

    public void deleteForm(long rowId) {
        DatabaseHelper helper = new DatabaseHelper(getActivity());
        SQLiteDatabase database = helper.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME + " where "
                + FormConstants._ID + " = " + rowId, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(FormConstants.UUID);
        UUID uuid = UUID.fromString(cursor.getString(columnIndex));
        FileSystemHelper.deleteEvent(getActivity(), uuid);

        database.delete(FormConstants.TABLE_NAME, FormConstants._ID + " = " + rowId, null);

        requery(getView());
    }

    public void deleteAllEvents() {
        FileSystemHelper.deleteAllEvents(getActivity());
        requery(getView());
    }

    public void requery() {
        requery(getView());
    }

    private void requery(View rootView) {

        ListView listView = (ListView)rootView.findViewById(R.id.main_list_view);
        RelativeLayout layout = (RelativeLayout)rootView.findViewById(R.id.main_relative_layout);

        DatabaseHelper helper = new DatabaseHelper(getActivity());
        SQLiteDatabase database = helper.getReadableDatabase();
        if (database.isOpen()) {
            Cursor cursor = database.rawQuery("select * from " + FormConstants.TABLE_NAME, null);
            if (cursor.getCount() > 0) {

                if (_adapter == null ) {
                    String[] from = new String[]{
                            FormConstants.STATE,
                            FormConstants.THUMBNAIL_URI_0,
                            FormConstants.VEHICLE_LICENSE,
                            FormConstants.REASON,
                            FormConstants.DATE };

                    int[] to = new int[]{
                            R.id.textViewState,
                            R.id.imageViewPic,
                            R.id.textViewLicense,
                            R.id.textViewReason,
                            R.id.textViewDate};

                    // FIXME
                    // SimpleCursorAdapter require API11, but current min API is 10.
                    _adapter = new SimpleCursorAdapter(getActivity(), R.layout.form_item,
                            cursor, from, to);

                    _adapter.setViewBinder(new ViewBinder());
                    listView.setAdapter(_adapter);
                } else {
                    _adapter.changeCursor(cursor);
                }

                listView.setVisibility(View.VISIBLE);
                layout.setVisibility(View.INVISIBLE);

            } else {

                listView.setVisibility(View.INVISIBLE);
                layout.setVisibility(View.VISIBLE);

            }
        }
    }


    private class ViewBinder implements SimpleCursorAdapter.ViewBinder {
        public boolean setViewValue (View view, Cursor cursor, int columnIndex) {

            String columnName = cursor.getColumnName(columnIndex);

            if (columnName.equals(FormConstants.THUMBNAIL_URI_0)) {

                try {
                    ImageView imageView = (ImageView)view;
                    String filePath = cursor.getString(columnIndex);
                    FileInputStream inputStream = new FileInputStream(new File(filePath));
                    imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (columnName.equals(FormConstants.VEHICLE_LICENSE)) {
                TextView textView = (TextView)view;
                textView.setText(cursor.getString(columnIndex));
            } else if (columnName.equals(FormConstants.REASON)) {
                TextView textView = (TextView)view;
                textView.setText(cursor.getString(columnIndex));
            } else if (columnName.equals(FormConstants.DATE)) {
                TextView textView = (TextView)view;
                textView.setText(cursor.getString(columnIndex));
            } else if (columnName.equals(FormConstants.STATE)) {
                TextView textView = (TextView)view;
                int state = cursor.getInt(columnIndex);
                if (state == FormConstants.STATE_DRAFT) {
                    textView.setText("草稿");
                } else if (state == FormConstants.STATE_UNSENT) {
                    textView.setText("未送達");
                } else if (state == FormConstants.STATE_SENDING){
                    textView.setText("傳送中");
                } else {
                    textView.setText("");
                }
            }

            return true;
        }
    }

}
