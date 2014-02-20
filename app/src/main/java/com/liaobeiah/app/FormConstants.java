package com.liaobeiah.app;

import android.provider.BaseColumns;

/**
 * Created by dokinkon on 1/27/14.
 */
public interface FormConstants extends BaseColumns {

    public static final String CONTENT_VALUE = "contentVale";

    public static final String TABLE_NAME = "forms";
    public static final String DATE = "date";
    public static final String TIME = "time";
    public static final String LOCATION = "location";
    public static final String VEHICLE_LICENSE = "vehicle_license";
    public static final String PIC_URI_0 = "pic_uri_0";
    public static final String PIC_URI_1 = "pic_uri_1";
    public static final String PIC_URI_2 = "pic_uri_2";
    public static final String THUMBNAIL_URI_0 = "thumbnail_uri_0";
    public static final String THUMBNAIL_URI_1 = "thumbnail_uri_1";
    public static final String THUMBNAIL_URI_2 = "thumbnail_uri_2";
    public static final String UUID = "uuid";
    public static final String REASON = "reason";
    public static final String COMMENT = "comment";
    public static final String RECEIVER = "receiver";
    public static final String STATE = "state";

    public static final int STATE_DRAFT   = 0;
    public static final int STATE_SENDING = 1;
    public static final int STATE_FINISH  = 2;
    public static final int STATE_UNSENT  = 3;
}
