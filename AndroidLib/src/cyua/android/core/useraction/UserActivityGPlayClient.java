package cyua.android.core.useraction;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.GPlayServiceClient;


/** Created by far.be on 7/28/13. */
class UserActivityGPlayClient extends GPlayServiceClient<ActivityRecognitionClient> {
private static final String TAG = "UserActivityGPlayClient";

/** INSTANCE */

private long interval = 10 * 1000;
private Boolean isRequest;
private final int RESOLVE_CODE = 0xA048;
private final int REQUEST_CODE = 0xA049;
private PendingIntent pendingIntent;


void requestUpdates(long _interval) {
	if (_interval > 0) interval = _interval;
	isRequest = true;
	execService();
}

void removeUpdates() {
	isRequest = false;
	execService();
}

long getInterval() {
	return interval;
}



@Override protected void onDisconnecting() {
	if (isConnected()) client.removeActivityUpdates(getIntent());
	if (pendingIntent != null) pendingIntent.cancel();
	pendingIntent = null;
}

@Override protected void execService() {
	if (!isConnected() || isRequest == null) return;
	else if (isRequest) client.requestActivityUpdates(interval, getIntent());
	else client.removeActivityUpdates(getIntent());
}
private PendingIntent getIntent() {
	if (pendingIntent == null) {
		Intent intent = new Intent(AppCore.context(), RecieverService.class);
		pendingIntent = PendingIntent.getService(AppCore.context(), REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	return pendingIntent;
}

@Override protected ActivityRecognitionClient newInstance() {
	return new ActivityRecognitionClient(AppCore.context(), this, this);
}

@Override protected int getResolveCode() {
	return RESOLVE_CODE;
}






/** RECIEVER SERVICE */

public static class RecieverService extends IntentService {

	public RecieverService() { super(TAG); }

	@Override protected void onHandleIntent(Intent intent) {
		UserActivityService.onDetectActivity(ActivityRecognitionResult.extractResult(intent));
	}
}


}

