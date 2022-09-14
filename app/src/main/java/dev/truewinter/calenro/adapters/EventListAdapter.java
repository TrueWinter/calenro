package dev.truewinter.calenro.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.truewinter.calenro.Event;
import dev.truewinter.calenro.R;
import dev.truewinter.calenro.TimeFormat;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
    private List<Event> eventMap;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public EventListAdapter(Context context, List<Event> eventMap) {
        this.mInflater = LayoutInflater.from(context);
        this.eventMap = eventMap;
    }

    // inflates the row layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.event_list_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public synchronized void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String times = "";
        Event event = eventMap.get(position);

        if (event == null) {
            return;
        }

        if (event.isAllDay()) {
            times = "All Day";
        } else {
            TimeFormat.ReadableTimeFormat readableTimeFormat = TimeFormat.getInstance()
                    .createReadableTime(event.getStart(), event.getEnd());

            times = readableTimeFormat.getStart() + " - " + readableTimeFormat.getEnd();
        }

        holder.eventTitle.setText(event.getTitle());
        holder.eventTimes.setText(times);
    }

    // total number of rows
    @Override
    public synchronized int getItemCount() {
        return eventMap.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView eventTitle;
        TextView eventTimes;

        ViewHolder(View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventTimes = itemView.findViewById(R.id.event_times);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            if (mClickListener != null) mClickListener.onLongClick(view, getAdapterPosition());
            return true;
        }
    }

    // convenience method for getting data at click position
    public synchronized Event getIdFromIndex(int index) {
        return eventMap.get(index);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onLongClick(View view, int position);
    }
}
