package ru.korniltsev.telegram.attach_panel;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import ru.korniltsev.telegram.core.picasso.RxGlide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecentImagesAdapter extends RecyclerView.Adapter<RecentImagesAdapter.VH> {
    final Context ctx;
    private final LayoutInflater viewFactory;
    final RxGlide glide;
    final List<Item> recentImages = new ArrayList<>();
    final Callback cb;

    public RecentImagesAdapter(Context ctx, RxGlide glide, Callback cb) {
        this.ctx = ctx;
        this.glide = glide;
        this.cb = cb;
        viewFactory = LayoutInflater.from(ctx);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(viewFactory.inflate(R.layout.item_recent_image, parent, false));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(
                recentImages.get(position));
    }

    @Override
    public int getItemCount() {
        return recentImages.size();
    }

    public void addAll(List<String> response) {
        for (String s : response) {
            recentImages.add(new Item(s));
        }
        notifyDataSetChanged();
    }

    class VH extends RecyclerView.ViewHolder{
        public static final int DURATION = 80;
        final ImageView img;
        final View attachCheck;
        public VH(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.img);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Item item = recentImages.get(getPosition());
                    item.selected = !item.selected;
                    animateChange(item);
                }
            });
            attachCheck = itemView.findViewById(R.id.attach_check);
        }

        private void animateChange(Item item) {
            attachCheck.clearAnimation();
            if (item.selected) {
                attachCheck.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(DURATION);
            } else {
                attachCheck.animate()
                        .scaleX(0.2f)
                        .scaleY(0.2f)
                        .alpha(0f)
                .setDuration(DURATION);
            }
            dispatchItemSelection();
        }


        public void bind(Item path) {
            glide.getPicasso().load(new File(path.image))
                    .fit()
                    .centerCrop()
                    .into(img);
            attachCheck.clearAnimation();
            if (path.selected){
                attachCheck.setScaleX(1f);
                attachCheck.setScaleY(1f);
                attachCheck.setAlpha(1f);
            } else {
                attachCheck.setScaleX(0.2f);
                attachCheck.setScaleY(0.2f);
                attachCheck.setAlpha(0f);
            }
        }
    }

    class Item {
        final String image;
        boolean selected;

        Item(String image) {
            this.image = image;
        }
    }


    private void dispatchItemSelection() {
        ArrayList<String> selectedImages = getSelectedImages();
        cb.imagesSelected(selectedImages.size());
    }

    public ArrayList<String> getSelectedImages() {
        ArrayList<String> res = new ArrayList<>();
        for (Item recentImage : recentImages) {
            if (recentImage.selected) {
                res.add(recentImage.image);
            }
        }
        return res;
    }

    interface Callback {
        void imagesSelected(int count);
    }
}
