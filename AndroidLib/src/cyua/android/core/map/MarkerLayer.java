package cyua.android.core.map;

import java.util.ArrayList;
import java.util.List;
import cyua.android.core.db.DbCore.Select;
import static cyua.android.core.AppCore.D;
import cyua.android.core.log.Wow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public final class MarkerLayer implements MapDrawLayer,
GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener,
GoogleMap.OnInfoWindowClickListener, GoogleMap.InfoWindowAdapter
{
private static final String TAG = MarkerLayer.class.getSimpleName();
//


/*****   INSTANCE   */

protected MarkerTable table;
protected MapCore map;
protected List<MarkerObject> markers = new ArrayList<MarkerObject>();;
protected boolean loading;
protected MarkerObject activeMarker;

// default constructor is required
public MarkerLayer(){}


/** MAP LIFECYCLE METHODS */


@Override public void onCreate(MapCore _map, LayoutInflater inflater, ViewGroup container, Object param)
{
	map = _map;
	table = MarkerTable.onMapCreate(this);
}

@Override public Object onDestroy(MapCore map)
{
	MarkerTable.onMapDestroy();
	return null;
}

@Override public void onResume(MapCore map) {}
@Override public void onPause(MapCore map) {}

/** MAP METHODS */

@Override public void onDraw(final double left, final double top, final double right, final double bottom, CameraPosition camera, Projection projection)
{
	if (loading) return;
	// LOAD
	loading = true;
	String where = MarkerObject.LNG+">"+left+" AND "+MarkerObject.LNG+"<"+right+
			" AND "+MarkerObject.LAT+">"+top+" AND "+MarkerObject.LAT+"<"+bottom;
	new Select<MarkerObject>(where, null, 0) {
		@Override protected boolean isUiRequired() { return true; }
		@Override protected void onFail(Throwable error) {
			//TODO
			loading = false;
		}
		@Override public void onSelect(List<MarkerObject> list) {
			if (D) Wow.i(TAG, "onSelect", "size=" + list.size());
			for (MarkerObject mo : markers) if (mo.marker != null) mo.marker.remove();
			markers = list;
			loading = false;
			if (map.active) for (MarkerObject mo : markers) addToMap(mo);
		}
	};
}



/** TYPE METHODS */

public void setTypeVisible(MarkerType type, boolean visible)
{
	if (type.visible == visible) return;
	type.visible = visible;
	for (MarkerObject mo : markers) mo.setVisible(visible);
}


public void setVisible(Long ref, String type, boolean visible)
{
	MarkerObject mo = findByRef(ref, type);
	if (mo == null || mo.marker == null) return;
	mo.setVisible(visible);
}

void removeFromList(MarkerObject mo)
{
	if (mo.marker != null) mo.marker.remove();
	markers.remove(mo);
}
void addToList(MarkerObject mo)
{
	MarkerType type = table.getType(mo.type);
	if (type == null || !type.visible) return;
	addToMap(mo);
	markers.add(mo);
}
private void addToMap(MarkerObject mo)
{
	MarkerType type = table.getType(mo.type);
	if (mo.visible && !type.visible) mo.visible = false;
	mo.draggable = type.draggable;
	BitmapDescriptor icon = type == null ? BitmapDescriptorFactory.defaultMarker() : type.getIcon(mo);
	MarkerOptions opts = mo.initOptions(icon);
	Marker m = map.add(opts);
	mo.setMarker(m);
}

protected MarkerObject find(Marker m)
{
	for (MarkerObject mo : markers) if (mo.isSame(m)) return mo;
	return null;
}
protected MarkerObject findByRef(Long ref, String type)
{
	for (MarkerObject mo : markers) if (mo.isSame(ref, type)) return mo;
	// else
	return table.findByRef(ref, type);
}

//// FIXME for test
//public MarkerObject testCreateMarker(LatLng coord)
//{
//	MarkerObject mo = new MarkerObject();
//	mo.lat = coord.latitude; mo.lng = coord.longitude;
//	insertMarker(1L, "default", coord.latitude, coord.longitude, null, null, null);
//	String title = mo.marker.getId()+" : "+mo.marker.hashCode();
//	updateMarker(1L, "default", null, null, title, null, null);
//	if (D) Wow.i(TAG, "MAP >>  create marker  size="+markers.size()+",  m="+mo.marker);
//	return mo;
//}

@Override public boolean isClicked(LatLng point) {return false;}






/** MARKER INFO WINDOW LISTENERS */

@Override public View getInfoWindow(Marker m)
{
	activeMarker = find(m);
	if (activeMarker == null) return null;
	MarkerType type = table.getType(activeMarker.type);
	return type == null ? null : type.getInfoWindow(activeMarker);
}
@Override public View getInfoContents(Marker m)
{
	if (activeMarker == null || !activeMarker.isSame(m)) return null;
	MarkerType type = table.getType(activeMarker.type);
	return type == null ? null : type.getInfoContents(activeMarker);
}


@Override public void onInfoWindowClick(Marker m)
{
	activeMarker = find(m);
	if (activeMarker == null) return;
	MarkerType type = table.getType(activeMarker.type);
	if (type != null) type.onInfoWindowClick(activeMarker);
}


/** MARKER LISTENERS */

@Override public boolean onMarkerClick(Marker m)
{
	if (D) Wow.i(TAG, "onMarkerClick", "marker=" + m);
	activeMarker = find(m);
	if (activeMarker == null) return true;
	MarkerType type = table.getType(activeMarker.type);
	if (type != null) type.onMarkerClick(activeMarker);
	return true;
}

@Override public void onMarkerDragStart(Marker m)
{
	activeMarker = find(m);
	if (activeMarker == null) return;
	MarkerType type = table.getType(activeMarker.type);
	if (type != null) type.onMarkerDragStart(activeMarker);
}
@Override public void onMarkerDrag(Marker m)
{
	if (activeMarker == null || !activeMarker.isSame(m)) return;
	MarkerType type = table.getType(activeMarker.type);
	if (type != null) type.onMarkerDrag(activeMarker);
}
@Override public void onMarkerDragEnd(Marker m)
{
	if (activeMarker == null || !activeMarker.isSame(m)) return;
	MarkerType type = table.getType(activeMarker.type);
	if (type != null) type.onMarkerDragEnd(activeMarker);
}



//-------------------------------------------------------------------
//protected double shiftLat, shiftLng;// to avoid full ovrlaping of markers created at the same place
//public Double getShiftedLat()
//{//-0.0001,0.00012
//	if (shiftLat == 0 || Math.abs(lastFix.lat - shiftLat) > 0.0001)
//		return shiftLat = lastFix.lat;
//	else return shiftLat -= 0.0001;
//}
//
//public Double getShiftedLng()
//{
//	if (shiftLng == 0 || Math.abs(lastFix.lng - shiftLng) > 0.00012)
//		return shiftLng = lastFix.lng;
//	else return shiftLng += 0.00012;
//}


}
