package org.span.manager;

import java.util.ArrayList;
import java.util.List;

import org.span.R;
import org.span.service.vanetsex.VANETEvent;
import org.span.service.vanetsex.VANETMessage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class VANETMessagesAdapter extends BaseAdapter {

    static class ViewHolder {
        public TextView textView_message;
        public ImageView imageView_icon;
    }

    protected static final String TAG = VANETMessagesAdapter.class.getSimpleName();

    private List<VANETMessage> listMessages;
    private LayoutInflater inflater;
    private Context context;

    public VANETMessagesAdapter(Context context) {
        this.context = context;
        this.listMessages = new ArrayList<VANETMessage>();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return listMessages.size();
    }

    @Override
    public Object getItem(int idx) {
        return listMessages.get(idx);
    }

    @Override
    public long getItemId(int idx) {
        return idx;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        VANETMessage msg = (VANETMessage) getItem(position);

        if (rowView == null) {
            rowView = inflater.inflate(R.layout.vanet_item_textview, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.textView_message = (TextView) rowView.findViewById(R.id.textView_message);
            viewHolder.imageView_icon = (ImageView) rowView.findViewById(R.id.imageView_icon);
            rowView.setTag(viewHolder);
        }

        // fill data
        ViewHolder holder = (ViewHolder) rowView.getTag();

        if (msg.getType() == VANETMessage.TYPE_BEACON) {
            if (msg.isIncoming()) {
                holder.textView_message.setText(msg.getStringAddressSource());
                holder.imageView_icon.setImageResource(R.drawable.vse_incoming_beacon);
            } else {
                holder.textView_message.setText(msg.getStringAddressDestination());
                holder.imageView_icon.setImageResource(R.drawable.vse_outgoing_beacon);
            }

        } else if (msg.getType() == VANETMessage.TYPE_EVENT) {
            VANETEvent event = (VANETEvent) msg.getData();
            if (msg.isIncoming()) {
                holder.textView_message.setText(msg.getStringAddressSource() + " ("
                        + event.getStringOriginatorAddress() + ") " + event.getTypeTitle());
                holder.imageView_icon.setImageResource(R.drawable.vse_incoming);
            } else {
                holder.textView_message.setText(msg.getStringAddressDestination() + " " + event.getTypeTitle());
                holder.imageView_icon.setImageResource(R.drawable.vse_outgoing);
            }

        } else {
            if (msg.isIncoming()) {
                holder.textView_message.setText("Unknow type: [" + Byte.toString(msg.getType()) + "] from: "
                        + msg.getStringAddressSource());
                holder.imageView_icon.setImageResource(R.drawable.vse_message_incoming);
            } else {
                holder.textView_message.setText("Unknow type: [" + Byte.toString(msg.getType()) + "] to: "
                        + msg.getStringAddressDestination());
                holder.imageView_icon.setImageResource(R.drawable.vse_message_outgoing);
            }
        }

        return rowView;
    }
    
    public void setData(List<VANETMessage> listMessages) {
        this.listMessages = listMessages;
        notifyDataSetChanged();
    }

}
