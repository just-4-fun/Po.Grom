package cyua.android.core.map;

import android.view.LayoutInflater;
import android.view.ViewGroup;


public class MapOverlay implements MapLayer
{

@Override public void onResume(MapCore map) {}
@Override public void onPause(MapCore map) {}
@Override public void onCreate(MapCore map, LayoutInflater inflater, ViewGroup container, Object param) {}
@Override public Object onDestroy(MapCore map) {return null;}

}
