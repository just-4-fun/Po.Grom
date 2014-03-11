package cyua.android.core.useraction;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Listeners;
import cyua.android.core.misc.Tool;

import static cyua.android.core.AppCore.D;


/** Created by far.be on 7/27/13. */
public class UserActivityService extends AppCore.AppService {
private static final String TAG = "UserActivityService";
// SINGLETON
private static UserActivityService I;
private static Listeners<UserActionListener> listeners = new Listeners<UserActionListener>();
//
public static enum UAct {
	CAR, BIKE, FOOT, STAY, TILT, UNDEF, NULL;
	int cfd;
	UAct setConf(int val) {cfd = val; return this;}
}


//TODO remove for test only
private static String testinfo;

/** STATIC */

public static UserActivityService instantiate() {
	if (I != null) return I;
	I = new UserActivityService();
	return I;
}

public static UserActivityService getInstance() {
	return I;
}

public static void addListener(UserActionListener listener, long interval) {
	if (I != null) {
		listeners.add(listener);
		I.requestUpdates(interval);
	}
}
public static void removeListener(UserActionListener listener) {
	if (I != null) {
		listeners.remove(listener);
		I.removeUpdates();
	}
}

public static UAct getLastActivity() {
	return I == null ? UAct.NULL : I.lastActivity;
}
public static int getLastConfidence() {
	return I == null ? 0 : I.lastConfidence;
}
public static boolean isActualState() {
	return I != null && I.isActual();
}

static void onDetectActivity(ActivityRecognitionResult result) {
	// todo WARNING for cases when app force stopped but PlayService still fires events
	if (!AppCore.isInitStarted() && I != null && I.client != null) {
		if (I.client.isConnected()) I.client.disconnect();// that removes updates
		else if (!I.client.isAlive()) I.client.connect();
	}
	//
	I.analize(result);
}

/** INSTANCE */

//
private UserActivityGPlayClient client = new UserActivityGPlayClient();
private UAct a1, a2;//, a3, a4, a5;
private UAct lastActivity;
private int lastConfidence;
private long lastUpdate, lastBroadcast;
private final long MAX_BROADCAST_INTERVAL = 60 * 1000L;



/** SERVICE API */

@Override public String onInitFinish(AppCore app) throws Throwable {
	client.connect();
	return super.onInitFinish(app);
}
@Override public void onExitStart(AppCore app) throws Throwable {
	listeners.clear();
	client.disconnect();
}
@Override public String getStateInfo() {
	if (I == null) return "Not available";
	return I.isActual() + ";  " + testinfo; //I.activityInfo(I.lastActivity);
}

/**  */

private boolean isActual() {
	return Tool.deviceNow() - lastUpdate <= client.getInterval() * 2;
}

private void requestUpdates(long _interval) {
	if (!listeners.isEmpty()) client.requestUpdates(_interval);
}
private void removeUpdates() {
	if (listeners.isEmpty()) client.removeUpdates();
}


private void analize(ActivityRecognitionResult result) {
	long now = Tool.deviceNow();
	lastUpdate = now;
	List<DetectedActivity> activities = result.getProbableActivities();
	if (result == null || activities == null) return;
	//
	assignActivities(activities);
	//
	if (a1 != UAct.TILT) {
		UAct activity = a1.cfd >= 75 ? a1 : UAct.UNDEF;
		if (activity == UAct.UNDEF) {
			if (a1 == UAct.UNDEF && a1.cfd >= 50) {
				if (a2 == lastActivity) activity = a2;
			}
			else if (isStillOrUnknown(a1) && isStillOrUnknown(a2) && a1.cfd + a2.cfd >= 85) {
				activity = UAct.STAY;
			}
			else if (isMovingOrUnknown(a1) && isMovingOrUnknown(a2) && a1.cfd + a2.cfd >= 85) {
				activity = a1 == UAct.UNDEF ? a2 : a1;
			}
		}
		int confidence = a1 == UAct.UNDEF ? a2.cfd : a1.cfd;
		//
		if (lastActivity != activity || lastConfidence != confidence || now - lastBroadcast > MAX_BROADCAST_INTERVAL) {
			lastActivity = activity;
			lastConfidence = confidence;
			// fire Event
			while (listeners.hasNext()) {
				try { listeners.next().onUserActivityChanged(activity, lastConfidence); } catch (Exception ex) {
					Wow.e(ex);
				}
			}
			lastBroadcast = now;
		}
	}
	//
	if (D) {
		StringBuilder text = new StringBuilder();
		for (DetectedActivity a : activities) text.append(text.length() > 0 ? "; " : "").append(activityInfo(a));
		Wow.i(TAG, "analize", ">>> UA " + text);
		testinfo = text.toString();// todo remove
	}
}
private boolean isStillOrUnknown(UAct a) {
	return a == UAct.UNDEF || a == UAct.STAY;
}
private boolean isMovingOrUnknown(UAct a) {
	return a == UAct.UNDEF || a == UAct.CAR || a == UAct.BIKE || a == UAct.FOOT;
}
private void assignActivities(List<DetectedActivity> activities) {
	int size = activities.size();
	a1 = size > 0 ? assignActivity(activities.get(0)) : UAct.NULL.setConf(0);
	a2 = size > 1 ? assignActivity(activities.get(1)) : UAct.NULL.setConf(0);
//	a3 = size > 2 ? assignActivity(activities.get(2)) : UAct.NULL.setConf(0);
//	a4 = size > 3 ? assignActivity(activities.get(3)) : UAct.NULL.setConf(0);
//	a5 = size > 4 ? assignActivity(activities.get(4)) : UAct.NULL.setConf(0);
}
private UAct assignActivity(DetectedActivity activity) {
	UAct a = null;
	switch (activity.getType()) {
		case DetectedActivity.IN_VEHICLE: a = UAct.CAR; break;
		case DetectedActivity.ON_BICYCLE: a = UAct.BIKE; break;
		case DetectedActivity.ON_FOOT: a = UAct.FOOT; break;
		case DetectedActivity.STILL: a = UAct.STAY; break;
		case DetectedActivity.TILTING: a = UAct.TILT; break;
		case DetectedActivity.UNKNOWN: a = UAct.UNDEF; break;
		default: a = UAct.NULL.setConf(0); return a;
	}
	a.cfd = activity.getConfidence();
	return a;
}

private String activityInfo(DetectedActivity activity) {
	if (activity == null) return "null";
	String type = null;
	switch (activity.getType()) {
		case DetectedActivity.IN_VEHICLE: type = "CAR"; break;
		case DetectedActivity.ON_BICYCLE: type = "BIKE"; break;
		case DetectedActivity.ON_FOOT: type = "FOOT"; break;
		case DetectedActivity.STILL: type = "STAY"; break;
		case DetectedActivity.TILTING: type = "TILT"; break;
		case DetectedActivity.UNKNOWN: type = "UNDEF"; break;
		default: type = "?"; break;
	}
	return type + "-" + activity.getConfidence();
}







/** LISTENER */

public static interface UserActionListener {
	public void onUserActivityChanged(UAct activity, int confidence);
}

}
