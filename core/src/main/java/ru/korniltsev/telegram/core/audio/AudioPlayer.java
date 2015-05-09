package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

@Singleton
public class AudioPlayer {
    private final MediaPlayer mPlayer;
    private final Context ctx;
    private RxDownloadManager downloader;

    @Inject
    public AudioPlayer(Context ctx, RxDownloadManager downloader) {
        this.ctx = ctx;
        this.downloader = downloader;
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.start();
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

            }
        });
        mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {

            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
        mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }
    public void play(TdApi.FileLocal file){

        try {

            File src = new File(file.path);

            File exposed = downloader.exposeFile(src, Environment.DIRECTORY_MUSIC);
//            File externalFilesDir = ctx.getExternalFilesDir("telegram audio");
//            externalFilesDir.mkdirs();
//            File dst = new File(externalFilesDir, src.getName());
//            Utils.copyFile(src, dst);

            mPlayer.setDataSource(exposed.getAbsolutePath());

            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Assert.fail();
        } finally {

        }

    }

    public void pause(){
        if (mPlayer.isPlaying()){
            mPlayer.pause();
        }
    }

}
