package cyua.android.core.map;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;


public interface MapDrawLayer extends MapLayer
{

public void onDraw(double left, double top, double right, double bottom, CameraPosition camera, Projection projection);
public boolean isClicked(LatLng point);

}
