package moe.lolis.metroirc;

import org.pircbotx.PircBotX;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MoeService extends Service implements ServiceEventListener{
	private final IBinder binder = new LocalBinder();
	ServiceEventListener connectedServiceEventListener;
	private IRCListener listener;

	public class LocalBinder extends Binder {
		public MoeService getService() {
			return MoeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	ConnectTask connectionTask;
	PircBotX bot;

	// Temporary, single channel, blah
	public Channel channel;

	@Override
	public void onCreate() {
		channel = new Channel();
		
		listener = new IRCListener(this);
		connectionTask = new ConnectTask();
		connectionTask.execute(new Object[] {});
		super.onCreate();
	}

	// Asynchronous connect (Can't have UI networking)
	private class ConnectTask extends AsyncTask<Object, Void, Boolean> {
		protected Boolean doInBackground(Object... o) {
			bot = new PircBotX();
			bot.setName("MoeLoliBot");
			bot.getListenerManager().addListener(listener);
			try {
				bot.connect("irc.rizon.net");
			} catch (Exception ex) {
				Log.e("whoops", ex.getMessage());
				return false;
			}
			bot.joinChannel("#metroirc");
			return true;
		}

		protected void onPostExecute(Boolean succesful) {
			if (succesful) {
				channel = new Channel();
				channel.channel = bot.getChannel("#metroirc");
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// Service started
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d("Moe Service", "Kyun~");
	}

	@Override
	public void messageRecieved(Channel channel) {
		connectedServiceEventListener.messageRecieved(channel);
	}

}
