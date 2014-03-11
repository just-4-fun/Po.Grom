package cyua.android.core.map;

import static cyua.android.core.AppCore.D; import cyua.android.core.log.Wow;

import android.view.View;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;


public class MarkerType
{
private static final String TAG = MarkerType.class.getSimpleName();





/** INSTANCE  */

protected MarkerTable markerTab;
protected MarkerLayer markerLayer;
public String name;
public int minLevel, maxLevel;
public BitmapDescriptor icon;
public boolean draggable;
public boolean visible = true;
//public List<BitmapDescriptor> stateIcons;



public MarkerType()
{
	this(null, 0, 0);
}
public MarkerType(String typeName, int minLvl, int maxLvl)
{
	name = typeName == null ? this.getClass().getSimpleName() : typeName;
	maxLevel = maxLvl > 0 && maxLvl < 25 ? maxLvl : 18;
	minLevel = minLvl > 0 && minLvl < 25 ? minLvl : 9;
	//
	markerTab = MarkerTable.registerType(this);
}
protected void onMapCreate(MarkerLayer layer)
{
	markerLayer = layer;
	try {
	icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
	} catch (Exception ex) {
		if (D) Wow.i(TAG, "onMapCreate", ex + "");}
//	stateIcons = new ArrayList<BitmapDescriptor>();
}
protected void onMapDestroy() // NOTE caled from MarkerLayer avoiding MapNotInitedException
{
	markerLayer = null;
	icon = null;
//	stateIcons = new ArrayList<BitmapDescriptor>();
}



/** INSTANCE METHODS */

public void insertMarker(long objectId, Double lat, Double lng, String title, String snippet, Double state)
{
	markerTab.insertMarker(objectId, name, lat, lng, title, snippet, state);
}
public void updateMarker(long objectId, Double lat, Double lng, String title, String snippet, Double state)
{
	markerTab.updateMarker(objectId, name, lat, lng, title, snippet, state);
}
public void deleteMarker(long objectId)
{
	markerTab.deleteMarker(objectId, name);
}
public void show(long objectId)
{
	markerLayer.setVisible(objectId, name, true);
}
public void hide(long objectId)
{
	markerLayer.setVisible(objectId, name, false);
}
public void showAll()
{
	markerLayer.setTypeVisible(this, true);
}
public void hideAll()
{
	markerLayer.setTypeVisible(this, false);
}

public BitmapDescriptor getIcon(MarkerObject mo)
{
	return icon;
}


/** MARKER LISTENERS */

public boolean onMarkerClick(MarkerObject mo)
{
//	layer.updateMarker(mo.ref, mo.type, null, null, mo.title+"+", null, null);
	if (D) Wow.i(TAG, "onMarkerClick", "m=" + mo.marker + ",  lng=" + mo.marker.getPosition().longitude + ",  lat=" + mo.marker.getPosition().latitude);
	return true; // NOTE avoids default behavior
//	return false;// moves to marker and opens window
}

public void onMarkerDragStart(MarkerObject mo)
{
	if (D) Wow.i(TAG, "onMarkerDragStart", "m=" + mo.marker);
	
}
public void onMarkerDrag(MarkerObject mo)
{
//	if (D) Wow.i(TAG, "MAP >>  onMarkerDrag  m="+m);
}
public void onMarkerDragEnd(MarkerObject mo)
{
	if (D) Wow.i(TAG, "onMarkerDragEnd", "m=" + mo.marker);
	LatLng coord = mo.marker.getPosition();
	markerTab.updateMarker(mo.ref, mo.type, coord.latitude, coord.longitude, null, null, null);
}



/** INFO WINDOW LISTENERS */

public View getInfoWindow(MarkerObject mo)
{
	if (D) Wow.i(TAG, "getInfoWindow", "m=" + mo);
	return null;
}
public View getInfoContents(MarkerObject mo)
{
	if (D) Wow.i(TAG, "getInfoContents", "m=" + mo);
	return null;
}


public void onInfoWindowClick(MarkerObject mo)
{
	if (D) Wow.i(TAG, "onInfoWindowClick", "m=" + mo);
}

}
