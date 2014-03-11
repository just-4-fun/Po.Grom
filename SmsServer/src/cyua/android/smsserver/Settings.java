package cyua.android.smsserver;

import cyua.android.core.CacheVar;


/**
 Created by Marvell on 1/28/14.
 */
public class Settings {

public static CacheVar.IntVar sendCounterVar;
public static CacheVar.IntVar pendCounterVar;
public static CacheVar.StringVar lastIdVar;
public static CacheVar.StringVar phone;


public static void initCache() {
	CacheVar.initVars(Settings.class, null, null);
	// set Defaults
	lastIdVar.defolt("-1");
	if (phone.isEmpty()) phone.set(App.getPhone());
}

public static void incrSendCounter() {
	sendCounterVar.set(sendCounterVar.get() + 1);
}
public static void decrSendCounter() {
	sendCounterVar.set(sendCounterVar.get() - 1);
}
public static int getSendCounter() {
	return sendCounterVar.get();
}

public static void setPendingCounter(int count) {
	pendCounterVar.set(count);
}
public static void incrPendCounter() {
	pendCounterVar.set(pendCounterVar.get() + 1);
}
public static void decrPendCounter() {
	pendCounterVar.set(pendCounterVar.get() - 1);
}
public static int getPendCounter() {
	return pendCounterVar.get();
}

public static void setLastId(String val) {
	lastIdVar.set(val);
}
public static String getLastId() {
	return lastIdVar.get();
}

}
