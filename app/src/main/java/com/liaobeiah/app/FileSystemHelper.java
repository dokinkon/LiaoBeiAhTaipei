package com.liaobeiah.app;

import android.content.Context;

import java.io.File;
import java.util.UUID;

/**
 * Created by dokinkon on 2/11/14.
 */
public class FileSystemHelper {

    public static File getEventFolder(Context context, UUID uuid) {
        File file = context.getExternalFilesDir(uuid.toString());
        file.mkdir();
        return file;
    }

    public static File getEventPicture(Context context, UUID uuid, int index) {

        String fileName = "PIC-" + index + ".jpg";
        return new File(getEventFolder(context, uuid), fileName);
    }

    public static File getEventThumbnail(Context context, UUID uuid, int index) {

        String fileName = "THUMBNAIL-" + index + ".jpg";
        return new File(getEventFolder(context, uuid), fileName);

    }

    public static File getEventForm(Context context, UUID uuid) {
        String fileName = "form.docx";
        return new File(getEventFolder(context, uuid), fileName);
    }
}
