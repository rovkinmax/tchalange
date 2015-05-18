package ru.korniltsev.telegram.chat.adapter.view;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.views.DownloadView;

import javax.inject.Inject;
import java.io.File;

public class DocumentView extends LinearLayout{

//    private ImageView btnPlay;
    private ImageView documentThumb;
    private TextView documentName;
    private TextView documentProgress;
    private View clicker;
//    private Subscription subscription = Subscriptions.empty();
    @Inject RxDownloadManager downloader;
    @Inject RxGlide picasso;
    private TdApi.Document document;
    private DownloadView downloadView;

    public DocumentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        documentName = ((TextView) findViewById(R.id.document_name));
        documentProgress = ((TextView) findViewById(R.id.document_progress));
        documentThumb = (ImageView) findViewById(R.id.image_document_thumb);
        downloadView = (DownloadView) findViewById(R.id.download_view);
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
            picasso.loadPhoto(document.thumb.photo, false)
                    .into(documentThumb);
        } else {
            documentThumb.setVisibility(View.GONE);
        }
        documentName.setText(document.fileName);
        documentProgress.setText("");
        DownloadView.Config cfg;
        if (image){
            cfg = new DownloadView.Config(DownloadView.Config.FINAL_ICON_EMPTY, false, false, 48);
        } else {
            cfg = new DownloadView.Config(R.drawable.ic_file, true, false, 38);
        }
        downloadView.bind(d.document, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {
                documentProgress.setText(getResources().getString(R.string.downloading_kb,  kb(p.size),kb(p.ready)));
            }

            @Override
            public void onFinished(TdApi.FileLocal e) {
                documentProgress.setText(getResources().getString(R.string.downloaded_kb, kb(e.size)));
            }

            @Override
            public void play(TdApi.FileLocal e) {
                openDocument(e);
            }
        });

    }

    private void openDocument(TdApi.FileLocal e) {
        File f = new File(e.path);
        String name = document.fileName;
        if (name != null && name.equals("")) {
            name = null;
        }
        File target = downloader.exposeFile(f, Environment.DIRECTORY_DOWNLOADS, name);

        String type = document.mimeType;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.fromFile(target);

        intent.setDataAndType(data, type);

        try {
            getContext()
                    .startActivity(intent);
        } catch (ActivityNotFoundException e1) {
            //todo err
        }
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " b";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ( "KMGTPE").charAt(exp-1) + "";
        return String.format("%.1f %sb", bytes / Math.pow(unit, exp), pre);
    }

    private String kb(int size) {
        return humanReadableByteCount(size);
    }


}
