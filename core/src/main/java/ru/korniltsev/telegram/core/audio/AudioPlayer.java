package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.core.CrashlyticsCore;
import junit.framework.Assert;
import opus.OpusSupport;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.utils.Preconditions;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;

@Singleton
public class AudioPlayer {
    public static final int SAMPLE_RATE_IN_HZ = 48000;
    private final int[] mOutArgs = new int[3];
//    private final MediaPlayer mPlayer;
    private final Context ctx;
    private final File decodeCacheDir;
    private int playerBufferSize;
    private RxDownloadManager downloader;
    //guarde by ui thread
    @Nullable private Track currentTrack;

    @Inject
    public AudioPlayer(Context ctx, RxDownloadManager downloader) {
        this.downloader = downloader;
        this.ctx = ctx;
        decodeCacheDir = createAudioCacheDir(ctx);

        playerBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }
    }

    private File createAudioCacheDir(Context ctx) {
        File decodeCacheDir = new File(ctx.getFilesDir(), "decodeCacheDir");//todo move tod DownloadManger
        decodeCacheDir.mkdirs();
        File[] files = decodeCacheDir.listFiles();
        if (files != null){
            for (File f : files) {
                f.delete();
            }
        }
        return decodeCacheDir;
    }

    public Observable<TrackState> play(TdApi.FileLocal file){

        if (OpusSupport.nativeIsOpusFile(file.path)){
            return playOpus(file.path);
        } else {
            CrashlyticsCore.getInstance()
                    .logException(new IllegalStateException("unsupported"));
            return Observable.empty();

        }

    }

    public boolean isPaused(String path) {
        return currentTrack != null
                && currentTrack.filePath.equals(path)
                && currentTrack.track.getPlayState() == AudioTrack.PLAYSTATE_PAUSED;
    }

    public void resume(String path) {
        assertNotNull(currentTrack);
        assertTrue(path.equals(currentTrack.filePath));
        currentTrack.track.play();
    }

    class Track {
        final AudioTrack track;
        final String filePath;
        private final int frameCount;


        public BehaviorSubject<TrackState> state;

        public Track(  String filePath) {
            checkMainThread();
            this.filePath = filePath;
            File  arr = DecodeOpusFile(filePath);

            int length = (int) arr.length();
            this.track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    length,
                    AudioTrack.MODE_STATIC);

            write(arr);

            track.setPositionNotificationPeriod(SAMPLE_RATE_IN_HZ / 2);
            frameCount = length / 2;
            track.setNotificationMarkerPosition(frameCount);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    System.out.println("onMarkerReached");

                    state.onNext(new TrackState(false, frameCount, frameCount));
                    trackPlayed();
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {

                    int p = track.getPlaybackHeadPosition();
                    Log.e("AudioPlayer", String.format("%d %d", p, frameCount));
                    state.onNext(new TrackState(true, p, frameCount));
                }
            });
            track.play();
            state = BehaviorSubject.create(new TrackState(true, 0, frameCount));
        }

        private void write(File arr) {
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(arr.getPath());
                mp.prepare();
                mp.start();



//                writeUnsafe(arr);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        private void writeUnsafe(File arr) throws IOException {
            BufferedInputStream source = new BufferedInputStream(new FileInputStream(arr));
            byte[] buffer = new byte[playerBufferSize];
            int read ;
            while (((read = source.read(buffer)) != -1)){
                track.write(buffer, 0, read);
            }
        }

        @NonNull
        private File DecodeOpusFile(String filePath)  {
            try {
                return decodeOpusFileUnsafe(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @NonNull
        private File decodeOpusFileUnsafe(String filePath) throws IOException {
            Preconditions.checkMainThread();
            File src = new File(filePath);
            File dst = new File(decodeCacheDir, src.getName());
//            if (dst.exists()){
//                return dst;
//            }
            boolean opened = OpusSupport.nativeOpenOpusFile(filePath);
            Assert.assertTrue(opened);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);


            FileOutputStream out = new FileOutputStream(dst);
            while (true){
                OpusSupport.nativeReadOpusFile(buffer, buffer.capacity(), mOutArgs);
                int size = mOutArgs[0];
                int pcmOffset = mOutArgs[1];
                int finished = mOutArgs[2];
                out.write(buffer.array(), 0, size);

                if (finished == 1){
                    break;
                }
            }
            out.flush();
            out.close();
            return dst;
        }

        public void stop() {
            state.onNext(new TrackState(false, 0, frameCount));
            track.pause();
            track.stop();
        }
    }

    private void trackPlayed() {
        currentTrack = null;
    }

    public class TrackState{
        //false if finished or stopped by another track
        public final boolean playing;
        public final int head;
        public final int duration;

        public TrackState(boolean playing, int head, int duration) {
            this.playing = playing;
            this.head = head;
            this.duration = duration;


        }
    }



    private Observable<TrackState> playOpus(final String path) {
        if (currentTrack != null){
            currentTrack.stop();
        }
        currentTrack = new Track(path);
        return currentTrack.state;
    }

    public Observable<TrackState> current() {
        assertNotNull(currentTrack);
        return currentTrack.state;
    }

    public boolean isPLaying(String path) {
        return currentTrack != null
                && currentTrack.filePath.equals(path)
                && AudioTrack.PLAYSTATE_PLAYING == currentTrack.track.getPlayState();
    }


    public void pause(String path){
        assertNotNull(currentTrack);
        assertTrue(path.equals(currentTrack.filePath));
        assertTrue(currentTrack.track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
        currentTrack.track.pause();
    }


}
