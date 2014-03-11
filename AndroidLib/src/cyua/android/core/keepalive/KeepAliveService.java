package cyua.android.core.keepalive;

import cyua.android.core.AppCore;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.log.Wow;
import cyua.java.shared.BitState;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiHelper;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.AppCore.exit;

import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

//WARNING To use KeepAliveSrvice it should be added to MANIFEST as Service

public class KeepAliveService extends AppService {
private static final String TAG = KeepAliveService.class.getSimpleName();
// SINGLETON
private static KeepAliveService I;
//
private enum Opt {
	SCREEN, MEMO, CPU,
	KEEPATSTART, // Start onConnected at App init
	RESTARTONKILL, // Start onConnected after user try to kill it through AppSettings
}
;




/** STATIC INIT */

public static KeepAliveService instantiate() {
	if (I != null) return I;
	I = new KeepAliveService();
	return I;
}


public static boolean isKeepAlive() {return I != null && I.daemon != null;}

public static void keepAliveChange(KeepAliveListener listener) {
	if (I != null) I.doKeepAliveChange(listener);
}






/** INSTANCE */
private int notificationIconRid;
private Class<?> notificationClass;
private int notificationMessageRid;
private int notificationTitleRid;
//
private final int CHECK_INTERVAL = 1000;
private final int MAX_CHECK_INTERVAL = 10000;

private enum Op {CHANGE, TIK}

private BitState opts;
private WakeLock cpuLock;
private KeepAliveDaemon daemon;
private Tiker<Op> tiker;
private int checkInterval;
private List<KeepAliveListener> listeners = new ArrayList<KeepAliveListener>();


public KeepAliveService() {
	opts = new BitState();
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			switch (operation) {
				case TIK: doCheckIsRequired(); break;
				case CHANGE: doKeepAliveChange((KeepAliveListener) obj); break;
				default: break;
			}
		}
	};
}




/** OPTIONS */

public KeepAliveService keepScreen() {opts.set(Opt.SCREEN); return this;}
public KeepAliveService keepMemo() {opts.set(Opt.MEMO); return this;}
public KeepAliveService keepCpu() {opts.set(Opt.CPU); return this;}
public KeepAliveService keepAtStart() {opts.set(Opt.KEEPATSTART); return this;}
public KeepAliveService restartOnKill() {opts.set(Opt.RESTARTONKILL); return this;}
public KeepAliveService notification(Class<?> clas, int icon, int title, int msg) {
	notificationClass = clas; notificationIconRid = icon; notificationTitleRid = title; notificationMessageRid = msg;
	return this;
}

private void validate() {
	if (opts.has(Opt.CPU)) opts.set(Opt.MEMO);
}



/** APPSERVICE Methods */

@Override public void onInitStart(AppCore app) {
	validate();
	if (D)
		Wow.i(TAG, "onInitStart", "[started manually]   hold cpuLock:" + (opts.has(Opt.CPU) && (cpuLock == null || !cpuLock.isHeld())));
	if (opts.hasAll(Opt.KEEPATSTART, Opt.MEMO)) doStartKeepAlive();
}

@Override public void onActivityStarted(Activity activity, String cause) {
	if (D) Wow.i(TAG, "onActivityStarted", "opts:" + opts);
	if (opts.has(Opt.SCREEN)) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
}
@Override public void onActivityStopped(Activity activity, String cause) {
	if (D) Wow.i(TAG, "onActivityStopped", "opts:" + opts);
	if (opts.has(Opt.SCREEN)) activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
}

@Override public void onExitStart(AppCore app) {
	if (D) Wow.i(TAG, "onExitStart", "release cpuLock:" + (opts.has(Opt.CPU) && cpuLock != null && cpuLock.isHeld()));
	doStopKeepAlive();
}
@Override public boolean isExited(AppCore app) {
	return daemon == null;
}

@Override public String getStateInfo() throws Throwable {
	return "isKeeping = " + (daemon != null);
}




/** MANAGEMENT */

private void doKeepAliveChange(KeepAliveListener listener) {
	if (!AppCore.isMainThread()) tiker.setTik(Op.CHANGE, listener, 0);
	else {
		if (!listeners.contains(listener)) listeners.add(listener);
		checkInterval = CHECK_INTERVAL;
		tiker.setTik(Op.TIK, 0);
	}
}
private void doCheckIsRequired() {
	// increase interval if no outside changes
	tiker.setTik(Op.TIK, checkInterval);
	checkInterval += 500;
	if (checkInterval > MAX_CHECK_INTERVAL) checkInterval = MAX_CHECK_INTERVAL;
	// WARN:  not synchronized
	ListIterator<KeepAliveListener> litr = listeners.listIterator();
	while (litr.hasNext()) {
		KeepAliveListener listener = litr.next();
		boolean required = false;
		try {
			required = listener.isKeepAliveRequired();
		} catch (Exception ex) { }
		if (!required) litr.remove();
	}
	//
	int oldSize = listeners.size();
	if (D)
		Wow.i(TAG, "doCheckIsRequired", "interval = " + checkInterval + ", oldSize = " + oldSize + ",  newSize = " + listeners.size() + ",  started ? " + (daemon != null));
	if (listeners.isEmpty() && daemon != null) doStopKeepAlive();
	else if (! listeners.isEmpty() && daemon == null) doStartKeepAlive();
	else if (listeners.isEmpty() && daemon == null) tiker.clear();
}


private void doStartKeepAlive() {
	Intent intent = new Intent(AppCore.context(), KeepAliveDaemon.class);
	AppCore.context().startService(intent);
	//
	if (opts.has(Opt.CPU)) {
		if (cpuLock == null) {
			PowerManager pm = (PowerManager) AppCore.context().getSystemService(Context.POWER_SERVICE);
			cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		}
		if (!cpuLock.isHeld()) cpuLock.acquire();
	}
}

private void doStopKeepAlive() {
	if (D) Wow.i(TAG, "doStopKeepAlive");
	if (daemon != null) daemon.stopSelf();
	daemon = null;
	tiker.clear();
	listeners.clear();
	//
	if (cpuLock != null && cpuLock.isHeld()) cpuLock.release();
	//
	AppCore.context().checkMayExit();
}






/** FOREGROUND SERVICE */

public static class KeepAliveDaemon extends Service {

	@Override public void onDestroy() {
		if (D) Wow.i(TAG, "onDestroy", "isForceStop:" + (I.daemon != null));
		// User manually tries to kill process via Application Settings
		if (I.daemon != null && I.opts.has(Opt.RESTARTONKILL)) I.doStartKeepAlive();
		I.daemon = null;
	}
	@Override public void onCreate() {
		if (D) Wow.i(TAG, "createRoot");
		I.daemon = this;
		startForeground(1, getNotification());
	}
	@Override public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent != null ? intent.getAction() : "";// there was case with NullPointerException
		if (D) Wow.i(TAG, "onStartCommand", "intent:" + action);
		//
		return Service.START_STICKY;
	}

	//WARNING Override to function properly
	protected Notification getNotification() {
		CharSequence title = UiHelper.string(I.notificationTitleRid);
		CharSequence msg = UiHelper.string(I.notificationMessageRid);
		Notification notification = new Notification(I.notificationIconRid, title, System.currentTimeMillis());
		// pending intent
		Context context = getApplicationContext();
		Intent intent = new Intent(context, I.notificationClass);
		intent.setAction("android.intent.action.MAIN");
		intent.addCategory("android.intent.category.LAUNCHER");
		PendingIntent targIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(context, title, msg, targIntent);
		return notification;
	}

	@Override public IBinder onBind(Intent intent) {
		return null;
	}
}






/** LISTENER */

public static interface KeepAliveListener {
	public boolean isKeepAliveRequired();
}

}

