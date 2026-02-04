package com.example.infovista.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.infovista.R;
import com.example.infovista.models.Notice;
import com.example.infovista.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class BoardNoticeAdapter extends RecyclerView.Adapter<BoardNoticeAdapter.ViewHolder> {

    private List<Notice> notices = new ArrayList<>();

    public void setNotices(List<Notice> notices) {
        this.notices = notices != null ? notices : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_board_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notice notice = notices.get(position);
        holder.bind(notice);
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvCategory, tvDuration;
        ImageView ivMedia;
        View priorityBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            priorityBar = itemView.findViewById(R.id.priorityBar);
        }

        void bind(Notice notice) {
            // Title
            tvTitle.setText(notice.getTitle());

            // Description
            tvDescription.setText(notice.getDescription());

            // Category
            tvCategory.setText("• " + notice.getCategory().toUpperCase());

            // Duration
            tvDuration.setText(notice.getDisplayDuration() + "s");

            // Priority color
            int priorityColor = getPriorityColor(notice.getPriority());
            priorityBar.setBackgroundColor(priorityColor);
            tvCategory.setTextColor(priorityColor);

            // Media
            if (notice.getMediaUrl() != null && !notice.getMediaUrl().isEmpty()
                    && notice.getMediaType() != null) {
                ivMedia.setVisibility(View.VISIBLE);

                // Construct media URL
                String mediaUrl = notice.getMediaUrl().startsWith("/")
                        ? notice.getMediaUrl().substring(1)
                        : notice.getMediaUrl();
                String fullMediaUrl = Constants.BASE_URL + mediaUrl;

                if ("image".equals(notice.getMediaType())) {
                    Glide.with(itemView.getContext())
                            .load(fullMediaUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(ivMedia);
                } else if ("video".equals(notice.getMediaType())) {
                    Glide.with(itemView.getContext())
                            .asBitmap()
                            .load(fullMediaUrl)
                            .frame(1000000)
                            .placeholder(R.drawable.ic_video_placeholder)
                            .error(R.drawable.ic_video_placeholder)
                            .into(ivMedia);
                } else {
                    ivMedia.setVisibility(View.GONE);
                }

                // Show only 1 line of description if media exists
                tvDescription.setMaxLines(1);
            } else {
                ivMedia.setVisibility(View.GONE);
                // Show 2 lines of description if no media
                tvDescription.setMaxLines(2);
            }
        }

        private int getPriorityColor(String priority) {
            if (priority == null) priority = "medium";

            switch (priority.toLowerCase()) {
                case "urgent":
                    return itemView.getContext().getResources().getColor(R.color.priority_urgent);
                case "high":
                    return itemView.getContext().getResources().getColor(R.color.priority_high);
                case "medium":
                    return itemView.getContext().getResources().getColor(R.color.priority_medium);
                case "low":
                default:
                    return itemView.getContext().getResources().getColor(R.color.priority_low);
            }
        }
    }
}
