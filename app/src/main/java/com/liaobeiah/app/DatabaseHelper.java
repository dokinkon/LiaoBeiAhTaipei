package com.liaobeiah.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by dokinkon on 1/27/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "com.liaobeiah.form";
    private static final int DATABASE_VERSION = 1;


    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {


        db.execSQL("CREATE TABLE " + FormConstants.TABLE_NAME
                + "( " + FormConstants._ID + " integer primary key autoincrement"
                + ", " + FormConstants.STATE + " integer"
                + ", " + FormConstants.UUID + " text"
                + ", " + FormConstants.PIC_URI_1 + " text"
                + ", " + FormConstants.PIC_URI_2 + " text"
                + ", " + FormConstants.PIC_URI_3 + " text"
                + ", " + FormConstants.THUMBNAIL_URI_0 + " text"
                + ", " + FormConstants.THUMBNAIL_URI_2 + " text"
                + ", " + FormConstants.THUMBNAIL_URI_3 + " text"
                + ", " + FormConstants.DATE + " text"
                + ", " + FormConstants.TIME + " text"
                + ", " + FormConstants.LOCATION + " text"
                + ", " + FormConstants.VEHICLE_LICENSE + " text"
                + ", " + FormConstants.REASON + " text"
                + ", " + FormConstants.COMMENT + " text"
                + ", " + FormConstants.RECEIVER + " text);");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FormConstants.TABLE_NAME);
        onCreate(db);
    }

}
