package cyua.android.core.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import cyua.java.shared.BitState;
import android.location.Location;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapConfig
{
private static final String TAG = MapConfig.class.getSimpleName();

/** OPTIONS */
public static enum Features {
uiCOMPASS, uiZOOMS, uiLOCATION,
fINDOOR, fLOCATION, fTRAFFIC, fRETAIN
}

public static enum Gestures {
gALL, gNOROTATE, gNOSCROLL, gNOTILT, gNOZOOM
}


public Class<? extends MyLocationProvider> myProvider;
public Class<? extends MapOverlay> overlay;
public List<Class<? extends MapLayer>> layers = new ArrayList<Class<? extends MapLayer>>();
public BitState features = new BitState();
public BitState gestures = new BitState();
public CameraPosition camera;
public int mapType;
public int maxZoom;
public int minZoom;
public HashMap<Class<?>, Object> paramMap = new HashMap<Class<?>, Object>();


public MapConfig myProvider(Class<? extends MyLocationProvider> clas) {myProvider = clas; return this;}
public MapConfig overlay(Class<? extends MapOverlay> clas) {overlay = clas; return this;}
public MapConfig layers(Class<? extends MapLayer> ...clas) { layers.addAll(Arrays.asList(clas)); return this; }
public MapConfig features(Features ...bits) { features.setOnly(bits); return this; }
public MapConfig gestures(Gestures ...bits) { gestures.setOnly(bits); return this; }
public MapConfig mapType(int type) {mapType = type; return this;}
public MapConfig maxZoom(int lvl) {maxZoom = lvl; return this;}
public MapConfig minZoom(int lvl) {minZoom = lvl; return this;}
public MapConfig camera(double lat, double lng, float zoom, float tilt, float bearing) {
	if (lat == 0) {
		Location lcn = MapService.getLastKnownLocation();
		lat = lcn.getLatitude(); lng = lcn.getLongitude();
	}
	LatLng L = new LatLng(lat, lng);
	camera = new CameraPosition(L, zoom, tilt, bearing);
	return this;	
}

}