package com.example.apptimetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {

    private List<AppUsageInfo> usageList;
    private OnItemClickListener listener; // Listener for clicks

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(AppUsageInfo item);
    }

    public AppUsageAdapter(List<AppUsageInfo> usageList, OnItemClickListener listener) {
        this.usageList = usageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_app_usage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppUsageInfo info = usageList.get(position);
        // Pass the item and listener to the ViewHolder
        holder.bind(info, listener);
    }

    @Override
    public int getItemCount() {
        return usageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView usageTime;

        public ViewHolder(View view) {
            super(view);
            appIcon = view.findViewById(R.id.image_view_app_icon);
            appName = view.findViewById(R.id.text_view_app_name);
            usageTime = view.findViewById(R.id.text_view_usage_time);
        }

        // Bind data and set the click listener
        public void bind(final AppUsageInfo item, final OnItemClickListener listener) {
            appIcon.setImageDrawable(item.appIcon);
            appName.setText(item.appName);
            usageTime.setText(item.formattedUsageTime);
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}