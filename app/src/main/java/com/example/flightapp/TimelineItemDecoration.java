package com.example.flightapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class TimelineItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;
    private final int defaultColor;
    private final int highlightColor;
    private final int lineWidth;

    public TimelineItemDecoration(Context context) {
        paint = new Paint();
        paint.setStrokeCap(Paint.Cap.ROUND);
        // Convert 2dp to pixels for line width
        lineWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources().getDisplayMetrics());
        paint.setStrokeWidth(lineWidth);

        defaultColor = ContextCompat.getColor(context, android.R.color.black);
        highlightColor = ContextCompat.getColor(context, android.R.color.holo_blue_dark);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount - 1; i++) {
            View currentChild = parent.getChildAt(i);
            View nextChild = parent.getChildAt(i + 1);

            TimelineItem currentItem = (TimelineItem) currentChild.getTag();
            TimelineItem nextItem = (TimelineItem) nextChild.getTag();
            if (currentItem == null || nextItem == null)
                continue;

            View circleCurrent = currentChild.findViewById(R.id.circleView);
            View circleNext = nextChild.findViewById(R.id.circleView);
            if (circleCurrent == null || circleNext == null)
                continue;

            // Calculate the center of the current circle
            int currentCenterX = circleCurrent.getLeft() + circleCurrent.getWidth() / 2;
            int currentCenterY = circleCurrent.getTop() + circleCurrent.getHeight() / 2;

            // Calculate the center of the next circle
            int nextCenterX = circleNext.getLeft() + circleNext.getWidth() / 2;
            int nextCenterY = circleNext.getTop() + circleNext.getHeight() / 2;

            // Set the color of the line: highlight if both items are completed
            if (currentItem.isCompleted() && nextItem.isCompleted()) {
                paint.setColor(highlightColor);
            } else {
                paint.setColor(defaultColor);
            }

            // Draw a continuous line connecting the centers of the circles
            canvas.drawLine(currentCenterX, currentCenterY, nextCenterX, nextCenterY, paint);
        }
    }
}
