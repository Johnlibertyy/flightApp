package com.example.flightapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private List<TimelineItem> timelineItems;

    public TimelineAdapter(List<TimelineItem> timelineItems) {
        this.timelineItems = timelineItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        TimelineItem item = timelineItems.get(position);
        holder.titleText.setText(item.getTitle());
        holder.subTitleText.setText(item.getSubTitle());
        holder.timeText.setText(item.getTime());

        // Set the circle drawable based on the isCompleted flag
        if (item.isCompleted()) {
            holder.circleView.setImageResource(R.drawable.circle_filled_blue);
        } else {
            holder.circleView.setImageResource(R.drawable.circle_empty);
        }

        // On click: mark all items with index <= clicked index as completed.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    for (int i = 0; i < timelineItems.size(); i++) {
                        timelineItems.get(i).setCompleted(i <= pos);
                    }
                    notifyDataSetChanged();
                }
            }
        });

        // Tag the item view with its TimelineItem for use in the ItemDecoration.
        holder.itemView.setTag(item);
    }

    @Override
    public int getItemCount() {
        return timelineItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView circleView;
        TextView titleText, subTitleText, timeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Only the circle and text views are needed.
            circleView = itemView.findViewById(R.id.circleView);
            titleText = itemView.findViewById(R.id.titleText);
            subTitleText = itemView.findViewById(R.id.subTitleText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}
