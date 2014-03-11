package cyua.android.core.misc;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;

import static com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import static com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import static cyua.android.core.ActivityCore.ActivityResultListener;
import static cyua.android.core.AppCore.D;


/** Created by far.be on 7/28/13. */
public abstract class GPlayServiceClient<T extends GooglePlayServicesClient> implements OnConnectionFailedListener, ConnectionCallbacks, ActivityResultListener {
private static final String TAG = "GPlayServiceClient";
//

/** INSTANCE */

protected final long TIK_INTERVAL = 4 * 60 * 1000L;
protected T client;
protected enum Op {TIK}
protected Tiker<Op> tiker;



protected abstract T newInstance();
protected abstract int getResolveCode();
protected abstract void execService();
protected abstract void onDisconnecting();

public void connect() {
	lazyInit();
	if (!isAlive()) try {
		tiker.clear();
		client = newInstance();
		client.connect();
	} catch (Exception ex) {Wow.e(ex);}
}
private void lazyInit() {
	if (tiker != null) return;
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			if (operation == Op.TIK) connect();
		}
	};
}
public boolean isConnected() {
	return client != null && client.isConnected();
}
public boolean isAlive() {
	return client != null && (client.isConnected() || client.isConnecting());
}
public void disconnect() {
	if (isAlive()) try {
		onDisconnecting();
		client.disconnect();
	} catch (Exception ex) {Wow.e(ex);}
	client = null;
	if (tiker != null) tiker.clear();
}



/** LISTENERS */

@Override public void onConnected(Bundle bundle) {
//	if (D) Wow.i(TAG, "onConnected", ">>>GPS");
	if (client != null) execService();// if not disconnected
}
@Override public void onDisconnected() {
//	if (D) Wow.i(TAG, "onDisconnected", ">>>GPS");
	// wait for execService to be recalled (???? as docs told :)
	tiker.setTik(Op.TIK, TIK_INTERVAL);
}
@Override public void onConnectionFailed(ConnectionResult connectionResult) {
//	if (D) Wow.i(TAG, "onConnectionFailed", ">>>GPS");
	tiker.setTik(Op.TIK, TIK_INTERVAL);
	try {
		// TODO use AlertManager
		if (AppCore.isUiStarted() && connectionResult != null && connectionResult.hasResolution()) {
			AppCore.uiContext().setActivityResultListener(this, getResolveCode());
			connectionResult.startResolutionForResult(AppCore.uiContext(), getResolveCode());
		}
	} catch (Exception ex) {Wow.e(ex); }
}
@Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (getResolveCode() == requestCode) connect();
}

}
