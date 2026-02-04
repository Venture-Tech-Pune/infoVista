package com.example.infovista.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.infovista.R;
import com.example.infovista.models.Notice;
import com.example.infovista.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

    public interface OnNoticeClickListener {
        void onNoticeClick(Notice notice);
    }

    private Context context;
    private List<Notice> notices;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private OnNoticeClickListener clickListener;

    public NoticeAdapter(Context context, List<Notice> notices, OnNoticeClickListener clickListener) {
        this.context = context;
        this.notices = notices;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notice notice = notices.get(position);

        holder.tvTitle.setText(notice.getTitle());
        holder.tvDescription.setText(notice.getDescription());
        holder.tvCategory.setText(notice.getCategory().toUpperCase());
        
        // Priority badge
        String priority = notice.getPriority().toUpperCase();
        holder.tvPriority.setText(priority);
        
        // Priority color
        int priorityColor;
        switch (notice.getPriority().toLowerCase()) {
            case "urgent":
                priorityColor = context.getResources().getColor(R.color.priority_urgent);
                break;
            case "high":
                priorityColor = context.getResources().getColor(R.color.priority_high);
                break;
            case "medium":
                priorityColor = context.getResources().getColor(R.color.priority_medium);
                break;
            default:
                priorityColor = context.getResources().getColor(R.color.priority_low);
        }
        holder.tvPriority.setBackgroundColor(priorityColor);

        // Date
        try {
            Date createdDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .parse(notice.getCreatedAt());
            if (createdDate != null) {
                holder.tvDate.setText(dateFormat.format(createdDate));
            }
        } catch (Exception e) {
            holder.tvDate.setText(notice.getCreatedAt());
        }

        // Load media if available
        if (notice.getMediaUrl() != null && !notice.getMediaUrl().isEmpty() 
                && notice.getMediaType() != null) {
            holder.ivMedia.setVisibility(View.VISIBLE);
            
            // Construct proper media URL
            String mediaUrl = Constants.BASE_URL + notice.getMediaUrl().substring(1); // Remove leading slash
            
            if (notice.getMediaType().equals("image")) {
                Glide.with(context)
                        .load(mediaUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(holder.ivMedia);
            } else if (notice.getMediaType().equals("video")) {
                // Glide can handle video URLs to pull a frame
                Glide.with(context)
                        .asBitmap()
                        .load(mediaUrl)
                        .frame(1000000) // Get frame at 1 second
                        .placeholder(R.drawable.ic_video_placeholder)
                        .error(R.drawable.ic_video_placeholder)
                        .centerCrop()
                        .into(holder.ivMedia);
            } else {
                holder.ivMedia.setVisibility(View.GONE);
            }
        } else {
            holder.ivMedia.setVisibility(View.GONE);
        }

        // Creator info
        if (notice.getCreatedBy() != null) {
            holder.tvCreator.setText("By: " + notice.getCreatedBy().getName());
        }

        // Card click listener
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNoticeClick(notice);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }

    public void updateNotices(List<Notice> newNotices) {
        this.notices = newNotices;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvDescription, tvPriority, tvCategory, tvDate, tvCreator;
        ImageView ivMedia;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCreator = itemView.findViewById(R.id.tvCreator);
            ivMedia = itemView.findViewById(R.id.ivMedia);
        }
    }
}
