package ru.korniltsev.telegram.attach_panel;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import rx.Observable;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GalleryService {
    private static List<String> getRecentImages(Context ctx) {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns.DATA,
        };
        final Cursor cursor = ctx.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if (cursor == null) {
            return Collections.emptyList();
        }
        ArrayList<String> images = new ArrayList<>();
        while (cursor.moveToNext()) {
            images.add(cursor.getString(0));
        }
        cursor.close();
        return images;
    }

    public static Observable<List<String>> recentImages(final Context ctx) {
        return Observable.defer(new Func0<Observable<List<String>>>() {
            @Override
            public Observable<List<String>> call() {
                return Observable.just(getRecentImages(ctx));
            }
        }).subscribeOn(Schedulers.io());
    }

}
