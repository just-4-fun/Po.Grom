package cyua.android.core.location;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.location.Fix.Action.ACCEPT;
import static cyua.android.core.location.Fix.Action.BEGIN;
import static cyua.android.core.location.Fix.Action.COMMIT;
import static cyua.android.core.location.Fix.Action.FIRST;
import static cyua.android.core.location.Fix.Action.IGNORE;
import static cyua.android.core.location.Fix.Action.RESET;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import cyua.android.core.inet.InetService;
import cyua.android.core.location.LocationService.Mode;
import cyua.android.core.log.Wow;
import cyua.java.shared.Column;

import android.location.Location;


public class LocationProcessor {
private static final String TAG = LocationProcessor.class.getSimpleName();
// STAT
private static int _total, _accepted, _commited, _optimized, _saved;
public static float _distance, _distPlus;

// FIXME remove
private static StringBuilder testinfo;
public static String getStateInfo() {
	String info = "T=" + _total + "; OK=" + _accepted + "; CM=" + _commited + "; OP=" + _optimized + "; SV=" + _saved + "\n" + "Dist=" + (int) _distance + "; baseDiff=" + (int) (_distPlus - _distance) + "\n" + testinfo.toString();
	if (testinfo.length() > 1500) testinfo.delete(0, testinfo.length() - 1500);
	return info;
}



//
// PATH vars
private int STEP_TIME;
private int COMMIT_STEPS;
private int COMMIT_TIMEOUT;//STEP_TIME * COMMIT_STEPS seconds (time of u-turn 180 dgr)
private int COMMIT_TURNS;
private float MIN_DISTANCE;//COMMIT_TIMEOUT * MIN_SPEED;
// BLIND SPOT
private final float MIN_SPEED = 1.4f;// m/s  = 5 km/h
private final int FORCE_COMMIT_DELAY = 30;//sec
//
private final float NORM_SPEED = 10f;//36 km/h
private final float MAX_SPEED = 50; // m/s  = 240 km/h
private final float MAX_ACCEL = 4f; // m/s2  > 100 km - 6 s
private final float MIN_ACCEL = -8f; // m/s2  > 100 km - 50 m
private final int MAX_DEFLECT_SPEED = 30;// degrees 180/8=22.5 +/-
//
private final int BAD_ACCURACY = 55;
public final int NORM_ACCURACY = 35;
//private final int FINE_ACCURACY = 15;
//
private final float OPTI_SPEED_12 = 12 / 3.6f;
private final float OPTI_DIST_200 = 200;
private final float OPTI_DEFL_120 = 120;
private final int OPTI_COMMIT_5 = 5;


/** INSTANCE */
//private FixTable fixTable;
//
private Fix firstFix;// current first fix
private Fix commitFix;// prev baseFix
private Fix beginFix;// when moves > 0  for preoptimization: last stable fix to link commitFix to: firstFix, beginFix or commitFix
private Fix moveFix;// valid move
private Fix lastFix;// any last fix
private int moves, stays;
private boolean lastMoving, hasSignal;
private Mode mode, lastMode;
//
private ProcessorListener listener;
//
private DecimalFormat dFormat = new DecimalFormat("#.#");



public LocationProcessor(ProcessorListener _listener) {
	listener = _listener;
	_total = _accepted = _commited = _optimized = _saved = 0;
	_distance = 0; _distPlus = 0;


	//FIXME remove
	testinfo = new StringBuilder();
}


/** FROM SERVICE */


void flush() {
	if (commitFix != null) save();
}


Fix process(Location lcn, Mode _mode, boolean _hasSignal) {
	mode = _mode; hasSignal = _hasSignal;
	STEP_TIME = mode.stepTimeSec;
	COMMIT_STEPS = mode.commitSteps;
	COMMIT_TIMEOUT = (int) (STEP_TIME * COMMIT_STEPS * 1.5f + .5);
	COMMIT_TURNS = (int) (COMMIT_STEPS * 0.5f + .5);
	MIN_DISTANCE = COMMIT_TIMEOUT * MIN_SPEED;
	//
	Fix fix = new Fix(lcn);
	link(fix); filter(fix); selectAction(fix); doAction(fix);
	//
	if (D) fix.info = fix.act + (fix.info == null ? "" : ":  " + fix.info) + (moves > 0 ? "  :  moves=" + moves : "");
	// STAY Detect
	boolean moving = stays < COMMIT_STEPS;
	if (lastMoving != moving) listener.onMotionChanged(moving);
	lastMoving = moving;
	lastMode = mode;
	lastFix = fix;
	_total++;
	if (D) {
//	if (D) Wow.v(TAG, "[FIXX]: INFO   "+fix.info);
		Wow.v(TAG, ">>>[FIXX]: DATA   " + fix);
		testinfo.append("\nFIX [ ").append(fix.info).append(" ]");
	}
	return fix;
}
private void link(Fix fix) {
	if (firstFix == null) return;
	else if (moves > 0) fix.link(moveFix).deflect(moveFix, mode.distanceMult);
	else fix.link(beginFix).deflect(lastFix, mode.distanceMult);
	fix.baseDuration = (fix.time - beginFix.time) / 1000f;
}
private void filter(Fix fix) {
	if (!fix.isGps && !InetService.isOnline()) fixError(fix, Fix.PROVIDER, " not Online");
	else if (fix.accuracy >= BAD_ACCURACY) fixError(fix, Fix.ACCURACY, "> BAD");
	else if (fix.duration < 0) fixError(fix, Fix.DURATION, "< 0");
	else if (firstFix == null) return;
	else if (fix.baseDuration <= COMMIT_TIMEOUT && fix.speed < MIN_SPEED)
		fixError(fix, Fix.SPEED, "baseDur< " + COMMIT_TIMEOUT + " & speed< " + MIN_SPEED);
	else if (fix.baseDuration > COMMIT_TIMEOUT && (fix.distance < MIN_DISTANCE || fix.distance < fix.accuracy))
		fixError(fix, Fix.SPEED, "baseDur> " + COMMIT_TIMEOUT + " & baseDist< " + MIN_DISTANCE + " or " + fix.accuracy);
	else if (fix.speed > MAX_SPEED) fixError(fix, Fix.SPEED, "> MAX");
	else if (fix.accel > MAX_ACCEL) fixError(fix, Fix.ACCEL, "> MAX");
	else if (fix.accel < MIN_ACCEL) fixError(fix, Fix.ACCEL, "< MIN");
	else if (fix.deflSpeed > MAX_DEFLECT_SPEED) fixError(fix, Fix.DEFLSPEED, "> MAX");
}
private void selectAction(Fix fix) {
	if (fix.valid < 0) {
		fix.act = RESET;
		if (moves > 0) {
			if (COMMIT_TIMEOUT - fix.baseDuration < (COMMIT_STEPS - moves) * STEP_TIME)
				fixError(fix, "OVERTIME " + fix.baseDuration);
			else if (lastFix.valid < 0) fixError(fix, Fix.DURATION, "After failure");
			else fix.act = IGNORE;// SKIP and go on
		}
	}
	else if (firstFix == null) fix.act = FIRST;
	else if (moves >= COMMIT_STEPS - 1 || fix.duration > FORCE_COMMIT_DELAY) fix.act = COMMIT;
	else if (moves == 0) fix.act = BEGIN;
	else fix.act = ACCEPT;
}
private void doAction(Fix fix) {
	switch (fix.act) {
		case IGNORE:
			break;
		case RESET:
			moves = 0; stays++;
			beginFix = commitFix;
			break;
		case BEGIN:
			beginFix = fix;
		case ACCEPT:
			moves++;
			moveFix = fix;
			break;
		case COMMIT:
			_commited++;
			Fix tmpFix = commitFix;
			while (tmpFix != fix) {
				if (tmpFix.valid == 0) {_accepted++; tmpFix.valid = 1;}
				tmpFix = tmpFix.nextFix;
			}
			fix.link(beginFix).deflect(beginFix, mode.distanceMult);// preoptimize
			beginFix.valid = fix.valid = 1;
			moves = 1; stays = 0;
			moveFix = beginFix = commitFix = fix;
			boolean canSave = optimize();
			if (canSave) save();
			break;
		case FIRST:
			fix.valid = 1;
			moves = 1;
			moveFix = beginFix = commitFix = firstFix = fix;
			_commited = _accepted = 1;
			break;
	}
}
private boolean optimize() {
	// try to remove phantom dog walk
	if (commitFix.speed > OPTI_SPEED_12 || commitFix.distance > OPTI_DIST_200 || firstFix.nextFix == null)
		return true;
	float prevDist = firstFix.nextFix.distance;
	Fix fix = firstFix.nextFix.nextFix;
	int count = 1;
	while (fix != null) {
		float dist = distanceBetween(firstFix, fix);
		if (dist <= prevDist && Math.abs(fix.deflect) > OPTI_DEFL_120) {
			_optimized++;
			if (D) {
				Wow.w(TAG, "optimize", ">>>[FIXX]", fix.toString());
				testinfo.append("\nOPTIMIZE : ").append(fix.toString());
			}
			fix.link(firstFix).deflect(firstFix, mode.distanceMult);
			count = 1;
		}
		else count++;
		prevDist = dist;
		fix = fix.nextFix;
	}
	return count >= OPTI_COMMIT_5;
}
private void save() {
	List<Fix> fixes = new ArrayList<Fix>();
	Fix fix = firstFix == commitFix ? firstFix : firstFix.nextFix;
	while (fix != null) {
		fixes.add(fix);
		_distance += fix.distance;
		_distPlus += fix.distPlus;
		fix = fix.nextFix;
	}
	//
	firstFix = commitFix;
	firstFix.prevFix = null;
	// SAVE
	_saved += fixes.size();
	if (!fixes.isEmpty()) listener.onSave(fixes);
}

private void fixError(Fix fix, Column pty, String descr) {
	fix.valid = -1;
	if (D) {
		Object val = null;
		try {
			val = pty.get(fix);
			if (val == null) val = "null";
			else if (val instanceof Number) val = dFormat.format(val);
		} catch (Exception ex) { Wow.w(TAG, "fixError", pty + " value error: " + ex);}
		fix.info = (fix.info == null ? "" : fix.info + ";  ") + "ERR:  " + pty + "=" + val + " :: " + descr;
	}
}
private void fixError(Fix fix, String descr) {
	fix.valid = -1;
	if (D) fix.info = (fix.info == null ? "" : fix.info + ";  ") + "ERR:: " + descr;
}






/** UTILS */

public static float distanceBetween(Fix startFix, Fix endFix) {
	float[] res = new float[1];
	Location.distanceBetween(startFix.lat, startFix.lng, endFix.lat, endFix.lng, res);
	return res[0];
}
//
//private float bearingBetween(Fix startFix, Fix endFix)
//{
//	float[] res = new float[2];
//	Location.distanceBetween(startFix.lat, startFix.lng, endFix.lat, endFix.lng, res);
//	return res[1];
//}
//
//private float absDeflect(float b1, float b2)
//{
//	if (b2 < 0) b2 += 360;
//	if (b1 < 0) b1 += 360;
//	float deflect = b2 - b1;
//	if (deflect < -180) deflect += 360;
//	else if (deflect > 180) deflect -= 360;
//	return Math.abs(deflect);
//}








/** PROCESSOR LISTENER */

static interface ProcessorListener {
	void onMotionChanged(boolean moving);
	void onSave(List<Fix> fixes);
}

}
