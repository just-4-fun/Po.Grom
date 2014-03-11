package cyua.android.core.map;

import cyua.android.core.AppCore;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.log.Wow;
import cyua.android.core.ui.UiHelper;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.maps.MapsInitializer;


public class MapService extends AppService
{
private static final String TAG = MapService.class.getSimpleName();
//SINGLETON
private static MapService I;
private static Class<? extends MapCore> mapClass;

/*****   STATIC INIT   */

public static MapService instantiate(Class<? extends MapCore> _mapClass) throws Exception
{
	if (I != null) return I;
	I = new MapService();
	mapClass = _mapClass;
	I.initOrder = AppService.INIT_LAST;
	I.exitOrder = AppService.EXIT_FIRST;
	return I;
}




public static Fragment addFragment(int containerId, FragmentTransaction ta, MapConfig opts)
{
	I.opts = opts;
	Fragment f = I.getFragment();
	ta.add(containerId, f, TAG);
	return f;
}

public static Fragment replaceFragment(int containerId, FragmentTransaction ta, MapConfig opts)
{
	I.opts = opts;
	Fragment f = I.getFragment();
	ta.replace(containerId, f, TAG);
	return f;
}

public static Location getLastKnownLocation()
{
	LocationManager man = ((LocationManager) AppCore.context().getSystemService(Context.LOCATION_SERVICE));
	Location lnl = man.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	if (lnl == null) lnl = man.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	if (lnl == null) lnl = new Location("null");
	return lnl;
}

static MapConfig getCurrentOptions()
{
	return I.opts;
}







/** INSTANCE */

protected MapConfig opts;




/** SERVICE METHODS */

@Override public void onInitStart(AppCore app) throws Throwable
{
	try {
		MapsInitializer.initialize(app);
	} catch (Exception ex) {
		initError = "Google Maps Service is not available."+ex.getMessage();
	}
}

@Override public void onExitFinish(AppCore app) throws Throwable
{
	opts = null;
}



/** MAP API */

protected Fragment getFragment()
{
	Fragment f = UiHelper.fragment(TAG);
	if (f == null) try {
		f = mapClass.newInstance();
	} catch (Exception ex) {Wow.e(ex);}
	return f;
}





}
