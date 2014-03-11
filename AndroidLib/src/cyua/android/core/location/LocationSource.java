package cyua.android.core.location;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.location.LocationService.SignalState;


public class LocationSource implements LocationListener {
private static final String TAG = LocationSource.class.getSimpleName();



iLocationSourceListener listener;
SrcConf cfg;
LocationManager man;
long signalTime, startTime;



public LocationSource() {}

public LocationSource(iLocationSourceListener _receiver, SrcConf _cfg) {
	listener = _receiver;
	cfg = _cfg;
	man = ((LocationManager) AppCore.context().getSystemService(Context.LOCATION_SERVICE));
	signalTime = startTime = Tool.deviceNow();
	init();
	requestUpdates();
	if (D) Wow.i(TAG, "LocationSource", "SOURCE Started  provider=" + cfg.provider + ",  stepTimeSec=" + cfg.stepTime);
}

protected void init() {
	// to Override in subclasses
}
protected void requestUpdates() {
	man.requestLocationUpdates(cfg.provider, cfg.stepTime, 0, this);
}

protected void removeUpdates() {
	man.removeUpdates(this);
}

public void destroy() {
	removeUpdates();
	if (D) Wow.i(TAG, "destroy", "SOURCE Destroyed  provider=" + cfg.provider + "    first ? " + (listener != null));
	listener = null;
}

public SignalState getState() {
	long now = Tool.deviceNow();
	long iddleTime = now - signalTime;
	long lifeTime = now - startTime;
	boolean aquired = signalTime > startTime;
//	long resetDelay = iddleTime == lifeTime ? cfg.signalResetDelay : 2*cfg.signalResetDelay;
	SignalState state;
	if (!aquired && lifeTime < cfg.stepTime + 30000) state = SignalState.AQUIRING;
	else if (iddleTime < cfg.stepTime + 5000) state = SignalState.ACTIVE;
	else if (iddleTime < cfg.stepTime + 30000) state = SignalState.LINGER;
	else if (iddleTime < cfg.stepTime + 90000) state = SignalState.NOSIGNAL;
	else state = SignalState.STUCK;
	return state;
}



@Override public void onLocationChanged(Location location) {
	if (location == null || listener == null) ;
	else {
		signalTime = Tool.deviceNow();
		listener.onLocationChanged(location);
	}
}

@Override public void onStatusChanged(String provider, int status, Bundle extras) {
}
@Override public void onProviderEnabled(String provider) {
}
@Override public void onProviderDisabled(String provider) {
	if (listener != null) listener.onLocationSourceChanged(this, 0);
}







/** CALLBACK */

public interface iLocationSourceListener {
	public void onLocationChanged(Location location);
	public void onLocationSourceChanged(LocationSource src, int delay);
}






/** SOURCE CONF */
public static class SrcConf {
	String provider;
	long stepTime;
	public SrcConf(String _provider, long _stepTime) {
		provider = _provider;
		stepTime = _stepTime;
	}
}

}
