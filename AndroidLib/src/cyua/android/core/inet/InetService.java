package cyua.android.core.inet;

import cyua.android.core.AppCore;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.misc.Listeners;
import cyua.android.core.misc.Sequence;
import cyua.android.core.misc.Tiker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.HandlerThread;

import static cyua.android.core.AppCore.D;

import cyua.android.core.log.Wow;


/* WARNING PERMISSION required:
 * android.permission.ACCESS_NETWORK_STATE
 * android.permission.INTERNET*/
public class InetService extends AppService {
private static final String TAG = InetService.class.getSimpleName();
static final String INET_THREAD_NAME = "Inet_Thread";
//
static final int LONG_CHECK_SPAN = 60000;// 1 minute
static final int SHORT_CHECK_SPAN = 4000;
//
private static InetService I;
//



/** **   STATIC */

public static AppService instantiate() {
	if (I != null) return I;
	I = new InetService();
	I.initOrder = AppService.INIT_FIRST;
	return I;
}

public static boolean isOnline() {
	return I != null && I.online;
}

public static boolean isMobileAvailable() {
	ConnectivityManager cm = (ConnectivityManager) AppCore.context().getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	if (info != null && info.isAvailable()) return true;
	else {
		info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
		return info != null && info.isAvailable();
	}
}

public static void addListener(NetStateListener listener, boolean recieveNow) {
	if (I == null || !I.listeners.add(listener)) return;
	if (recieveNow) try {listener.onlineStatusChanged(I.online, false);} catch (Throwable ex) {Wow.e(ex);}
}
public static void removeListener(NetStateListener listener) {
	if (I != null) I.listeners.remove(listener);
}




/** *   INSTANCE */

private enum Op {
	CHECK
}

private BroadcastReceiver receiver;
private Tiker<Op> mainTiker;
//private Tiker<Op> inetTiker;
HandlerThread inetThread;
private boolean online;
private Listeners<NetStateListener> listeners = new Listeners<NetStateListener>();
private int checkSpan = LONG_CHECK_SPAN;
private int lastType = -1;




/** **   SERVICE LIFE */

@Override public void onInitStart(AppCore app) {
	online = false;
	//
	inetThread = new HandlerThread(INET_THREAD_NAME);
	inetThread.start();
	//
//	inetTiker = new Tiker<Op>(TAG, inetThread.getLooper()) {
//		@Override public void handleTik(Op operation, Object obj, Bundle data)
//		{
//			switch (operation) {
//				default: break;
//			}
//		}
//	};
	//
	mainTiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			switch (operation) {
				case CHECK: checkOnline(); break;
			}
		}
	};
	//
	receiver = new BroadcastReceiver() {
		@Override public void onReceive(Context context, Intent intent) {
			boolean isOnline = isRealyOnline();
			if (online && !isOnline) fireEvent(false, false);// TODO find reason
			else if (!online && isOnline) {
				checkSpan = SHORT_CHECK_SPAN;
				mainTiker.setTik(Op.CHECK, checkSpan);
			}
			if (D) Wow.i(TAG, "onReceive", "online:" + online + ",  isOnline:" + isOnline);
		}
	};
	//
	AppCore.context().registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	//
	checkOnline();
}

@Override public void onExitFinish(AppCore app) {
	try {
		online = false;
		listeners.clear();
		if (inetThread != null) inetThread.quit();
//		if (inetTiker != null) {
//			inetTiker.clear();
//			inetTiker.getLooper().quit();
//		}
		if (mainTiker != null) mainTiker.clear();
		try { AppCore.context().unregisterReceiver(receiver); } catch (Exception ex) {}
	} finally {
//		inetTiker = null;
		inetThread = null;
		mainTiker = null;
		receiver = null;
	}
}

@Override public String getStateInfo() throws Throwable {
	return "online = " + online;
}





/** **   MISC */

private void fireEvent(boolean isOnline, boolean byUser) {
	online = isOnline;
	if (D) Wow.i(TAG, "fireEvent", "online:" + isOnline + ", byUser:" + byUser + ", listrs size:" + listeners.size());
	while (listeners.hasNext()) {
		try {listeners.next().onlineStatusChanged(isOnline, byUser);} catch (Throwable ex) {Wow.e(ex);}
	}
	//
	if (!online) {
		checkSpan = LONG_CHECK_SPAN;
		mainTiker.setTik(Op.CHECK, checkSpan);
	}
	else {
		mainTiker.cancelTik(Op.CHECK);
		lastType = getConnectionType();
	}
}

private int getConnectionType() {
	ConnectivityManager cm = (ConnectivityManager) AppCore.context().getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo netInfo = cm.getActiveNetworkInfo();
	return netInfo != null ? netInfo.getType() : -1;
}
private boolean isRealyOnline() {
	ConnectivityManager cm = (ConnectivityManager) AppCore.context().getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo netInfo = cm.getActiveNetworkInfo();
	return netInfo != null && netInfo.isConnected();
}
private void checkOnline() {
	boolean isOnline = isRealyOnline();
	if (isOnline != online) fireEvent(isOnline, false);
	else if (!isOnline) mainTiker.setTik(Op.CHECK, checkSpan);
}








/** *   LISTENER */

public static interface NetStateListener {
	public abstract void onlineStatusChanged(boolean isOnline, boolean byUser);
}







/** *   INTER THREAD CALLBACK INTERFACE */

public static abstract class InetSequence extends Sequence {
	public InetSequence() {
		super(I.inetThread.getLooper());
	}
}


}
