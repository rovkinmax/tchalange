package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.rx.RxPicasso;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

public class DocumentView extends LinearLayout{

    private ImageView btnPlay;
    private ImageView documentThumb;
    private TextView documentName;
    private View clicker;
    private Subscription subscription = Subscriptions.empty();
    @Inject RxDownloadManager downloader;
    @Inject RxPicasso picasso;
    private TdApi.Document document;

    public DocumentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        documentName = ((TextView) findViewById(R.id.document_name));
        documentThumb = (ImageView) findViewById(R.id.image_document_thumb);
        btnPlay = ((ImageView) findViewById(R.id.btn_play));
        clicker = findViewById(R.id.thumb_and_btn_root);
        clicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloader.isDownloaded(document.document)) {
                    open();
                } else {
                    downloader.download((TdApi.FileEmpty) document.document);
                    set(document);//update ui
                }
            }
        });
    }

    private void open() {
        Assert.fail();
    }

    public void set(TdApi.Document d) {
        this.document = d;
        boolean image = document.mimeType.startsWith("image");
        if (image) {
            documentThumb.setVisibility(View.VISIBLE);
            picasso.loadPhoto(document.thumb.photo)
                    .into(documentThumb);
        } else {
            documentThumb.setVisibility(View.GONE);
        }
        documentName.setText(document.fileName);

        subscription.unsubscribe();

        if (downloader.isDownloaded(document.document)) {
            //show play
            btnPlay.setImageResource(R.drawable.ic_play);
            clicker.setEnabled(true);
        } else if (downloader.isDownloading((TdApi.FileEmpty) document.document)) {
            //show pause
            btnPlay.setImageResource(R.drawable.ic_pause);
            //that does nothing
            clicker.setEnabled(false);
            //subscribe for update
            subscribeForFileDownload((TdApi.FileEmpty) document.document);
        } else {
            //show download Button
            btnPlay.setImageResource(R.drawable.ic_download);
            clicker.setEnabled(true);
        }
    }

    private void subscribeForFileDownload(TdApi.FileEmpty d) {
        subscription = downloader.observableFor(d)
                .subscribe(new Action1<TdApi.UpdateFile>() {
            @Override
            public void call(TdApi.UpdateFile updateFile) {
                document.document = new TdApi.FileLocal(updateFile.fileId, updateFile.size, updateFile.path);
                set(document);
            }
        });
    }
}
