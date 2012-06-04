package moe.lolis.metroirc;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MoeService extends Service{
	private final IBinder binder = new LocalBinder();
	
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
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	//Service started
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d("Moe Service", "Kyun~");
	}

}
