package moe.lolis.metroirc;

import java.util.ArrayList;
import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ChannelActivity extends ListActivity {
	LayoutInflater inflater;
	MessageAdapter adapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_layout);
        
        //Temporary fake list
        ArrayList<ChannelMessage> messages = new ArrayList<ChannelMessage>();
        ChannelMessage m = new ChannelMessage();
        m.text = "Test";
        m.time = new Date();
        messages.add(m);
        messages.add(m);
        messages.add(m);
        
        //Set up list adapter
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        adapter = new MessageAdapter(getApplicationContext(), R.layout.channel_message_row, messages);
		setListAdapter(adapter);
        
    }
    
    //Adapter that handles the message list
    private class MessageAdapter extends ArrayAdapter<ChannelMessage> {

		private ArrayList<ChannelMessage> items;

		public MessageAdapter(Context context, int textViewResourceId, ArrayList<ChannelMessage> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView==null)
			{
				convertView = inflater.inflate(R.layout.channel_message_row, null);
			}
			ChannelMessage message = items.get(position);
			TextView name = (TextView)convertView.findViewById(R.id.channelMessageName);
			TextView content = (TextView)convertView.findViewById(R.id.channelMessageContent);
			
			content.setText(message.text);
			
			return convertView;
		}
    }
}