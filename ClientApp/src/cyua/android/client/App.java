package cyua.android.client;

import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import cyua.android.core.AppCore;
import cyua.android.core.BackupAgent;
import cyua.android.core.db.DbService;
import cyua.android.core.inet.InetService;
import cyua.android.core.inet.RmiUtils;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.location.LocationService;
import cyua.android.core.log.LogService;
import cyua.android.core.log.Wow;
import cyua.android.core.map.MapService;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;
import cyua.java.shared.Phantom;
import cyua.java.shared.RmiTargetInterface;

import static android.provider.Settings.Secure;
import static cyua.java.shared.Phantom.Client;


public class App extends AppCore implements InetService.NetStateListener {
static final String SERVER_KEY = Client.APP_SERVER_APIKEY;

private static final String TAG = App.class.getSimpleName();
private static Boolean hasTelephony;

private boolean hasServerData;

static {
	execType = ExecType.TEST;
	rmiType = RMIType.REMOTE;
	// WARNING RELEASE set execType to REAL in release
	if (execType == ExecType.TEST && !cyua.android.core.BuildConfig.DEBUG) execType = ExecType.REAL;
	//WARNING RELEASE set rmiType to REMOTE in release
	if (execType == ExecType.REAL) rmiType = RMIType.REMOTE;
}

@Override public void registerServices() {
	try {
		App.addService(LogService.instantiate(), false);
		LogService.setPersistAgentClass(LogPersistAgent.class);
		App.addService(KeepAliveService.instantiate().keepCpu().keepMemo().notification(MainActivity.class, R.drawable.status_logo, R.string.app_name, R.string.notification_message), false);
		App.addService(DbService.instantiate(Db.class), false);
		App.addService(InetService.instantiate(), false);
		App.addService(UiService.instantiate(Ui.class, UiState.class), false);
		App.addService(LocationService.instantiate(), false);// TODO tiks after exit
		if (isPlayServiceAvailable()) {
			App.addService(MapService.instantiate(Mapa.class), false);
		}
		} catch (Throwable ex) {Wow.e(ex);}
}

static boolean isPlayServiceAvailable() {
	return getGooglePlayServiceStatus() == ConnectionResult.SUCCESS;
}
static int getGooglePlayServiceStatus() {
	return GooglePlayServicesUtil.isGooglePlayServicesAvailable(AppCore.context());
}

@Override protected void startInit() {
	// Because BackupManager start restore right before Activity.createRoot
	if (!state.has(Bit.INIT)) Settings.initCache();
	super.startInit();
}
@Override protected void finishInit() {
	super.finishInit();
	if (UiService.getUi() != null) UiService.getUi().activate();
	InetService.addListener(this, true);
}
@Override protected void finishExit() {
	super.finishExit();
//	if (SendTask.isFailed())
	BackupAgent.requestBackup();
	hasServerData = false;
}

@Override public void onlineStatusChanged(boolean isOnline, boolean byUser) {
	if (!isOnline || hasServerData) return;
	// INIT from server
	new InetService.InetSequence() {
		@Override protected void doInBackground() throws Exception {
			InitRmi rmi = new InitRmi();
			rmi.request.uid = Settings.uid.get();
			RmiUtils.rmiRequest(rmi, App.getRmiUrl(), InitRmi.Response.class);
			if (rmi.isSuccess() && Tool.notEmpty(rmi.response)) {
				hasServerData = true;
				Settings.operators.set(rmi.response.phones);
				Settings.types.set(rmi.response.types);
				Settings.S3_KEY_ID = rmi.response.p1;
				Settings.S3_KEY = rmi.response.p2;
				Settings.S3_BUCKET = rmi.response.p3;
				Settings.S3_REGION = rmi.response.p4;
				Settings.S3_HOST = rmi.response.p5;
				Settings.S3_DIR = rmi.response.p6;
			}
			InetService.removeListener(App.this);
		}
	};
}






/** MISK* */

static String initDeviceUID() {
	String uid = "";
	int type = 0;
	try {
		TelephonyManager tm = (TelephonyManager) App.context().getSystemService(Context.TELEPHONY_SERVICE);
		// try IMEI
		uid = tm.getDeviceId();
		type = 1;
		if (Tool.isEmpty(uid)) {
			type = 2;
			uid = Secure.getString(App.context().getContentResolver(), Secure.ANDROID_ID);
		}
		if (Tool.isEmpty(uid) || "9774d56d682e549c".equals(uid)) throw new Exception();
		uid = Phantom.convertRadix(uid, 0, 0);
	} catch (Exception ex) {
		type = 0;
		uid = App.deviceUidVar.get();
		if (uid == null) uid = Tool.randomString(8);
	}
	App.deviceUidVar.set(uid);
	//
	if (D) Wow.v(TAG, "initDeviceUID", "uid = " + uid, "typeIndex = " + type);
	return type + uid;
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
static boolean isPhoneReal() {
	String phone = Settings.phone.get();
	return phone.startsWith("+");
}

public static String getRmiUrl() {
	if (App.execType == App.ExecType.REAL) App.rmiType = App.RMIType.REMOTE;
	//
	String appName = UiHelper.string(R.string.app_projectid);
	String localIP = "192.168.1.105";//"192.168.1.105";
	String localIP_emu = "10.0.2.2";// EMULATOR
	//localIP = "31.40.210.76";// MODEM
	String protocol = App.rmiType == App.RMIType.REMOTE ? "https://" : "http://";
	StringBuilder url = new StringBuilder(protocol);
	if (App.rmiType == App.RMIType.LOCAL) url.append(localIP + ":12313");//configured on router port forwarding page
	else if (App.rmiType == App.RMIType.LOCAL_EMU) url.append(localIP_emu + ":8888");
	else url.append(App.execType == App.ExecType.REAL ? appName : appName/*-test*/).append(".appspot.com");
	url.append(RmiTargetInterface.GATE_DIR);
	//
	return url.toString();
}

public static boolean hasTelephony() {
	if (hasTelephony != null) return hasTelephony;
	PackageManager pm = context().getPackageManager();
	hasTelephony = pm != null && pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	return hasTelephony;
}

public static boolean canSMS() {
	return (Tool.notEmpty(Settings.operators.get()));
}

public static boolean hasCamera() {
	PackageManager pm = context().getPackageManager();
	return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
}




class InitRmi extends RmiTargetInterface.InitRmi {}

}

