package cyua.android.core.map;

import android.view.LayoutInflater;
import android.view.ViewGroup;


public interface MapLayer
{

public void onCreate(MapCore map, LayoutInflater inflater, ViewGroup container, Object param);
public Object onDestroy(MapCore map);
public void onResume(MapCore map);
public void onPause(MapCore map);
}
