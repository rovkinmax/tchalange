package ru.korniltsev.telegram.core;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

/**
 * Created by korniltsev on 23/04/15.
 */
public class Utils {
    public static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        // Target ~15% of the available heap.
        int percent15 = 1024 * 1024 * memoryClass / 7;
        return 2 * percent15;
    }
    public static String textFrom(EditText e) {
        return e.getText().toString();
    }
    public static void hideKeyboard(EditText e){
        InputMethodManager imm = (InputMethodManager)e.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(e.getWindowToken(), 0);
    }



    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }

    public static String uiName(TdApi.User user) {//todo
        if (user == null) {
            return "";
        }
        String firstName = user.firstName;
        String lastName = user.lastName;
        String name = uiName(firstName, lastName);
        return name;
    }

    public static String uiName(String firstName, String lastName) {
        String name;
        StringBuilder sb = new StringBuilder();
        if (firstName.length() != 0) {
            sb.append(firstName);
        }
        if (lastName.length() != 0){
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        name = sb.toString();
        return name;
    }

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static long dateToMillis(long date) {
        return date * 1000;
    }

    public static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
    }

    @Nullable
    public static String getGalleryPickedFilePath(Context ctx, Intent data) {
        Uri selectedImage = data.getData();
        // h=1;
        //imgui = selectedImage;
        ContentResolver contentResolver = ctx.getContentResolver();

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor c;
        if(SDK_INT >= 19)
        {
            // Will return "image:x*"
            String wholeID = DocumentsContract.getDocumentId(selectedImage);
            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];
            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";
            c = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, sel, new String[]{id}, null);
        } else {
            c = contentResolver.query(selectedImage, projection, null, null, null);
        }
        String picturePath;
        if (c.moveToNext()) {
            picturePath = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));


        } else {
            picturePath = null;
        }
        c.close();
        return picturePath;
    }

    public static void event(String eventName) {
        CrashlyticsCore.getInstance()
                .log(Log.INFO, "Event", eventName);
    }
}
