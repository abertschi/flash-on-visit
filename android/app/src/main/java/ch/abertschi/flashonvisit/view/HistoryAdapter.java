package ch.abertschi.flashonvisit.view;

import android.content.Context;
import android.graphics.Rect;
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

import java.util.Collections;
import java.util.List;

import ch.abertschi.flashonvisit.HistoryEntry;
import ch.abertschi.flashonvisit.R;

/**
 * History RecycleView
 * 
 * <p>
 * Created by abertschi on 09.02.17.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private DateFormat mDateFormat = new android.text.format.DateFormat();
    private Context mContext;
    private List<HistoryEntry> mModel;
    private RecyclerView mRecyclerView;

    public HistoryAdapter(List<HistoryEntry> model, Context context, RecyclerView recyclerView) {
        this.mModel = model;
        this.mContext = context;
        this.mRecyclerView = recyclerView;
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
        HistoryEntry entry = mModel.get(position);
        String date = mDateFormat.format("d MMM. yy", entry.getDate()).toString();
        String time = mDateFormat.format("hh:mm:ss a", entry.getDate()).toString();

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
        holder.image.setImageDrawable(ContextCompat.getDrawable(this.mContext, imageId));
    }

    @Override
    public int getItemCount() {
        return this.mModel.size();
    }

    public void add(int position, HistoryEntry item) {
        mModel.add(position, item);
        notifyItemInserted(position);
        mRecyclerView.scrollToPosition(0);
    }

    public void addAtFront(HistoryEntry item) {
        add(0, item);
    }

    public void addAtEnd(HistoryEntry item) {
        mModel.add(item);
        notifyDataSetChanged();
    }

    public void remove(HistoryEntry item) {
        int position = mModel.indexOf(item);
        mModel.remove(position);
        notifyItemRemoved(position);
    }

    public List<HistoryEntry> getModel() {
        return Collections.unmodifiableList(this.mModel);
    }

    public void clearModel() {
        int size = this.mModel.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.mModel.remove(0);
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
            image.setImageDrawable(ContextCompat.getDrawable(this.context, imageId));
        }
    }
}

