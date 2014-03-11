package cyua.android.core.location;

import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;
import android.location.Location;
import android.os.Bundle;
import static cyua.android.core.AppCore.D;


public class GpsDiscreteSource extends LocationSource
{
private static final String TAG = GpsDiscreteSource.class.getSimpleName();

private enum Op {REQUEST_UPDATES;}
private Tiker<Op> tiker;
private boolean requesting;
private boolean expecting;
private long expectTime;


public GpsDiscreteSource(iLocationSourceListener _receiver, SrcConf _cfg)
{
	super(_receiver, _cfg);
}

@Override protected void init()
{
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op op, Object obj, Bundle data) {
			if (op == Op.REQUEST_UPDATES) requestUpdates();
		}
	};
}

@Override public void destroy()
{
	if (tiker != null) tiker.clear();
	super.destroy();
}

@Override protected void removeUpdates()
{
	if (requesting) man.removeUpdates(this);
	requesting = false;
}

@Override protected void requestUpdates()
{
	tiker.setTik(Op.REQUEST_UPDATES, LocationService.TIK_DELAY);
	long now = Tool.deviceNow();
	if (D) Wow.i(TAG, "requestUpdates", "SOURCE TIK  expecting=" + expecting + ",  requesting=" + requesting + ",  " + (expectTime > now ? "BEFORE " : "AFTER ") + "  expTime=" + Math.abs(((expectTime - now) / 1000)));
	if (expecting || listener == null);// do nothing
	else if (now < expectTime) removeUpdates();
	else {
		if (!requesting) {
			man.requestLocationUpdates(cfg.provider, 1000, 0, this);
//			startTime = signalTime = now;
		}
		expectTime = now + cfg.stepTime;
		requesting = true;
		expecting = true;
	}
}

@Override public void onLocationChanged(Location location)
{
	super.onLocationChanged(location);
	if (expecting) tiker.setTik(Op.REQUEST_UPDATES, 2000);
	expecting = false;
}

}
