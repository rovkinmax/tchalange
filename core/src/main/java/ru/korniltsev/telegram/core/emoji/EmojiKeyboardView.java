package ru.korniltsev.telegram.core.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.astuetz.PagerSlidingTabStrip;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.utils.R;
import rx.functions.Action1;

import javax.inject.Inject;
import java.util.List;

public class EmojiKeyboardView extends LinearLayout {

    private final RecentSmiles recent;
    private ViewPager pager;
    @Inject Emoji emoji;
    @Inject Stickers stickers;
    @Inject RxDownloadManager downloader;
    @Inject DpCalculator calc;
    @Inject RxGlide picasso;

    private PagerSlidingTabStrip strip;
    private View backspace;
    private final LayoutInflater viewFactory;

    public EmojiKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        recent =new RecentSmiles(context.getSharedPreferences("RecentEmoji", Context.MODE_PRIVATE));
        ObjectGraphService.inject(context, this);
        viewFactory = LayoutInflater.from(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        pager = ((ViewPager) findViewById(R.id.pager));
        strip = (PagerSlidingTabStrip) findViewById(R.id.tabs_strip);
        strip.setShouldExpand(true);
        Adapter adapter = new Adapter(getContext());
        pager.setAdapter(adapter);
        strip.setViewPager(pager);
        backspace = findViewById(R.id.backspace);
        backspace.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.backspaceClicked();
            }
        });
        if (adapter.recentIds.length==0) {
            pager.setCurrentItem(1);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private CallBack callback;

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    public interface CallBack {
        void backspaceClicked();
        void emojiClicked(long code);
        void stickerCLicked(String stickerFilePath);
    }




    class Adapter extends PagerAdapter implements PagerSlidingTabStrip.IconTabProvider{
        final int[] icons = new int[]{
                R.drawable.ic_smiles_recent_selector,
                R.drawable.ic_smiles_smiles_selector,
                R.drawable.ic_smiles_flower_selector,
                R.drawable.ic_smiles_bell_selector,
                R.drawable.ic_smiles_car_selector,
                R.drawable.ic_smiles_grid_selector,
                R.drawable.ic_smiles_stickers_selector,

        };
        private final LayoutInflater viewFactory;
        private Context context;
        private long[] recentIds;

        public Adapter(Context context) {
            this.context = context;
            recentIds = EmojiKeyboardView.this.recent.get();
            viewFactory = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return /*recent*/ 1 +
                    /*emoji*/ 5 +
                    /* stickers*/ 1;
        }

        @Override
        public GridView instantiateItem(ViewGroup container, int position) {
            if (position == 0){
                //todo cleanup

                final long[] longs = recentIds;
                GridView gridPage = createGridPage(container, position, new EmojiPageAdapter(longs), R.dimen.emoji_size);
                gridPage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        long emojiCode = longs[position];
                        callback.emojiClicked(emojiCode);
                    }
                });
                return gridPage;
            } else if (position == getCount() -1){
                final List<TdApi.Sticker> ss = EmojiKeyboardView.this.stickers.getStickers();
                GridView res = createGridPage(container, position, new StickerAdapter(ss), R.dimen.sticker_size);
                res.setVerticalSpacing(calc.dp(16));
                res.setClipToPadding(false);
                int dip8 = calc.dp(8);
                res.setPadding(dip8/2, dip8, dip8/2, dip8);
                res.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        stickerClicked(ss.get(position));
                    }
                });
                return res;
            } else {
                final long[] data = Emoji.data[position];
                GridView gridPage = createGridPage(container, position, new EmojiPageAdapter(data), R.dimen.emoji_size);
                gridPage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        long emojiCode = data[position];
                        callback.emojiClicked(emojiCode);
                        recent.emojiClicked(emojiCode);
                    }
                });
                return gridPage;
            }

        }

        private void stickerClicked(TdApi.Sticker sticker) {
            downloader.downloadWithoutProgress(sticker.sticker)
                    .subscribe(new ObserverAdapter<TdApi.FileLocal>() {
                        @Override
                        public void onNext(TdApi.FileLocal fileLocal) {
                            callback.stickerCLicked(fileLocal.path);
                        }
                    });
//            if (downloader.isDownloaded(sticker.thumb.photo)) {
//                downloader.downloadWithoutProgress(sticker.thumb.photo)
//                        .subscribe(new Action1<TdApi.FileLocal>() {
//                            @Override
//                            public void call(TdApi.FileLocal fileLocal) {
//
//                            }
//                        });
//            }
        }

        private GridView createGridPage(ViewGroup container, int position1, BaseAdapter adapter, int columnSizeResId) {
            int columnWidth = getContext().getResources().getDimensionPixelSize(columnSizeResId);
            GridView view = (GridView) viewFactory.inflate(R.layout.keyboard_page_emoji, container, false);
            view.setColumnWidth(columnWidth);
            view.setAdapter(adapter);
            container.addView(view);
            view.setTag(position1);
            return view;
        }



        private GridView createRecent(ViewGroup container, int position) {
            View view = viewFactory.inflate(R.layout.keyboard_page_recent, container, false);

            container.addView(view);
            view.setTag( position);
            return (GridView) view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getPageIconResId(int i) {
            return icons[i];
        }
    }

    class EmojiPageAdapter extends BaseAdapter {

        private long[] longs;
        ;

        public EmojiPageAdapter(long[] longs) {

            this.longs = longs;
        }

        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = viewFactory.inflate(R.layout.grid_item_emoji, parent, false);
            return new VH(v);
        }

        public void onBindViewHolder(VH holder, int position) {
            Drawable d = emoji.getEmojiBigDrawable(longs[position]);
            holder.img.setImageDrawable(d);
        }


        @Override
        public int getCount() {
            return longs.length;
        }

        @Override
        public Object getItem(int position) {
            return longs[position];
        }

        @Override
        public long getItemId(int position) {
            return longs[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VH vh;
            if (convertView == null) {
                vh = onCreateViewHolder(parent, 0);
                vh.img.setTag(vh);
            } else {
                vh = (VH) convertView.getTag();
            }
            onBindViewHolder(vh, position);
            return vh.img;
        }


    }
    class VH {
        final ImageView img;
        public VH(View itemView) {
            img = (ImageView)itemView.findViewById(R.id.img);
        }
    }

    class StickerAdapter extends BaseAdapter{
        final List<TdApi.Sticker> data ;

        StickerAdapter(List<TdApi.Sticker> data) {
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public TdApi.Sticker getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VH vh;
            if (convertView == null) {
                View v = viewFactory.inflate(R.layout.grid_item_sticker, parent, false);
                vh = new VH(v);
                v.setTag(vh);
            } else {
                vh = (VH) convertView.getTag();
            }

            onBindVH(vh, position);
            return vh.img;
        }

        private void onBindVH(final VH vh, int position) {
            final TdApi.Sticker s = getItem(position);
            picasso.loadPhoto(s.thumb.photo, true)
                    .priority(Picasso.Priority.HIGH)
                    .into(vh.img, new Callback() {
                        @Override
                        public void onSuccess() {
                            picasso.loadPhoto(s.sticker, true)
                                    .placeholder(vh.img.getDrawable())
                                    .into(vh.img);
                        }

                        @Override
                        public void onError() {

                        }
                    });
            picasso.fetch(s.sticker);

//                    , new Callback() {
//                        @Override
//                        public void onSuccess() {
//                            picasso.loadPhoto(s.sticker, true)
//                                    .placeholder(vh.img.getDrawable())
//                                    .into(vh.img);
//                        }
//
//                        @Override
//                        public void onError() {
//
//                        }
//                    });
        }
    }
}
