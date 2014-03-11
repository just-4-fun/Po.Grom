package cyua.android.core.map;

import java.util.ArrayList;
import java.util.List;
import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.map.MapConfig.Features;
import cyua.android.core.map.MapConfig.Gestures;

import android.os.Bundle;
import static cyua.android.core.AppCore.D;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class MapCore extends  SupportMapFragment implements 
GoogleMap.OnCameraChangeListener, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener
{
static final String TAG = MapCore.class.getSimpleName();
//
@SuppressWarnings("unchecked")
protected static MapConfig defaultOpts = new MapConfig().mapType(GoogleMap.MAP_TYPE_NORMAL)
.features(Features.uiCOMPASS, Features.uiZOOMS, Features.uiLOCATION, Features.fLOCATION)
.camera(0, 0, 14, 0, 0)
.myProvider(MyLocationProvider.class)
.layers(MarkerLayer.class, MyPathLayer.class);

//



/** INSTANCE API */

protected MapConfig opts;
protected GoogleMap mapView;
protected MyLocationProvider myProvider;
protected MapOverlay overlay;
protected List<MapLayer> layers = new ArrayList<MapLayer>();
protected boolean active;
double extTop, extLeft, extBottom, extRight;
float lastZoom = -1;
//


public MapCore() {}

public MapLayer findLayer(Class<?> clas)
{
	for (MapLayer lay : layers) if (clas.isInstance(lay)) return lay;
	return null;
}

@Override public void onResume()
{
	active = true;
	for (MapLayer lay : layers) lay.onResume(this);
	if (D) Wow.i(TAG, "onResume");
	super.onResume();
}
@Override public void onPause()
{
	active = false;
	for (MapLayer lay : layers) lay.onPause(this);
	if (D) Wow.i(TAG, "onPause");
	super.onPause();
}
@Override public void onDestroy()
{
	for (MapLayer lay : layers)
		opts.paramMap.put(lay.getClass(), lay.onDestroy(this));
	if (D) Wow.i(TAG, "onDestroy");
	super.onDestroy();
}
@Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state)
{
	View v =super.onCreateView(inflater, parent, state);
	//
	FrameLayout frame = new FrameLayout(getActivity());
	frame.setClickable(true);
	frame.setFocusable(true);
	frame.setFocusableInTouchMode(true);
	frame.addView(v);
	//
	mapView = getMap();
	if (mapView == null) onUnavailable();
	else {
		opts = MapService.getCurrentOptions();
		assignOpts();
		//
		for (MapLayer lay : layers)
			lay.onCreate(this, inflater, frame, opts.paramMap.get(lay.getClass()));
		//
		assignBehavior();
		drawData();
	}
	//
	if (D) Wow.i(TAG, "onCreateView", "map = " + mapView + ",  tag=" + getId() + ",   fragm = " + this);
	return frame;
}

protected void onUnavailable()
{
	int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(AppCore.context());
	if (status != ConnectionResult.SUCCESS) new PlayServiceErrorDialog().show();
}

private void assignOpts()
{
	setRetainInstance(opts.features.has(Features.fRETAIN));
	mapView.setMapType(opts.mapType);
	mapView.setTrafficEnabled(opts.features.has(Features.fTRAFFIC));
	mapView.setIndoorEnabled(opts.features.has(Features.fINDOOR));
	mapView.moveCamera(CameraUpdateFactory.newCameraPosition(opts.camera));
	UiSettings setts = mapView.getUiSettings();
	setts.setMyLocationButtonEnabled(opts.features.has(Features.uiLOCATION));// NOTE not here
	setts.setCompassEnabled(opts.features.has(Features.uiCOMPASS));
	setts.setZoomControlsEnabled(opts.features.has(Features.uiZOOMS));
	setts.setRotateGesturesEnabled(!opts.gestures.has(Gestures.gNOROTATE));
	setts.setScrollGesturesEnabled(!opts.gestures.has(Gestures.gNOSCROLL));
	setts.setTiltGesturesEnabled(!opts.gestures.has(Gestures.gNOTILT));
	setts.setZoomGesturesEnabled(!opts.gestures.has(Gestures.gNOZOOM));
	// LAYERS
	for (Class<? extends MapLayer> layClas : opts.layers)
		try {layers.add(layClas.newInstance());} catch (Exception ex) {	Wow.e(ex);}
	// MY PROVIDER
	if (opts.features.has(MapConfig.Features.fLOCATION) && opts.myProvider != null) {
		try {myProvider = opts.myProvider.newInstance();} catch (Exception ex) {	Wow.e(ex);}
		if (myProvider != null) mapView.setLocationSource(myProvider);
	}
	mapView.setMyLocationEnabled(opts.features.has(MapConfig.Features.fLOCATION));
	if (myProvider != null) layers.add(myProvider);
	// OVERLAY
	if (opts.overlay != null) 
		try {overlay = opts.overlay.newInstance();} catch (Exception ex) {	Wow.e(ex);}
	if (overlay != null) layers.add(overlay);
}

protected void assignBehavior()
{
	MarkerLayer markerLayer = getMarkerLayer();
	if (markerLayer != null) {
		mapView.setOnInfoWindowClickListener(markerLayer);
		mapView.setOnMarkerClickListener(markerLayer);
		mapView.setOnMarkerDragListener(markerLayer);
		mapView.setInfoWindowAdapter(markerLayer);
	}
	//
	mapView.setOnCameraChangeListener(this);
	mapView.setOnMapClickListener(this);
	mapView.setOnMapLongClickListener(this);
}

protected MarkerLayer getMarkerLayer()
{
	for (MapLayer lay : layers) if (lay instanceof MarkerLayer) return (MarkerLayer) lay;
	return null;
}

protected void drawData()
{
	// CHECK BOUNDS
	Projection pj = mapView.getProjection();
	LatLngBounds bounds = pj.getVisibleRegion().latLngBounds;
	LatLng tL = bounds.southwest, bR = bounds.northeast;
	double L = tL.longitude, R = bR.longitude, T = tL.latitude, B = bR.latitude;
//	if (D) Wow.i(TAG, "MAP >>  l="+L+", t="+T+", r="+R+", b="+B);
	boolean inside = extLeft <= L && extRight >= R && extTop <= T && extBottom >= B;
	if (inside && lastZoom == opts.camera.zoom) return;
	//
	double dx = R - L, dy = B - T;
//	double dx2 = dx/2, dy2 = dy/2;
	
	
	// FIXME REMOVE
	double dx2 = 0, dy2 = 0;
	
	
	extLeft = L-dx2; extRight = R+dx2; extTop = T-dy2; extBottom = B+dy2;
	lastZoom = opts.camera.zoom;
//	if (D) Wow.i(TAG, "MAP >>  L="+left+", T="+top+", R="+right+", B="+bottom);
	// DRAW
	for (MapLayer lay : layers) {
		if (lay instanceof MapDrawLayer) ((MapDrawLayer)lay).onDraw(extLeft, extTop, extRight, extBottom, opts.camera, pj);
	}
}





/** CAMERA LISTENERS */

@Override public void onCameraChange(CameraPosition pos)
{
	if (AppCore.isExitStarted()) return; // TODO same check in every danger method
	if (D) Wow.i(TAG, "onCameraChange");
	opts.camera = pos;
	drawData();
}




/** MAP ADD HELPERS */

public Marker add(MarkerOptions opts)
{
	if (mapView == null) return null;
	return mapView.addMarker(opts);
}
public Circle add(CircleOptions opts)
{
	if (mapView == null) return null;
	return mapView.addCircle(opts);
}
public GroundOverlay add(GroundOverlayOptions opts)
{
	if (mapView == null) return null;
	return mapView.addGroundOverlay(opts);
}
public Polygon add(PolygonOptions opts)
{
	if (mapView == null) return null;
	return mapView.addPolygon(opts);
}
public Polyline add(PolylineOptions opts)
{
	if (mapView == null) return null;
	return mapView.addPolyline(opts);
}




/** UTILS */

Projection getProjection()
{
	if (mapView == null) return null;
	return mapView.getProjection();
}



/** MAP LISTENERS */

@Override public void onMapClick(LatLng coord) {}
@Override public void onMapLongClick(LatLng coord) {}



}
