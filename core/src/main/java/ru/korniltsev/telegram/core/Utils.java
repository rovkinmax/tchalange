package ru.korniltsev.telegram.core;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by korniltsev on 23/04/15.
 */
public class Utils {
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
            os = new FileOutputStream(dest);
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
        String name;
        StringBuilder sb = new StringBuilder();
        if (user.firstName.length() != 0) {
            sb.append(user.firstName);
        }
        if (user.lastName.length() != 0){
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(user.lastName);
        }
        name = sb.toString();
        return name;
    }
}
