package cyua.android.smsserver;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import cyua.android.core.AppCore;
import cyua.android.core.BackupAgent;
import cyua.android.core.db.DbService;
import cyua.android.core.inet.InetService;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.location.LocationService;
import cyua.android.core.log.LogService;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;
import cyua.java.shared.RmiTargetInterface;


public class App extends AppCore {

private static final String TAG = App.class.getSimpleName();
// SINGLETONE instance
protected static App I;

static {
	execType = ExecType.TEST;
	rmiType = RMIType.REMOTE;
	// WARNING RELEASE set execType to REAL in release
	if (execType == ExecType.TEST && !cyua.android.core.BuildConfig.DEBUG) execType = ExecType.REAL;
	//WARNING RELEASE set rmiType to REMOTE in release
	if (execType == ExecType.REAL) rmiType = RMIType.REMOTE;

}

@Override public void onCreate() {
	super.onCreate();
	I = this;
}

@Override public void registerServices() {
	try {
		App.addService(LogService.instantiate().setPersistAgentClass(null), false);
		LogService.setPersistAgentClass(LogPersistAgent.class);
		App.addService(KeepAliveService.instantiate().keepCpu().keepMemo().notification(MainActivity.class, R.drawable.status_logo, R.string.app_name, R.string.notification_message), false);
//		App.addService(DbService.instantiate(Db.class), false);
		App.addService(InetService.instantiate(), false);
		App.addService(UiService.instantiate(Ui.class, UiState.class), false);
		App.addService(SmsService.instantiate(), false);
	} catch (Throwable ex) {Wow.e(ex);}
}

public static void wakeup() {
	if (App.isRecycled()) App.I.preInit();
}

@Override protected void preInit() {
	super.preInit();
	startInit();
}
@Override protected void startInit() {
	// Because BackupManager start restore right before Activity.createRoot
	if (!state.has(Bit.INIT)) Settings.initCache();
	super.startInit();
}
@Override protected void finishInit() {
	super.finishInit();
	if (UiService.getUi() != null) UiService.getUi().activate();
}
@Override protected void finishExit() {
	super.finishExit();
	BackupAgent.requestBackup();
}





/** MISK* */

public static String getDeviceUID(boolean init) {
	//TODO
	String dvid = null;// Cache.getString(UID_CACHE);
	if (!Tool.isEmpty(dvid) || !init) return dvid;
	// init
	try {
		TelephonyManager tm = (TelephonyManager) App.context().getSystemService(Context.TELEPHONY_SERVICE);
		dvid = tm.getDeviceId();
	} catch (Exception ex) {Log.w(TAG, "[getDeviceID Error]:" + Log.getStackTraceString(ex));}
	//
	if (dvid == null) dvid = Tool.randomString(2) + "-" + Tool.randomString(4);
	//
	return dvid;//Cache.set(UID_CACHE, dvid);
}

public static String getRmiUrl() {
	if (App.execType == App.ExecType.REAL) App.rmiType = App.RMIType.REMOTE;
	//
	String appName = UiHelper.string(R.string.app_projectid);
	String localIP = "192.168.1.105";//"192.168.1.105";
	String localIP_emu = "10.0.2.2";// EMULATOR
	//localIP = "31.40.210.76";// MODEM
	StringBuilder url = new StringBuilder("http://");
	if (App.rmiType == App.RMIType.LOCAL) url.append(localIP + ":12313");//configured on router port forwarding page
	else if (App.rmiType == App.RMIType.LOCAL_EMU) url.append(localIP_emu + ":8888");
	else url.append(App.execType == App.ExecType.REAL ? appName : appName/*-test*/).append(".appspot.com");
	url.append(RmiTargetInterface.GATE_DIR);
	//
	return url.toString();
}

static String getPhone() {
	String phone = "";
	try {
		TelephonyManager tm = (TelephonyManager) App.context().getSystemService(Context.TELEPHONY_SERVICE);
		phone = tm.getLine1Number();
	} catch (Exception ex) {Wow.e(ex);}
	if (D) Wow.v(TAG, "getPhone", phone);
	return phone;
}

}
