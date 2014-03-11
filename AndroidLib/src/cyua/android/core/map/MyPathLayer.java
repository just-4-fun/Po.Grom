package cyua.android.core.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import cyua.android.core.db.DbCore.Select;
import cyua.android.core.location.Fix;
import cyua.android.core.location.LocationService;
import cyua.android.core.location.LocationService.LocationServiceListener;
import android.location.Location;
import static cyua.android.core.AppCore.D;

import cyua.android.core.log.Wow;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class MyPathLayer implements MapDrawLayer, LocationServiceListener
{
private static final String TAG = MyPathLayer.class.getSimpleName();
private static MyPathLayer I;

/*****   INSTANCE   */


//
// FIXME memo leak
public static List<Fix> fixes;



protected MapCore map;
protected boolean loading, active;
protected List<Polyline> paths = new ArrayList<Polyline>();
protected Polyline currPath;
protected LinkedList<LatLng> currList = new LinkedList<LatLng>();


/** MAP LIFECYCLE METHODS */


@Override public void onResume(MapCore map)
{
	active = true;
}
@Override public void onPause(MapCore map)
{
	active = false;
}

@Override public void onCreate(MapCore _map, LayoutInflater inflater, ViewGroup container, Object param)
{
	map = _map;
	LocationService.addListener(this, true);
}
@Override public Object onDestroy(MapCore map)
{
	LocationService.removeListener(this);
//	paths.clear();
//	currPath = null;
//	currList.clear();
	return null;
}



/** MAP METHODS */

@Override public void onDraw(final double left, final double top, final double right, final double bottom, CameraPosition camera, Projection projection)
{
	if (loading) return;
	// LOAD
	loading = true;
	//
	String where = Fix.LNG+">"+left+" AND "+Fix.LNG+"<"+right+
			" AND "+Fix.LAT+">"+top+" AND "+Fix.LAT+"<"+bottom;
	
	// FIXME remove
	String order = Fix._ID+" DESC";
	//
	
	new Select<Fix>(where, order, 0)
	{
		@Override protected boolean isUiRequired() { return true; }
		@Override protected void onFail(Throwable error) {
			//TODO
		}
		@Override public void onSelect(List<Fix> list)
		{
			loading = false;
			//
			if (!paths.isEmpty()) for (Polyline pl : paths) pl.remove();
			paths.clear();
			//
//			PolylineOptions opts = null;
//			long prevId = -1;
//			for (Fix fix : tmpFixes) {
//				if (prevId+1 != fix._id) {
//					if (opts != null) paths.add(map.add(opts));
//					if (D) Wow.i(TAG, "MYPATH >>  add path   prevId="+prevId+",  fix_id="+fix._id);
//					opts = new PolylineOptions().color(0x880000FF).width(1).geodesic(false);
//				}
//				opts.add(new LatLng(fix.lat, fix.lng));
//				prevId = fix._id;
//			}
//			if (opts != null) paths.add(map.add(opts));
			
			// FIXME remove
			fixes = list;
			
			//
			PolylineOptions opts1 = null, opts2 = null, opts3 = null;
			long prevId = -1;
			for (Fix fix : list) {
				if (prevId-1 != fix._id) {
					if (opts1 != null) {
						paths.add(map.add(opts1));
						paths.add(map.add(opts2));
						paths.add(map.add(opts3));
					}
//					if (D) Wow.i(TAG, "MYPATH >>  add path   prevId="+prevId+",  fix_id="+fix._id);
					opts1 = new PolylineOptions().color(0x44FF0000).width(5).geodesic(false);
					opts2 = new PolylineOptions().color(0x550000FF).width(3).geodesic(false);
					opts3 = new PolylineOptions().color(0xFF00FF00).width(1).geodesic(false);
				}
				LatLng c = new LatLng(fix.lat, fix.lng);
				opts1.add(c);
				if (fix.info.startsWith("+")) {
					opts2.add(c);
					if (fix.info.charAt(1) != 'A') opts3.add(c);// +ACCEPT
				}
				prevId = fix._id;
			}
			if (opts1 != null) {
				paths.add(map.add(opts1));
				paths.add(map.add(opts2));
				paths.add(map.add(opts3));
			}
			//
			if (currPath != null) currPath.remove();
			PolylineOptions opts = new PolylineOptions().color(0x88FF0000).width(1).geodesic(false);
			currPath = map.add(opts);
			currList.clear();
		}
	};
}
@Override public boolean isClicked(LatLng point) {return false;}


@Override public void onNewFix(Fix fix, Location lcn)
{
	if (fix.valid < 0 && fix.isGps) return;
	currList.add(new LatLng(fix.lat, fix.lng));
	if (currList.size() > 100) currList.removeFirst();
	// WARNING Very consuming peace of resultCode
	if (active && currPath != null && currList.size()%2 == 0) {
		currPath.setPoints(currList);
		if (D) Wow.i(TAG, "onNewFix", "points=" + currList.size());
	}
}
@Override public void onSaveFixes(List<Fix> fixes) { }
@Override public void onSignalStateChanged(boolean hasGpsSignal, LocationService.SignalState gpsState, boolean gpsOn, boolean netOn) {}


}
