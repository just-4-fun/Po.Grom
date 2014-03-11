package cyua.android.client;



import android.os.Bundle;

import cyua.android.core.inet.InetService;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.location.Fix;
import cyua.android.core.location.LocationService;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.location.LocationProcessor.distanceBetween;
import static cyua.android.core.location.LocationService.Mode;
import static cyua.android.core.location.LocationService.isGpsAvailable;


public class Tracker implements KeepAliveService.KeepAliveListener {
private static final String TAG = "Tracker";
private static final long CNCELABLE_TIME = 11 * 1000;
private static final long TIK_SPAN = 60 * 1000;
private static Tracker i;


static boolean isActive() {
	return i != null && i.state != null;
}
static boolean isWorking() {
	return i != null && i.state == Stt.WORKING;
}
static String getStatus() {
	return i == null ? "" : i.calcStatus();
}

static void start() {
	if (i == null) i = new Tracker();
	i.doStart();
}
static void stop() {
	if (i != null) i.doStop();
	i = null;
}




private enum Stt {STARTING, WORKING}
;
private Stt state;
private long startedTime, lastSentTime;
int minutesLeft;
private Fix lastFix;

private enum Op {TIK, START}

private Tiker<Op> tiker = new Tiker<Op>(TAG) {
	@Override public void handleTik(Op operation, Object obj, Bundle data) {
		switch (operation) {
			case START:
				doStart();
				break;
			case TIK:
				tik();
				break;
		}
	}
};


private void doStart() {
	if (state == null) {
		state = Stt.STARTING;
		KeepAliveService.keepAliveChange(this);
		LocationService.resumeService();
		LocationService.setMode(Mode.INTENSIVE);
		startedTime = Tool.now();
		tiker.setTik(Op.START, CNCELABLE_TIME);
	}
	else if (state == Stt.STARTING) {
		state = Stt.WORKING;
		Settings.lastSend.remove();
		Settings.task.remove();
		requestTik(0);
	}
}

private void doStop() {
	tiker.clear();
	state = null;
	LocationService.setMode(Mode.ECONOM);
	LocationService.pauseService();
	KeepAliveService.keepAliveChange(this);
}

private void requestTik(long delay) {
	tiker.setTik(Op.TIK, delay);
}

private void tik() {
	if (SendTask.isActive()) requestTik(2000);
	else if (isSendRequired()) {
		new SendTask() {
			@Override protected void onFinish(boolean ok) {
				super.onFinish(ok);
				lastSentTime = ok ? Tool.now() : 0;// 0 - retry
				LocationService.setMode(Mode.ECONOM);
				if (ok) {
					((UiState) UiService.getUiState()).clearPhotos();
					new UiCore.UiAction(Ui.UiOp.UPDATE_THUMBS).execute();
				}
				requestTik(TIK_SPAN);
			}
		};
	}
	else requestTik(TIK_SPAN);
}

private boolean isSendRequired() {
	boolean online = InetService.isOnline();
	boolean canSms = App.hasTelephony() && App.canSMS();
	if (!online && !canSms) return false;
	//
	Fix fix = LocationService.getLastAvailableFix();
	boolean noActualFix = fix == null || !fix.isActual(60000);
	float currDistKm = lastFix == null ? 99 : (noActualFix ? 0 : distanceBetween(lastFix, fix) / 1000f);
	//
	float hoursFromStart = (Tool.now() - startedTime) / 3600000f;
	float currTimeMins = (Tool.now() - lastSentTime) / 60000f;
	//
	int minTimeMins = online ? 5 : 10;
	int maxTimeMins = hoursFromStart < 1 ? (online ? 10 : 15) : (online ? 30 : 45);// minutes
	int maxDistKm = hoursFromStart < 1 ? (online ? 5 : 7) : (online ? 10 : 15);// km
	//
	boolean reqByTime = currTimeMins >= maxTimeMins;
	boolean reqByDist = currDistKm >= maxDistKm;
	boolean req = reqByTime || reqByDist;
	minutesLeft = (int) ((req ? minTimeMins : maxTimeMins) - currTimeMins + 0.5f) ;
	req = req && currTimeMins >= minTimeMins;
	boolean firstTime = lastSentTime == 0;
	boolean needWarmup = false;
	if (req || firstTime) {
		boolean goodFix = fix != null && fix.isGps && fix.isActual(20000) && fix.accuracy < 15;
		needWarmup = isGpsAvailable() && !goodFix && LocationService.getMode() != Mode.INTENSIVE;
		LocationService.resumeService();
		LocationService.setMode(Mode.INTENSIVE);
		if (needWarmup && !firstTime) req = false;
		else lastFix = fix;
	}
	if (D) Wow.v(TAG, "isSendRequired", "hoursFromStart = "+hoursFromStart, "currDistKm = "+currDistKm, "currTimeMins = "+currTimeMins, "minTimeMins = "+minTimeMins, "firstTime ? "+firstTime, "warmup ? "+needWarmup, "byTime ? "+reqByTime, "byDist ? "+reqByDist, "Req ? "+req);
	return req;
}



private String calcStatus() {
	String s = "";
	if (state == Stt.WORKING) {
		String nextTime = minutesLeft >= 0 ? minutesLeft+"" : status(R.string.track_status_nextundef, null);
		s = status(R.string.track_status_working, nextTime);
	}
	else if (state == Stt.STARTING) {
		long secsLeft = ((startedTime + CNCELABLE_TIME) - Tool.now()) / 1000;
		if (secsLeft >= 0) s = status(R.string.track_status_starting, secsLeft);
	}
	return s;
}

private String status(int rid, Object param) {
	return rid == 0 ? "" : param == null ? UiHelper.string(rid) : UiHelper.string(rid, param);
}

@Override public boolean isKeepAliveRequired() {
	return state != null;
}


}
