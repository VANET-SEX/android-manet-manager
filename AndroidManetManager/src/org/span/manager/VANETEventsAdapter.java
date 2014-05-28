package org.span.manager;

import java.util.ArrayList;
import java.util.List;

import org.span.R;
import org.span.service.vanetsex.VANETEvent;
import org.span.service.vanetsex.VANETMessage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class VANETEventsAdapter extends BaseAdapter {

    static class ViewHolder {
        public TextView textView_event;
        public TextView textView_delay;
        public TextView textView_src_address;
    }

    protected static final String TAG = VANETEventsAdapter.class.getSimpleName();

    private List<VANETEvent> listEvents;
    private LayoutInflater inflater;
    private Context context;

    public VANETEventsAdapter(Context context) {
        this.context = context;
        this.listEvents = new ArrayList<VANETEvent>();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return listEvents.size();
    }

    @Override
    public Object getItem(int idx) {
        return listEvents.get(idx);
    }

    @Override
    public long getItemId(int idx) {
        return idx;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        VANETEvent event = (VANETEvent) getItem(position);

        if (rowView == null) {
            rowView = inflater.inflate(R.layout.vanet_item_textview, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.textView_event = (TextView) rowView.findViewById(R.id.textView_event);
            viewHolder.textView_delay = (TextView) rowView.findViewById(R.id.textView_delay);
            viewHolder.textView_src_address = (TextView) rowView.findViewById(R.id.textView_src_address);
            rowView.setTag(viewHolder);
        }

        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();

        if (event != null) {
            holder.textView_event.setText(event.getType() + "(" + event.getId() + ")");
            holder.textView_delay.setText("<no_data>");
            holder.textView_src_address.setText(event.getStringOriginatorAddress());
        }

        return rowView;
    }
    
    public void setData(List<VANETEvent> listEvents) {
        this.listEvents = listEvents;
        notifyDataSetChanged();
    }

}
