package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.Nullable;
import android.util.Log;
import junit.framework.Assert;
import opus.OpusSupport;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
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
    private int playerBufferSize;
    private RxDownloadManager downloader;
    //guarde by ui thread
    @Nullable private Track currentTrack;

    @Inject
    public AudioPlayer(Context ctx, RxDownloadManager downloader) {
        this.downloader = downloader;
        this.ctx = ctx;




        playerBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }
    }
    public Observable<TrackState> play(TdApi.FileLocal file){

        if (OpusSupport.nativeIsOpusFile(file.path)){
            return playOpus(file.path);
        } else {
            //todo :O
            throw new IllegalStateException("unsupported");
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
//        final OpusReader reader;
//        final OpusPlayer player;
        final String filePath;
        private final int frameCount;
        //        private final int totalDuration;
//        private final Future<Boolean> submit;
//        private final Future<Boolean> submit1;


        public BehaviorSubject<TrackState> state;

        public Track(  String filePath) {
            checkMainThread();
            boolean opened = OpusSupport.nativeOpenOpusFile(filePath);
//            totalDuration = (int) OpusSupport.nativeGetTotalPcmDuration();
            Assert.assertTrue(opened);
            this.filePath = filePath;




            ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
//            OpusSupport.nativeReadOpusFile(buffer, );



            int offst = 0;
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            while (true){
                OpusSupport.nativeReadOpusFile(buffer, buffer.capacity(), mOutArgs);
                int size = mOutArgs[0];
                int pcmOffset = mOutArgs[1];
                int finished = mOutArgs[2];
                arr.write(buffer.array(), 0, size);

                if (finished == 1){
                    break;
                }
            }
            final byte[] bytes = arr.toByteArray();
            this.track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bytes.length,
                    AudioTrack.MODE_STATIC);
            //todo do not read full file nemory or decode as a part of download process
            track.write(bytes, 0, bytes.length);


//            List<AudioBuffer> initialBuffer = Collections.singletonList(new AudioBuffer(playerBufferSize));
//            BlockingQueue<AudioBuffer> q1 = new ArrayBlockingQueue<>(1, false, initialBuffer);
//            BlockingQueue<AudioBuffer> q2 = new ArrayBlockingQueue<>(1);

//            this.reader = new OpusReader(q1, q2);
//            this.player = new OpusPlayer(track, q2, q1);

            track.setPositionNotificationPeriod(SAMPLE_RATE_IN_HZ / 2);
            frameCount = bytes.length / 2;
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
//            submit = readThread.submit(reader);
//            submit1 = playThread.submit(player);
            track.play();
            state = BehaviorSubject.create(new TrackState(true, 0, frameCount));
        }

        public void stop() {

            state.onNext(new TrackState(false, 0, frameCount));

//            try {
//                reader.stopped = true;
//                player.stopped = true;
//                submit.cancel(true);
//                submit1.cancel(true);
//                reader.await.await();
//                player.await.await();
//            } catch (InterruptedException ignore) {
//                Thread.currentThread().interrupt();
//            }
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

    private final ExecutorService readThread = Executors.newSingleThreadExecutor();
    private final ExecutorService playThread = Executors.newSingleThreadExecutor();

//    private final BlockingQueue<AudioBuffer> buffer = new ArrayBlockingQueue<>(1);

    class OpusReader  implements Callable<Boolean>{
//        private final BlockingQueue<AudioBuffer> queue;
        final BlockingQueue<AudioBuffer> readQueue;
        final BlockingQueue<AudioBuffer> writeQueue;
        private volatile boolean stopped = false;
        final CountDownLatch await = new CountDownLatch(1);
        OpusReader(BlockingQueue<AudioBuffer> readQueue, BlockingQueue<AudioBuffer> writeQueue) {
            this.readQueue = readQueue;
            this.writeQueue = writeQueue;

        }

//        @Override
//        public void run() {
//
//        }

        private void readImpl() throws InterruptedException {
//            boolean isFirstBuffer = true;
            AudioBuffer buffer ;
            do {
                if (stopped){
                    return;
                }
//                if (isFirstBuffer ) {
//                    buffer = firstBuffer;
//                    isFirstBuffer = false;
//                } else {
//                    we wait until other thread has stopped
                    buffer = readQueue.take();
//                }
                System.out.println("read");

                OpusSupport.nativeReadOpusFile(buffer.buffer, playerBufferSize, mOutArgs);

                buffer.size = mOutArgs[0];
                buffer.pcmOffset = mOutArgs[1];
                buffer.finished = mOutArgs[2];

                writeQueue.put(buffer);
            } while (buffer.finished != 1);
        }

        @Override
        public Boolean call() throws Exception {
            try {
                readImpl();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            await.countDown();
            System.out.println("reader exit");
            return true;
        }
    }

    class OpusPlayer implements Callable<Boolean> {
        final AudioTrack track;
        final BlockingQueue<AudioBuffer> readQueue;
        final BlockingQueue<AudioBuffer> writeQueue;
        private final CountDownLatch await = new CountDownLatch(1);
        private volatile boolean stopped = false;

        public OpusPlayer(AudioTrack track, BlockingQueue<AudioBuffer> readQueue, BlockingQueue<AudioBuffer> writeQueue) {
            this.track = track;
            this.readQueue = readQueue;
            this.writeQueue = writeQueue;
        }



        private void writeImpl() throws InterruptedException {
            AudioBuffer buffer;
            do {
                if (stopped){
                    return;
                }
                buffer = readQueue.take();
                if (buffer.size > 0) {
                    buffer.buffer.rewind();
                    buffer.buffer.get(buffer.bufferBytes);
                    System.out.println("write");
                    track.write(buffer.bufferBytes, 0, buffer.size);
//                    track.write(buffer, buffer.size, BbufferBytes, 0, buffer.size);
                }
                writeQueue.put(buffer);
            } while (buffer.finished != 1);
        }

        @Override
        public Boolean call() throws Exception {
            try {
                writeImpl();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            await.countDown();
            System.out.println("player exit");
            return true;
        }
    }


    public void pause(String path){
        assertNotNull(currentTrack);
        assertTrue(path.equals(currentTrack.filePath));
        assertTrue(currentTrack.track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
        currentTrack.track.pause();
    }

    private class AudioBuffer {
        public AudioBuffer(int capacity) {
            buffer = ByteBuffer.allocateDirect(capacity);
            bufferBytes = new byte[capacity];
        }

        final ByteBuffer buffer;
        final byte[] bufferBytes;
        int size;
        int finished;
        long pcmOffset;
    }

}
