package aca.com.nanohttpd;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import aca.com.nanohttpd.protocols.http.SysFreePort;

/**
 * 
 * @author lixm
 *
 */
public class HttpService extends Service {

	//private static String TAG = HttpService.class.getSimpleName();
	private static String TAG = "lixm";

	private HttpServerImpl mHttpServer;
	private static int freePort = HttpServerImpl.DEFAULT_SERVER_PORT;

	@Override
	public IBinder onBind(Intent intent) {
		return new httpBinder();
	}

	@Override
	public void onCreate() {
		try {
			freePort = SysFreePort.custom().getPortAndFree();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.i(TAG, "HttpService get free Port:" + freePort);
		mHttpServer = new HttpServerImpl(freePort);
        try {
			mHttpServer.start();
		} catch (IOException e) {
			Log.d(TAG, "onCreate http start error :" ,e);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mHttpServer.stop();
		Log.d(TAG, "onDestroy");
	}

	@Override
	public void onStart(Intent intent, int startid) {
		super.onStart(intent, startid);
		Log.d(TAG, "onStart");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		flags = START_STICKY;
		return super.onStartCommand(intent, flags, startId);
	}

	public class httpBinder extends android.os.Binder{
		public void setTransPath(String data){
			HttpService.this.mHttpServer.setTransPath(data);
		}
		public int getHttpServerPort() {
			return freePort;
		}
		public void setUpgPath(String data){
			HttpService.this.mHttpServer.setUpgPath(data);
		}
	}
}
