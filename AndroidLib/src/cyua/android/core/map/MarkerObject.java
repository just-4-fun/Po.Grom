package cyua.android.core.map;

import cyua.android.core.misc.Tool;
import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MarkerObject extends ObjectSh
{
private static final long serialVersionUID = -5079249238971611083L;
private static final String TAG = MarkerObject.class.getSimpleName();

//

/*****   INSTANCE   */

public static final Column REF = new Column(10).indexId(1);
public Long ref = 0L;
public static final Column TYPE = new Column(20).indexId(1);
public String type;
public static final Column TITLE = new Column(30);
public String title;
public static final Column SNIPPET = new Column(40);
public String snippet;
public static final Column LAT = new Column(50);
public Double lat = 0d;
public static final Column LNG = new Column(60);
public Double lng = 0d;
public static final Column STATE = new Column(70);
public Double state = 0d;
public static final Column MAXLEVEL = new Column(80);
public Integer maxlevel = 0;
public static final Column MINLEVEL = new Column(90);
public Integer minlevel = 0;
transient boolean visible = true;
transient boolean draggable = true;
transient Marker marker;
//transient private MarkerOptions opts;


public MarkerObject() {}
public MarkerObject(Long _ref, String _type, Double _lat, Double _lng, String _title, String _snippet, Double _state)
{
	if (_ref != null) ref = _ref; if (_type != null) type = _type;
	if (_lat != null) lat = _lat; if (_lng != null) lng = _lng;
	if (_title != null) title = _title; if (_snippet != null) snippet = _snippet;
	if (_state != null) state = _state;
}


MarkerOptions initOptions(BitmapDescriptor icon)
{
	return  new MarkerOptions()
	.position(new LatLng(lat, lng))
	.title(title).snippet(snippet)
	.visible(visible).draggable(draggable)
	.anchor(.5f, .5f).icon(icon);
}

void clearMarker()
{
//	markerId = null;
	marker = null;
}
void setMarker(Marker m)
{
//	markerId = m.getId();
	marker = m;
}
void update(Double _lat, Double _lng, String _title, String _snippet, Double _state)
{
	if (_lat != null) lat = _lat; if (_lng != null) lng = _lng;
	if (_title != null) title = _title; if (_snippet != null) snippet = _snippet;
	if (_state != null) state = _state;
	//
	if (marker == null) return;
	if (_lat != null || _lng != null) marker.setPosition(new LatLng(lat, lng));
	if (_title != null) marker.setTitle(title);
	if (_snippet != null) marker.setSnippet(snippet);
}
void setVisible(boolean _visible)
{
	visible = _visible;
	if (marker != null) marker.setVisible(visible);
}
void setDraggable(boolean _draggable)
{
	draggable = _draggable;
	if (marker != null) marker.setDraggable(draggable);
}

boolean isSame(Marker m)
{
//	return m.getId().equals(markerId);
//	if (D) Wow.i(TAG, "MAP >>  isSame m="+m+"  vs  mo.m="+marker+"  ?  "+(m.equals(marker)));
	return m.equals(marker);
}
boolean isSame(Long _ref, String _type)
{
	if (_ref == null || ref == null || !ref.equals(_ref)) return false;
	return Tool.safeEquals(type, _type); 
}



@Override public String toString()
{
	return Tool.printObject(this);
}

@Override public String getStorableID() {
	return null;
}
@Override public boolean isCacheable() {
	return false;
}
}