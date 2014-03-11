package cyua.android.core.map;

import cyua.android.core.location.Fix;
import cyua.android.core.location.LocationService;
import cyua.android.core.location.LocationService.LocationServiceListener;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;


/** MY LOCATION PROVIDER */
public class MyLocationProvider implements MapLayer,
LocationServiceListener, LocationSource, GoogleMap.CancelableCallback
{
protected MapCore map;
protected OnLocationChangedListener listener;
protected Location lastLcn;
protected LatLngBounds lastBounds;
protected boolean centering, active;


@Override public Object onDestroy(MapCore _map)
{
	LocationService.removeListener(this);
	map = null;
	return null;
}
@Override public void onCreate(MapCore _map, LayoutInflater inflater, ViewGroup container, Object param)
{
	LocationService.addListener(this, true);
	map = _map;
}

@Override public void onPause(MapCore map)
{
	active = false;
}
@Override public void onResume(MapCore _map)
{
	active = true;
	if (lastLcn == null) lastLcn = MapService.getLastKnownLocation();
	listener.onLocationChanged(lastLcn);
}

/**  */

@Override public void activate(OnLocationChangedListener _listener)
{
	listener = _listener;
}
@Override public void deactivate()
{
	listener = null;
}


@Override public void onNewFix(Fix fix, Location lcn)
{
	if (fix.valid < 0 && fix.isGps) return;
	if (map == null || listener == null) return;
	//
	lcn.setBearing(fix.bearing);
	lcn.setSpeed(fix.speed);
	Location oldLcn = lastLcn;
	lastLcn = lcn;
	//
	if (!active) return;
	//
	listener.onLocationChanged(lcn);
	// CENTER MY-pointer
	Projection pj = map.getProjection();
	LatLngBounds bounds = pj.getVisibleRegion().latLngBounds;
	if (bounds == null) return;
	//
	LatLng newCoord = new LatLng(fix.lat, fix.lng);
	boolean was = oldLcn == null || bounds.contains(new LatLng(oldLcn.getLatitude(), oldLcn.getLongitude()));
	boolean is = bounds.contains(newCoord);
	if (centering || (was && !is)) {
		centering = true;
		map.mapView.animateCamera(CameraUpdateFactory.newLatLng(newCoord), 500,  this);
	}
}
@Override public void onSaveFixes(List<Fix> fixes) {}

@Override public void onSignalStateChanged(boolean hasGpsSignal, LocationService.SignalState gpsState, boolean gpsOn, boolean netOn) {}


/** CAMERA ANIMATION CALLBACKS */

@Override public void onCancel()
{
	centering = false;
}
@Override public void onFinish()
{
	centering = false;
}

}