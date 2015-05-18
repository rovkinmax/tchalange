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
import ru.korniltsev.telegram.core.views.DownloadView;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class AudioMessageView extends LinearLayout {

    public static final Subscription EMPTY_SUBSCRIPTION = Subscriptions.empty();
//    private ImageView btnPlay;
    private TextView duration;
    private View progress;

    @Inject AudioPlayer player;
    @Inject RXClient client;

    private TdApi.Audio audio;
    private DownloadView download_view;

    public AudioMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        download_view = ((DownloadView) findViewById(R.id.download_view));
        progress = findViewById(R.id.progress);
        duration = ((TextView) findViewById(R.id.duration));

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setAudio(TdApi.Audio a) {
        this.audio = a;
        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, true, true, 38);
        download_view.bind(a.audio, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {

            }

            @Override
            public void onFinished(TdApi.FileLocal e) {

            }

            @Override
            public void play(TdApi.FileLocal e) {
                player.play(e);
            }
        }, download_view);
    }



}
