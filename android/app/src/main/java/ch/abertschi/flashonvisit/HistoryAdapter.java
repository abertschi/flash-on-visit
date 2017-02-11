package ch.abertschi.flashonvisit;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

/**
 * Created by abertschi on 09.02.17.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryEntry> model;
    private Context context;
    private RecyclerView recyclerView;
    private DateFormat dateFormat = new android.text.format.DateFormat();

    public HistoryAdapter(List<HistoryEntry> model, Context context, RecyclerView recyclerView) {
        this.model = model;
        this.context = context;
        this.recyclerView = recyclerView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_recycle_view, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        HistoryEntry entry = model.get(position);
        String date = dateFormat.format("d MMM. yy", entry.getDate()).toString();
        String time = dateFormat.format("hh:mm:ss a", entry.getDate()).toString();

        holder.dateLabel.setText(date + "\n" + time);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.detailLabel.setText(Html.fromHtml(entry.getMessage(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            holder.detailLabel.setText(Html.fromHtml(entry.getMessage()));
        }

        int imageId;
        if (position == 0) {
            imageId = R.mipmap.history_start;
        } else {
            imageId = R.mipmap.history;
        }
        holder.image.setImageDrawable(ContextCompat.getDrawable(this.context, imageId));
    }

    @Override
    public int getItemCount() {
        return this.model.size();
    }

    public void add(int position, HistoryEntry item) {
        model.add(position, item);
        notifyItemInserted(position);
        recyclerView.scrollToPosition(0);
    }

    public void addAtFront(HistoryEntry item) {
        add(0, item);
    }

    public void addAtEnd(HistoryEntry item) {
        model.add(item);
        notifyDataSetChanged();
    }

    public void remove(HistoryEntry item) {
        int position = model.indexOf(item);
        model.remove(position);
        notifyItemRemoved(position);
    }

    public List<HistoryEntry> getModel() {
        return Collections.unmodifiableList(this.model);
    }

    public void clearModel() {
        int size = this.model.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.model.remove(0);
            }
            this.notifyItemRangeRemoved(0, size);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private TextView dateLabel;
        private TextView detailLabel;

        public ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.history_image);
            dateLabel = (TextView) itemView.findViewById(R.id.history_entry_date);
            detailLabel = (TextView) itemView.findViewById(R.id.history_entry_detail);

        }
    }

    public static class HistoryItemDecorator extends RecyclerView.ItemDecoration {

        private Context context;
        private final HistoryAdapter adapter;

        public HistoryItemDecorator(Context context, HistoryAdapter adapter) {
            this.adapter = adapter;
            this.context = context;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            ImageView image = (ImageView) view.findViewById(R.id.history_image);
            int imageId;

            if (parent.getChildAdapterPosition(view) == 0) {
                if (adapter.getModel().size() == 1) {
                    imageId = R.mipmap.history_thick2;
                } else {
                    imageId = R.mipmap.history_thick2_top;
                }
            } else {
                imageId = R.mipmap.history_thick2;
            }

            System.out.println(imageId);
            image.setImageDrawable(ContextCompat.getDrawable(this.context, imageId));
        }
    }
}

