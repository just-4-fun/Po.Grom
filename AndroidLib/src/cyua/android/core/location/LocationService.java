package cyua.android.core.location;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import cyua.android.core.AppCore;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.location.LocationProcessor.ProcessorListener;
import cyua.android.core.location.LocationSource.SrcConf;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Listeners;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import static cyua.android.core.AppCore.D;


public class LocationService extends AppService implements
		ProcessorListener, LocationSource.iLocationSourceListener {
private static final String TAG = LocationService.class.getSimpleName();
//
static final String GPS = LocationManager.GPS_PROVIDER;
static final String NET = LocationManager.NETWORK_PROVIDER;
static final String PASSIVE = LocationManager.PASSIVE_PROVIDER;
//
static final long TIK_DELAY = 5000;
//
private static LocationService I;


//FIXME remove
public static StringBuilder textinfo = new StringBuilder();




/** STATIC INIT */

public static AppService instantiate() {
	if (I != null) return I;
	I = new LocationService();
	I.initOrder = AppService.INIT_MID;
	I.exitOrder = AppService.EXIT_MID;
	return I;
}


public static LocationService getInstance() {
	return I;
}

public static boolean isAvailable() {
	return I != null && I.isAvailable;
}
public static boolean isGpsAvailable() {
	return isAvailable() && I.hasGps;
}
public static boolean isGpsActive() {
	return isGpsAvailable() && I.isGpsOn();
}
public static boolean hasGpsSignal() {
	return isAvailable() && I.hasGpsSignal;
}


public static void addListener(LocationServiceListener listener, boolean getNow) {
	if (!isAvailable()) return;
	I.listeners.add(listener);
	if (getNow && I.isAvailable)
		listener.onSignalStateChanged(I.hasGpsSignal, I.gpsState, I.isGpsEnabled(), I.isNetEnabled());
}
public static void removeListener(LocationServiceListener listener) {
	if (!isAvailable()) return;
	I.listeners.remove(listener);
}

public static void resumeService() {
	if (isAvailable()) I.doPause(false);
}
public static void pauseService() {
	if (isAvailable()) I.doPause(true);
}

public static void flushData() {
	if (isAvailable()) I.flush();
}


public static Mode getMode() {
	return isAvailable() ? I.mode : null;
}
public static void setMode(Mode mode) {
	if (!isAvailable()) return;
	I.newMode = mode;
}

public static Fix getLastValidFix() {
	return I == null ? null : I.validFix;
}
public static Fix getLastAvailableFix() {
	return I == null ? null : I.availFix;
}
public static Fix getLastKnownFix() {
	LocationManager man = ((LocationManager) AppCore.context().getSystemService(Context.LOCATION_SERVICE));
	Location lnl = man.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	if (lnl == null) lnl = man.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
	if (lnl == null) lnl = man.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	if (lnl == null) lnl = new Location("null");
	Fix fix = new Fix(lnl);
	fix.provider = "";
	return fix;
}


public static float getSpeedKmph() {
	return isAvailable() ? I.doGetSpeed() : -1;
}

public static String getTailKml() {
	return isAvailable() ? I.doGetTailKml() : "";
}

public static String pointOpenTag() {
	return "<Point><coordinates>";
}
public static String pointCloseTag() {
	return "</coordinates></Point>";
}
public static String lineOpenTag() {
	return "<LineString><coordinates>";
}
public static String lineCloseTag() {
	return "</coordinates></LineString>";
}


/** INSTATNCE */

private enum Op {
	TIC;
}
private Tiker<Op> tiker;
private LocationProcessor processor;
private LocationManager manager;
private Mode mode, newMode;
private LocationSource gpsSource, netSource, pasSource;
//
protected boolean hasGps, hasNet;
protected boolean gpsOn, netOn;
protected boolean isAvailable;
//
SignalState gpsState;
boolean isPaused;
boolean hasGpsSignal;
boolean isMoving;
private Fix validFix, availFix;
//
private Listeners<LocationServiceListener> listeners = new Listeners<LocationServiceListener>();
private final long TAIL_SPAN_5 = 5 * 60 * 1000;
private LinkedList<Fix> tail;
private int lastSpeed = -1;




/** PUBLIC API */

private void doPause(boolean yes) {
	isPaused = yes;
}




/** APPSERVICE Methods */

@Override public void onInitStart(AppCore app) throws Throwable {
	manager = (LocationManager) AppCore.context().getSystemService(Context.LOCATION_SERVICE);
	hasGps = manager.getProvider(GPS) != null;
	hasNet = manager.getProvider(NET) != null;
	gpsOn = isGpsOn();
	netOn = isNetOn();
	isAvailable = hasGps || hasNet;
	hasGpsSignal = true;
	//
	if (!isAvailable) {manager = null; return;}
	//
	if (newMode == null) newMode = Mode.ECONOM;
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op op, Object obj, Bundle data) {
			if (op == Op.TIC) tik();
		}
	};
	//
	tail = new LinkedList<Fix>();
	processor = new LocationProcessor(this);


	//FIXME remove
	textinfo = new StringBuilder();
}
@Override public String onInitFinish(AppCore app) throws Throwable {
	if (!isAvailable) return null;
	tik();
	return super.onInitFinish(app);
}
@Override public void onExitStart(AppCore app) throws Throwable {
	if (!isAvailable) return;
	//
	if (gpsSource != null) gpsSource.destroy();
	if (netSource != null) netSource.destroy();
	if (pasSource != null) pasSource.destroy();
}
@Override public void onExitFinish(AppCore app) throws Throwable {
	listeners.clear();
	if (!isAvailable) return;
	//
	if (tiker != null) tiker.clear();
	tiker = null;
	gpsSource = netSource = pasSource = null;
	tail = null;
	processor = null;
	manager = null;
	newMode = mode = null;
	isAvailable = isPaused = hasGpsSignal = isMoving = false;
	gpsState = null;
	validFix = availFix = null;
}


@Override public String getStateInfo() {
	return "Gps (Avail/Enab/On)=(" + hasGps + " / " + isGpsEnabled() + " / " + (gpsSource != null) + "),    Net (Avail/Enab/On)=(" + hasNet + " / " + isNetEnabled() + " / " + (netSource != null) + "); sinceLastFix = " + (availFix == null ? "X" : (int) ((Tool.now() - availFix.uptime) / 1000)) + "; sinceValidFix = " + (validFix == null ? "X" : (int) ((Tool.now() - validFix.uptime) / 1000)) + "; mode = " + mode + ";  gpsState = " + gpsState;
}

/** OTHER */

public boolean isGpsEnabled() {
	return manager != null && manager.isProviderEnabled(GPS);
}
public boolean isNetEnabled() {
	return manager != null && manager.isProviderEnabled(NET);
}
public boolean isGpsOn() {
	return !hasGps || isGpsEnabled();
}
public boolean isNetOn() {
	return !hasNet || isNetEnabled();
}


protected void tik() {
	if (AppCore.isExitStarted()) return;
	tiker.setTik(Op.TIC, TIK_DELAY);
	try {analize(); } catch (Exception ex) { Wow.e(ex); }
}

protected void analize() {
	// mode
	boolean modeChanged = mode != newMode;
	mode = newMode;
	// availability check
	boolean wasGpsOn = gpsOn, wasNetOn = netOn;
	gpsOn = isGpsOn();
	netOn = isNetOn();
	boolean availabChanged = gpsOn != wasGpsOn || netOn != wasNetOn;
	boolean gpsEnabled = isGpsEnabled();
	// gps state
	gpsState = gpsSource == null ? SignalState.NONE : gpsSource.getState();
	// signal
	boolean hadSignal = hasGpsSignal;
	hasGpsSignal = false;
	if (gpsEnabled) hasGpsSignal = (gpsState == SignalState.NONE || gpsState == SignalState.AQUIRING) ? hadSignal : (gpsState == SignalState.ACTIVE || gpsState == SignalState.LINGER);
	// action
	int action = 0, STOP = -1, START = 1;
	if (gpsEnabled && !isPaused) {
		if (gpsSource == null) action = START;
		else if (modeChanged) action = STOP;
	}
	else action = STOP;
	// GPS manage
	if (action == STOP && gpsSource != null) stopGps();
	else if (action == START) startGps();
	// NET manage
	boolean reallyHasSignal = hasGpsSignal && (gpsSource != null || modeChanged);
	boolean tooOldFix = availFix == null || !availFix.isActual(300000);// 5 minutes
	boolean startNet = !reallyHasSignal || tooOldFix;
	if (startNet && netSource == null && isNetEnabled()) startNet();
	else if (!startNet && netSource != null) stopNet();
	//fire signal event
	if (hadSignal != hasGpsSignal || availabChanged) {
		while (listeners.hasNext()) {
			try { listeners.next().onSignalStateChanged(hasGpsSignal, gpsState, gpsOn, netOn); } catch (Exception ex) {
				Wow.e(ex);
			}
		}
	}
	if (D)
		Wow.i(TAG, "analize", "Mode=" + mode + ",  Signal (had/has)=(" + hadSignal + " / " + hasGpsSignal + "),  Gps (Enabled/On)=(" + isGpsEnabled() + " / " + (gpsSource != null) + "),    Net (Enabled/On)=(" + isNetEnabled() + " / " + (netSource != null) + "),  moving=" + isMoving + ",    gpsState=" + gpsState + ",   action=" + action + ",  paused=" + isPaused);
	// returns whether gps should be restarted
}
private void startGps() {
	gpsSource = mode == Mode.ECONOM ?
			new GpsDiscreteSource(this, mode.gps) :
			new LocationSource(this, mode.gps);
}
private void stopGps() {
	if (gpsSource != null) gpsSource.destroy();
	gpsSource = null;
}
private void startNet() {
	netSource = new LocationSource(this, mode.net);
}
private void stopNet() {
	if (netSource != null) netSource.destroy();
	netSource = null;
}


private void flush() {
	processor.flush();
}
private String doGetTailKml() {
	try {
		if (Tool.isEmpty(tail)) return "";// FIXME throws ConcurrentModificationException
		StringBuilder str = new StringBuilder();
		Fix[] fixes = tail.toArray(new Fix[0]);
		for (Fix fix : fixes) {
			if (fix.valid > 0) str.append(str.length() > 0 ? " " : "").append(fix.lng + "," + fix.lat);
		}
		if (str.length() > 0) str.insert(0, lineOpenTag()).append(lineCloseTag());
		return str.toString();
	} catch (Exception ex) {
		if (!(ex instanceof ConcurrentModificationException)) Wow.e(ex);
		return "";
	}
}



@Override public void onLocationChanged(Location lcn) {
//	if (D) Tool.setTimer();
	Fix fix = processor.process(lcn, mode, hasGpsSignal);
	//
	availFix = fix;
	// TAIL
	long overtime = Tool.now() - TAIL_SPAN_5;
	ListIterator<Fix> litr = tail.listIterator();
	while (litr.hasNext()) {
		if (litr.next().uptime < overtime) litr.remove();
		else break;
	}
	if (fix.isGps && fix.valid >= 0) {
		validFix = fix;
		if (tail.size() >= 2) lastSpeed = calcSpeed(fix);
		tail.add(fix);
	}
	// fire event
	while (listeners.hasNext()) {
		try { listeners.next().onNewFix(fix, lcn); } catch (Exception ex) { Wow.e(ex); }
	}
//	if (D) Wow.i(TAG, "onLocationChanged", "cycle = " + Tool.getTimer(true) + " ms");
}
private int calcSpeed(Fix fix) {
	// find fix closest to last within closeTime sec (no less minTime sec, no more maxTime sec) and calc speed in that span
	long minTime = fix.uptime - 8000;
	long closeTime = fix.uptime - 12000;
	long maxTime = fix.uptime - 30000;
	ListIterator<Fix> litr = tail.listIterator(tail.size());
	Fix newerFix = null, olderFix = null;
	while (litr.hasPrevious()) {
		olderFix = litr.previous();
		if (olderFix.uptime < closeTime) break;
		else newerFix = olderFix;
	}
	long newerDelta = (newerFix == null || newerFix.uptime > minTime) ? -1 : Math.abs(newerFix.uptime - closeTime);
	long olderDelta = (olderFix == null || olderFix.uptime < maxTime) ? -1 : Math.abs(olderFix.uptime - closeTime);
	Fix baseFix = (newerDelta < olderDelta && newerDelta != -1) ? newerFix : (olderDelta != -1 ? olderFix : null);
	if (baseFix == null) return -1;
	float baseDrn = (fix.uptime - baseFix.uptime) / 1000f;
	float baseDist = LocationProcessor.distanceBetween(baseFix, fix);
	float speed = baseDist / baseDrn;
	if (D) {
		Wow.i(TAG, "doGetSpeed", "DUR=" + baseDrn, "DIST=" + baseDist, ">>>SPEED = " + (int) (speed * 3.6));
		textinfo.delete(0, textinfo.length());
		textinfo.append("DUR=" + (int) baseDrn + "; DIST=" + (int) baseDist + "; SPEED = " + (int) (speed * 3.6) + "\n");
	}
	return (int) Math.floor(speed * 3.6f);

}

private int doGetSpeed() {
	// todo WARNING should be called from main thread or CuncurrentModification may occure
	return !tail.isEmpty() && validFix.isActual(30000) ? lastSpeed : -1;
}

@Override public void onLocationSourceChanged(LocationSource src, int delay) {
	tiker.setTik(Op.TIC, delay);
}

@Override public void onMotionChanged(boolean moving) {
	isMoving = moving;
}
@Override public void onSave(final List<Fix> fixes) {
//	if (D) Tool.setTimer();
	// fire event
	while (listeners.hasNext()) {
		try { listeners.next().onSaveFixes(fixes); } catch (Exception ex) { Wow.e(ex); }
	}
//	if (D) Wow.i(TAG, "onSave", "cycle = " + Tool.getTimer(true) + " ms");
}








/** MODE */

public static enum Mode {
	INTENSIVE(1, 8, 1.02f), NORMAL(8, 2, 1.04f), ECONOM(30, 1, 1.06f);

	public int stepTimeSec;
	public int commitSteps;
	public float distanceMult;
	SrcConf gps, net, passive;
	private Mode(int _stepTime, int _commitSteps, float _mult) {
		stepTimeSec = _stepTime;
		commitSteps = _commitSteps;
		distanceMult = _mult;
		gps = new SrcConf(GPS, stepTimeSec * 1000);
		net = new SrcConf(NET, 30 * 1000);
		passive = new SrcConf(PASSIVE, 1000);
	}
}
//
public static enum SignalState {
	NONE, AQUIRING, ACTIVE, LINGER, NOSIGNAL, STUCK
}






/** PUBLIC LISTENER */

public static interface LocationServiceListener {
	public void onNewFix(Fix fix, Location lcn);
	public void onSaveFixes(List<Fix> fixes);
	public void onSignalStateChanged(boolean hasGpsSignal, SignalState gpsState, boolean gpsOn, boolean netOn);
}

}
