package cyua.android.client;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import cyua.android.core.inet.InetRequest;
import cyua.android.core.inet.InetService;
import cyua.android.core.inet.RmiUtils;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.location.Fix;
import cyua.android.core.location.LocationService;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;
import cyua.java.shared.RmiTargetInterface;
import cyua.java.shared.objects.MessageSh;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.keepalive.KeepAliveService.KeepAliveListener;



public class SendTask extends InetService.InetSequence implements KeepAliveListener {
private static final String TAG = "SendTask";

long WAIT_FIX_TIME = (D ? 10 : 30) * 1000;
long WAIT_SMS_TIME = 30 * 1000;



/** STATIC API */

public static String status() {return Settings.task.status.get();}
private void setStatus(int rid) {
	if (isCancelled()) return;
	Settings.task.status.set(rid == 0 ? "" : UiHelper.string(rid));
}
public static boolean isFailed() {return Settings.task.failed.get();}
private void setFailed(boolean yeap) {
	if (isCancelled()) return;
	Settings.task.failed.set(yeap);
}
public static boolean isActive() {return Settings.task.active.get();}
private void setActive(boolean yeap) {
	if (isCancelled()) return;
	Settings.task.active.set(yeap);
}




/** INSTANCE API * */

SendTask() {
	new UiCore.UiAction(Ui.UiOp.APPLY_USER_INPUT).execute();
	setActive(true);
	setFailed(true);
	setStatus(0);
	Settings.lastSend.set(Tool.now());
	KeepAliveService.keepAliveChange(this);
}

@Override protected void doInBackground() {
	try {
		LocationService.setMode(LocationService.Mode.INTENSIVE);
		LocationService.resumeService();
		// Send photos
		List<String> photoLinks = InetService.isOnline() ? sendPhotos() : null;
		// Get fix
		Fix fix = waitFix();
		if (D) Wow.v(TAG, "doInBackground", "fix = " + fix);
		LocationService.setMode(LocationService.Mode.ECONOM);
		//
		String text = Settings.msg.text.get();
		String uid = Settings.uid.get();
		String user = Settings.user.get();
		String phone = Settings.phone.get();
		String extra = Settings.contacts.get();
		double lat = fix.lat, lng = fix.lng;
		boolean realLcn = fix.isActual(60000);
		boolean sos = Tracker.isActive();
		//
		if (InetService.isOnline()) {
			MessageSh msg = MessageSh.create(0, uid, user, phone, text, lat, lng, realLcn, sos);
			msg.extra = extra;
			assignPhotos(msg, photoLinks);
			assignGeocode(msg, realLcn);
			sendViaInet(msg);
		}
		else if (App.hasTelephony() && App.canSMS()) {
			String sms = MessageSh.toSms(0, uid, user, phone, text, lat, lng, realLcn, sos);
			sendViaSms(sms);
		}
		else setStatus(R.string.taskinfo_global_fail);
	} catch (Throwable ex) { Wow.e(ex); }
}
private void assignPhotos(MessageSh msg, List<String> links) {
	if (Tool.isEmpty(links)) return;
	msg.photo1 = nextPhotoLink(links); msg.photolink1 = nextPhotoLink(links);
	msg.photo2 = nextPhotoLink(links); msg.photolink2 = nextPhotoLink(links);
	msg.photo3 = nextPhotoLink(links); msg.photolink3 = nextPhotoLink(links);
	msg.photo4 = nextPhotoLink(links); msg.photolink4 = nextPhotoLink(links);
}
private String nextPhotoLink(List<String> links) {
	return links.isEmpty() ? null : links.remove(0);
}
Fix waitFix() {
	setStatus(R.string.taskinfo_gps_wait);
	Fix fix = LocationService.getLastAvailableFix();
	long waitUntill = Tool.now() + WAIT_FIX_TIME;
	while (!isCancelled() && !isGoodEnough(fix) && LocationService.hasGpsSignal() && Tool.now() < waitUntill) {
		try { Thread.sleep(1000); } catch (InterruptedException e) { Wow.e(e); }
		fix = LocationService.getLastAvailableFix();
	}
	if (fix == null) fix = LocationService.getLastKnownFix();
	return fix;
}
boolean isGoodEnough(Fix fix) {
	return fix != null && fix.accuracy > 0 && fix.accuracy < 20 && fix.isActual(10000);
}

private void sendViaSms(String sms) {
	int opNum = Settings.operators.get().size();
	String operatorN = Settings.operators.get().get(new Random().nextInt(opNum));// throws if empty
	//
	setStatus(R.string.taskinfo_sms_start);
	//
	String SMS_SENT = "SMS_SENT";
	String SMS_DELIVERED = "SMS_DELIVERED";
	BroadcastReceiver sentRvr = null, delvRvr = null;
	//
	try {
		PendingIntent sentPI = PendingIntent.getBroadcast(App.context(), 0, new Intent(SMS_SENT), 0);
		PendingIntent delvPI = PendingIntent.getBroadcast(App.context(), 0, new Intent(SMS_DELIVERED), 0);
		// Callback when the SMS has been sent
		sentRvr = new SmsSentReceiver();
		App.context().registerReceiver(sentRvr, new IntentFilter(SMS_SENT));
		//Callback when the SMS has been delivered
		delvRvr = new SmsDeliveredReceiver();
		App.context().registerReceiver(delvRvr, new IntentFilter(SMS_DELIVERED));
		//
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager.divideMessage(sms);
		ArrayList<PendingIntent> sendIntents = new ArrayList<PendingIntent>(parts.size());
		ArrayList<PendingIntent> delivIntents = new ArrayList<PendingIntent>(parts.size());
		for (int $ = 0; $ < parts.size(); $++) {
			sendIntents.add(sentPI);
			delivIntents.add(delvPI);
//		if (D) Wow.v(TAG, "sendViaSms", "part[" + $ + "] len =" + parts.get($).length(), " text = " + parts.get($));
		}
		if (D) Wow.v(TAG, "sendViaSms", "sms = " + sms, "parts = " + parts.size());
		smsManager.sendMultipartTextMessage(operatorN, null, parts, sendIntents, delivIntents);
		// WAIT receiver's DELIVERED response
		long waitUntil = Tool.now() + WAIT_SMS_TIME;
		while (isActive()) {
			try { Thread.sleep(200); } catch (InterruptedException e) { Wow.e(e); }
			if (Tool.now() > waitUntil) {
				setStatus(R.string.taskinfo_sms_timeout);
				setActive(false);
				cancel();
			}
		}
	} finally {
		// Unregister Receivers
		if (sentRvr != null) App.context().unregisterReceiver(sentRvr);
		if (delvRvr != null) App.context().unregisterReceiver(delvRvr);
	}
}

private void sendViaInet(MessageSh msg) throws IllegalAccessException {
	setStatus(R.string.taskinfo_inet_start);
	// PACK RMI
	MessageSendRmi rmi = new MessageSendRmi();
	rmi.request.message = msg;
	// exec
	RmiUtils.rmiRequest(rmi, App.getRmiUrl(), MessageSendRmi.Response.class);
	try { rmi.done(); } catch (Throwable ex) {Wow.e(ex);}
}

private List<String> sendPhotos() throws InterruptedException {
	UiState state = (UiState) UiService.getUiState();
	if ((!state.processing && !state.hasPhotos()) || Tool.isEmpty(Settings.S3_KEY_ID)) return null;
	// else
	setStatus(R.string.taskinfo_photos_wait);
	// wait if still processing photo
	long waitTo = Tool.now() + 10000;
	while (state.processing && Tool.now() < waitTo) Thread.sleep(1000);
	// calc sum size
	long payloadSize = 0;
	for (File file : state.photoFiles) payloadSize += (file == null ? 0 : file.length());
	if (D) Wow.v(TAG, "sendPhotos", "num = " + state.photos.length, "bytes = " + payloadSize);
	// SEND
	LinkedList<String> links = new LinkedList<String>();
	BasicAWSCredentials creds = new BasicAWSCredentials(Settings.S3_KEY_ID, Settings.S3_KEY);
	AmazonS3Client s3Client = new AmazonS3Client(creds);
	s3Client.setRegion(Region.getRegion(Regions.fromName(Settings.S3_REGION)));
	String awsUrl = "http://" + Settings.S3_HOST + "/" + Settings.S3_BUCKET + "/";
	for (int $ = 0; $ < state.photos.length; $++) {
		Uri uri = state.photos[$];
		if (uri == null) continue;
		String name = Settings.S3_DIR + "/" + uri.getLastPathSegment();
		sendPhoto(uri, name, state.photoFiles[$], s3Client);
		links.add(awsUrl + name);
		if (D) Wow.v(TAG, "sendPhotos", "sent link = " + links.getLast());
		//TODO update progress
	}
	return links;
}
private void sendPhoto(Uri uri, String objName, File file, AmazonS3Client s3Client) {
	String link = null;
	try {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("image/jpeg");
		metadata.setContentLength(file.length());
		InputStream is = App.context().getContentResolver().openInputStream(uri);
		PutObjectRequest req = new PutObjectRequest(Settings.S3_BUCKET, objName, is, metadata);
		PutObjectResult res = s3Client.putObject(req);
	} catch (Exception ex) {Wow.e(ex);}
}
/*
private String sendPhoto(Uri uri) {
	String link = null;
	try {
		InputStream is = App.context().getContentResolver().openInputStream(uri);
		byte[] payload = getBytes(is);
				InetRequest.Options opts = new InetRequest.Options(link)
				.maxAttempts(4)
				.maxDuration(10000)
				.method(InetRequest.Method.POST)
				.contentType(InetRequest.ContentType.DEFAULT)
//				.addHeader(Rmi.HEAD_KEY, sign)
				.payload(payload);
		InetRequest<String> request = new InetRequest<String>(String.class, opts);
		if (D) Wow.v(TAG, "sendPhoto", "exec started ... uri = " + uri.getPath() + "; link = " + link);
		String result = request.execute();
		if (D) Wow.v(TAG, "sendPhoto", "exec finished ... result = " + result);
	} catch (Exception ex) {Wow.e(ex);}
	return link;
}
*/
public byte[] getBytes(InputStream inputStream) throws IOException {
	ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
	int bufferSize = 1024, len = 0;
	byte[] buffer = new byte[bufferSize];
	while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
	return byteBuffer.toByteArray();
}


private void assignGeocode(MessageSh msg, boolean actualLcn) {
	try {
		GeocodeResponse.geocode(msg);
		if (!actualLcn) msg.address = "востаннє зафіксовано";
	} catch (Exception ex) {Wow.e(ex);}
}


@Override protected void onFinish(boolean ok) {
	setActive(false);
	KeepAliveService.keepAliveChange(this);
	Settings.lastSend.set(Tool.now() - (isFailed() ? Settings.msgWaitTime : 0));// cheat to not block retry

}
@Override public boolean isKeepAliveRequired() {
	return isActive();
}








class MessageSendRmi extends RmiTargetInterface.MessageSendRmi {

	@Override public void onSuccess() {
		setStatus(R.string.taskinfo_ok);
		setFailed(false);
	}
	@Override public void onError() {
		setStatus(R.string.taskinfo_inet_fail);
	}
}





class SmsSentReceiver extends BroadcastReceiver {
	@Override public void onReceive(Context arg0, Intent arg1) {
		switch (getResultCode()) {
			case Activity.RESULT_OK:
				setStatus(R.string.taskinfo_sms_sent);
				if (Tool.isEmulator()) setFailed(false);
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				setStatus(R.string.taskinfo_sms_gereric);
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				setStatus(R.string.taskinfo_sms_noservice);
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				setStatus(R.string.taskinfo_sms_nopdu);
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				setStatus(R.string.taskinfo_sms_radiooff);
				break;
		}
		if (D) Wow.v(TAG, "onReceive SMS_SENT", "status = " + status());
		if (Tool.isEmulator()) setActive(false);
	}
}


class SmsDeliveredReceiver extends BroadcastReceiver {
	@Override public void onReceive(Context arg0, Intent arg1) {
		switch (getResultCode()) {
			case Activity.RESULT_OK:
				setStatus(R.string.taskinfo_ok);
				setFailed(false);
				break;
			case Activity.RESULT_CANCELED:
				setStatus(R.string.taskinfo_sms_canceled);
				break;
		}
		if (D) Wow.v(TAG, "onReceive SMS_DELIVERED", "status = " + status());
		setActive(false);
	}
}


}
