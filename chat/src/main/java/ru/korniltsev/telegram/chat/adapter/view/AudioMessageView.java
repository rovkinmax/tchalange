package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.audio.AudioPlayer;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class AudioMessageView extends LinearLayout {

    public static final Subscription EMPTY_SUBSCRIPTION = Subscriptions.empty();
    private ImageView btnPlay;
    private TextView duration;
    private View progress;

    @Inject RxDownloadManager downloader;
    @Inject AudioPlayer player;
    @Inject RXClient client;

    private TdApi.Audio audio;
    private Subscription subscription = EMPTY_SUBSCRIPTION;

    public AudioMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        btnPlay = (ImageView) findViewById(R.id.btn_play);
        progress = findViewById(R.id.progress);
        duration = ((TextView) findViewById(R.id.duration));
        btnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloader.isDownloaded(audio.audio)) {
                    play();
                } else {
                    download();
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }

    public void setAudio(TdApi.Audio a) {
        this.audio = a;
        subscription.unsubscribe();

        if (downloader.isDownloaded(a.audio)) {
            //show play
            btnPlay.setImageResource(R.drawable.ic_play);
            btnPlay.setEnabled(true);
        } else if (downloader.isDownloading((TdApi.FileEmpty) a.audio)) {
            //show pause
            btnPlay.setImageResource(R.drawable.ic_pause);
            //that does nothing
            btnPlay.setEnabled(false);

            //subscribe for update
            subscribeForFileDownload((TdApi.FileEmpty) a.audio);
        } else {
            //show download Button
            btnPlay.setImageResource(R.drawable.ic_download);
            btnPlay.setEnabled(true);
        }
    }

    private void play() {
        player.play((TdApi.FileLocal) audio.audio);
//        setDataSource(((TdApi.FileLocal) audio.audio).path);
//        player.start();
    }

    private void download() {
        downloader.download((TdApi.FileEmpty) audio.audio);
        setAudio(audio);//update ui
    }

    private void subscribeForFileDownload(final TdApi.FileEmpty file) {
        subscription = downloader.nonMainThreadObservableFor(file)
                .observeOn(mainThread())
                .subscribe(new Action1<TdApi.FileLocal>() {
                    @Override
                    public void call(TdApi.FileLocal update) {
                        audio.audio = update;//new TdApi.FileLocal(update.fileId, update.size, update.path);
                        setAudio(audio);
                    }
                });
    }
}
