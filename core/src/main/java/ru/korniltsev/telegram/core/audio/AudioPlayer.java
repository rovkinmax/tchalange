package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Environment;
import junit.framework.Assert;
import opus.OpusSupport;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class AudioPlayer {
    private final int[] mOutArgs = new int[3];
    private final MediaPlayer mPlayer;
    private final Context ctx;
    private int playerBufferSize;
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




        playerBufferSize = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }
    }
    public void play(TdApi.FileLocal file){

        if (OpusSupport.nativeIsOpusFile(file.path)){
            playOpus(file.path);
            return;
        }
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

    private void playOpus(final String path) {


        boolean opened = OpusSupport.nativeOpenOpusFile(path);
        Assert.assertTrue(opened);
        AudioTrack audioTrackPlayer = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                48000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                playerBufferSize,
                AudioTrack.MODE_STREAM);
        List<AudioBuffer> initialBuffer = Collections.singletonList(new AudioBuffer(playerBufferSize));
        BlockingQueue<AudioBuffer> q1 = new ArrayBlockingQueue<>(1, false, initialBuffer);
        BlockingQueue<AudioBuffer> q2 = new ArrayBlockingQueue<>(1);

        readThread.submit(new OpusReader(q1, q2));
        playThread.submit(new OpusPlayer(audioTrackPlayer, q2, q1));
        audioTrackPlayer.play();

    }

    private final ExecutorService readThread = Executors.newSingleThreadExecutor();
    private final ExecutorService playThread = Executors.newSingleThreadExecutor();

//    private final BlockingQueue<AudioBuffer> buffer = new ArrayBlockingQueue<>(1);

    class OpusReader  implements Runnable{
//        private final BlockingQueue<AudioBuffer> queue;
        final BlockingQueue<AudioBuffer> readQueue;
        final BlockingQueue<AudioBuffer> writeQueue;

        OpusReader(BlockingQueue<AudioBuffer> readQueue, BlockingQueue<AudioBuffer> writeQueue) {
            this.readQueue = readQueue;
            this.writeQueue = writeQueue;
        }

        @Override
        public void run() {
            try {
                readImpl();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void readImpl() throws InterruptedException {
//            boolean isFirstBuffer = true;
            AudioBuffer buffer ;
            do {
//                if (isFirstBuffer ) {
//                    buffer = firstBuffer;
//                    isFirstBuffer = false;
//                } else {
//                    we wait until other thread has stopped
                    buffer = readQueue.take();
//                }
                OpusSupport.nativeReadOpusFile(buffer.buffer, playerBufferSize, mOutArgs);

                buffer.size = mOutArgs[0];
                buffer.pcmOffset = mOutArgs[1];
                buffer.finished = mOutArgs[2];

                writeQueue.put(buffer);
            } while (buffer.finished != 1);
        }
    }

    class OpusPlayer implements Runnable {
        final AudioTrack track;
        final BlockingQueue<AudioBuffer> readQueue;
        final BlockingQueue<AudioBuffer> writeQueue;

        public OpusPlayer(AudioTrack track, BlockingQueue<AudioBuffer> readQueue, BlockingQueue<AudioBuffer> writeQueue) {
            this.track = track;
            this.readQueue = readQueue;
            this.writeQueue = writeQueue;
        }

        @Override
        public void run() {
            try {
                writeImpl();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void writeImpl() throws InterruptedException {
            AudioBuffer buffer;
            do {
                buffer = readQueue.take();
                if (buffer.size > 0) {
                    buffer.buffer.rewind();
                    buffer.buffer.get(buffer.bufferBytes);
                    track.write(buffer.bufferBytes, 0, buffer.size);
                }
                writeQueue.put(buffer);
            } while (buffer.finished != 1);
        }
    }


    public void pause(){
//        if (mPlayer.isPlaying()){
//            mPlayer.pause();
//        }
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
