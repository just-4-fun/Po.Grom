package cyua.android.core.location;

import cyua.android.core.db.DbTable;
import cyua.android.core.misc.Tool;
import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;

import android.location.Location;
import android.location.LocationManager;


public class Fix extends ObjectSh<Fix.FixTable> {
private static final long serialVersionUID = -5870911024588890645L;
// types
//TODO depends on Earth elipsoid location
static final float LAT_MPD = 111238.68f;// meters in 1 degree (y axis)
static final float LNG_MPD = 71695.22f;// meters in 1 degreev (x axis)
public static final int MAX_FIXES_PER_MINUTE = 10;
//
private static float[] results = new float[2];

//
static enum Action {
	RESET, IGNORE, FIRST, BEGIN, ACCEPT, COMMIT
}
public static final int BAD = -1, OK = 0, CMT = 1;


/** INSTANCE ************************ */

transient Fix prevFix;
transient Fix nextFix;
//INITIAL PARAMS (no need of previous fix)
public static final Column VALID = new Column(5);
public int valid;
public static final Column ACCURACY = new Column(10);
public float accuracy;// m
public static final Column TIME = new Column(20);
public long time;// milis > 5 mins later than uptime
public static final Column UPTIME = new Column(21);
public long uptime;// milis Time signal comes to device
public static final Column LAT = new Column(30);
public double lat;
public static final Column LNG = new Column(40);
public double lng;
//RELATIVE PARAMS (need previous fix's initial params)
public static final Column DURATION = new Column(50);
public float duration;// seconds
public static final Column DISTANCE = new Column(60);
public float distance;// meters
public static final Column DISTPLUS = new Column(62);
public float distPlus;
public static final Column SPEED = new Column(70);
public float speed;// m/s
public static final Column ACCEL = new Column(80);
public float accel;// m/s2
public static final Column BEARING = new Column(90);
public float bearing;// degrees
public static final Column DEFLECT = new Column(100);
public float deflect;// degrees
public static final Column DEFLSPEED = new Column(110);
public float deflSpeed;// degrees per sec
// etc
public static final Column INFO = new Column(140);
public String info;// NOTE test
public static final Column PROVIDER = new Column(150);
public String provider;// NOTE test
//
transient public float baseDuration;
transient public boolean isGps;
transient public Action act;
//public int type;//
//public float dy;// meters // NOTE just for test
//public float dx;// meters // NOTE just for test
//public boolean xMove;// moving by x // NOTE just for test
//public boolean yMove;// moving by y // NOTE just for test



public Fix() {}

Fix(Location lcn) {
	// INITIAL PARAMS (no need of previous fix)
	accuracy = lcn.getAccuracy();
	lat = lcn.getLatitude();
	lng = lcn.getLongitude();
	time = lcn.getTime();
	speed = lcn.getSpeed();
	bearing = lcn.getBearing();
	uptime = Tool.now();
	provider = lcn.getProvider();
	isGps = !LocationManager.NETWORK_PROVIDER.equals(provider);// TODO what about  FUSE provider
}

// RELATIVE PARAMS (need previous fix's initial params)
protected Fix link(Fix _prevFix) {
	// PREVIOUS FIX Relations
	prevFix = _prevFix;
	prevFix.nextFix = this;
	//
	duration = (isGps == prevFix.isGps ? time - prevFix.time : uptime - prevFix.uptime) / 1000f;
	if (duration <= 0) return this; // impossible
	//
	Location.distanceBetween(prevFix.lat, prevFix.lng, lat, lng, results);
	distance = distPlus = results[0];
	speed = distance / duration;
	accel = (speed - prevFix.speed) / duration;
	//
	bearing = results[1];
//	dy = (float) ((lat - lastFix.lat)*LAT_MPD);// NOTE just for test
//	dx = (float) ((lng - lastFix.lng)*LNG_MPD);// NOTE just for test
//	xMove = (lastFix.dx >= 0 && dx > 0) || (lastFix.dx <= 0 && dx < 0);// NOTE just for test
//	yMove = (lastFix.dy >= 0 && dy > 0) || (lastFix.dy <= 0 && dy < 0);// NOTE just for test
//	//if (D) Wow.v(TAG, "[LINK]: "+this);
	//
	return this;
}
protected Fix deflect(Fix nearestFix, float distMult) {
	if (prevFix != nearestFix) {
		Location.distanceBetween(nearestFix.lat, nearestFix.lng, lat, lng, results);
		bearing = results[1];
	}
	// bearing  (0 <> 360)
	if (bearing < 0) bearing += 360;
	// deflection (-180 <> 180)
	deflect = bearing - nearestFix.bearing;
	if (deflect < -180) deflect += 360;
	else if (deflect > 180) deflect -= 360;
	//
	float absDefl = Math.abs(deflect);
	//
	if (absDefl > 20) distPlus *= distMult;// TODO  formula that depends of deflect
	//
	deflSpeed = absDefl / duration;
	return this;
}
//protected void base(Fix baseFix)
//{
//	// BASE FIX Relations
//	baseDuration = (time - baseFix.time)/1000f;
//	Location.distanceBetween(baseFix.lat, baseFix.lng, lat, lng, results);
//	distPlus = results[0];
//	// bearing  (0 <> 360)
//	float baseBearing = results[1];
//	if (baseBearing < 0) baseBearing += 360;
//	// deflection (-180 <> 180)
//	baseDeflect = baseBearing - baseFix.bearing;
//	if (baseDeflect < -180) baseDeflect += 360;
//	else if (baseDeflect > 180) baseDeflect -= 360;
//	baseDeflect = Math.abs(baseDeflect);
//}


public String toString() {
	return String.format("accur:%1$.0f,  dist:%2$.1f,  dur:%3$.1f,  speed:%4$.1f,  accel:%5$.1f,  "
			+ "bear:%6$.0f,  defl:%7$.0f,  dflspd:%8$.1f,  %9$s;\nINFO[ %10$s ]"
			, accuracy, distance, duration, speed, accel, bearing, deflect, deflSpeed, provider, info);
}

public boolean isActual(long withinMs) {
	return Tool.now() - uptime <= withinMs;
}

@Override public String getStorableID() { return null; }
@Override public boolean isCacheable() { return false; }



public static class FixTable extends DbTable<Fix> {
	public static String tableName = "fixes";
	@Override public String tableName() {return tableName;}

	public FixTable() throws Exception {super();}

	//FIXME remove
	@Override protected void onOpen() {
//	db.backupDb("fixes"+Tool.nowDateTimeStr().replace(':', '.')+".db");
//	db.delete(tableName(), null, null);
//	
//	db.attachDbFromAssets("fixes.db", true);
//	StringBuilder s = new StringBuilder();
//	for (Column col : sqlColumns) s.append((s.length() > 0 ? ",":"")+col.name());
//	String sql = "INSERT INTO main.fixes ("+s+") SELECT "+s+" FROM fixes.fixes";
//	db.execSql(sql);
	}

}
}