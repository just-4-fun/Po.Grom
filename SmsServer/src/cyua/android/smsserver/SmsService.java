package cyua.android.smsserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import cyua.android.core.AppCore;
import cyua.android.core.inet.InetService;
import cyua.android.core.inet.RmiUtils;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;
import cyua.java.shared.BitState;
import cyua.java.shared.RmiTargetInterface;
import cyua.java.shared.objects.MessageSh;

import static cyua.android.core.AppCore.AppService;
import static cyua.android.core.AppCore.D;
import static cyua.android.core.inet.InetService.InetSequence;
import static cyua.android.core.inet.InetService.NetStateListener;
import static cyua.android.core.keepalive.KeepAliveService.KeepAliveListener;


/**
 Created by Marvell on 2/4/14.
 */
public class SmsService extends AppService implements KeepAliveListener, NetStateListener {
private static final String TAG = "SmsService";

// SINGLETON
private static SmsService I;

/** STATIC API */

public static SmsService instantiate() {
	if (I != null) return I;
	I = new SmsService();
	I.initOrder = AppService.INIT_LAST;
	return I;
}


public static void stop() {
	if (I != null) I.doStop();
}

public static void lazyCheck() {
	if (I != null) I.doLazyCheck();
}



/** INSTANCE */
private BitState state = new BitState();
private enum Stt {STARTED, BUSSY}

private enum Op {LAZY_CHECK,}
private Tiker<Op> tiker;



/** SERVICE API */

@Override public String onInitFinish(AppCore app) throws Throwable {
	if (D) Wow.v(TAG, "onInitFinish", "");
	state.set(Stt.STARTED);
	KeepAliveService.keepAliveChange(this);
	InetService.addListener(this, true);// will call listener's callback
	//
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			switch (operation) {
				case LAZY_CHECK: doCheck(); break;
			}
		}
	};
	return super.onInitFinish(app);
}

@Override public void onExitStart(AppCore app) throws Throwable {
	InetService.removeListener(this);
}
@Override public void onExitFinish(AppCore app) throws Throwable {
	state.clear();
	tiker.clear();
}



private boolean isActive() {
	return state.has(Stt.STARTED, Stt.BUSSY);
}
private void doStop() {
	state.clear(Stt.STARTED);
	mayStop();
}
private void mayStop() {
	if (!isActive()) KeepAliveService.keepAliveChange(this);
}
@Override public boolean isKeepAliveRequired() {
	return isActive();
}


@Override public void onlineStatusChanged(boolean isOnline, boolean byUser) {
	if (isOnline) doCheck();
}


private void doLazyCheck() {// device sms database may still not updated so wait a little
	if (tiker != null) tiker.setTik(Op.LAZY_CHECK, 100);
	Settings.incrPendCounter();
}

private void doCheck() {
	if (D) Wow.v(TAG, "doCheck", "state = " + state.toString(Stt.values()), "online = " + InetService.isOnline());
	if (!state.has(Stt.STARTED) || state.has(Stt.BUSSY) || !InetService.isOnline()) return;
	//
	MessageSh msg = checkInbox();
	if (msg != null) {
		if (isValid(msg)) {
			state.set(Stt.BUSSY);
			new SendTask(msg);
		}
		else {
			onTaskDone(msg._id, false, true);
			if (D) Wow.v(TAG, "doCheck", "WRONG FORMAT > Skiped");
		}
	}
}

private void onTaskDone(long lastId, boolean isOk, boolean skip) {
	if (isOk) Settings.incrSendCounter();
	if (isOk || skip) Settings.setLastId(String.valueOf(lastId));
	state.clear(Stt.BUSSY);
	if (isActive()) doCheck();
	else mayStop();
	if (D) Wow.v(TAG, "onTaskDone", "Last ID = " + Settings.getLastId(), "isOk ? " + isOk, "skip ? " + skip);
}


/*
[0]_id=8; [1]thread_id=6; [2]address=+380638158552; [3]person=null; [4]date=1390763805981; [5]date_sent=1390763805000; [6]protocol=0;
[7]read=0; [8]status=-1; [9]type=1; [10]reply_path_present=0; [11]subject=null;
[12]body=Ok:); [13]service_center=+380630000007; [14]locked=0; [15]error_code=0; [16]seen=0; [17]deletable=0; [18]hidden=0;
[19]group_id=null; [20]group_type=null; [21]delivery_date=null; [22]app_id=0; [23]msg_id=0;
[24]callback_number=null; [25]reserved=0; [26]pri=0; [27]teleservice_id=0; [28]link_url=null;

[0]_id=8; [2]address=+380638158552;
[4]date=1390763805981; [5]date_sent=1390763805000;
[12]body=Ok:);
*/

private final String idCol = "_id", addrCol = "address", dateCol = "date"/*, dateSentCol = "date_sent"*/, bodyCol = "body";
private int idIx, addrIx, dateIx, bodyIx = -1; //dateSendIx  absent on API 2.1

private MessageSh checkInbox() {
	Uri uriSMSURI = Uri.parse("content://sms/inbox");
	String lastId = String.valueOf(Settings.getLastId());
	String[] cols = new String[]{idCol, addrCol, dateCol, bodyCol};
	String where = idCol + " > " + lastId;
	String order = idCol;
	//
	Cursor cursor = App.context().getContentResolver().query(uriSMSURI, cols, where, null, order);
	if (cursor == null) return null;
	//
	try {
		if (bodyIx < 0) {
			idIx = cursor.getColumnIndex(idCol);
			addrIx = cursor.getColumnIndex(addrCol);
			dateIx = cursor.getColumnIndex(dateCol);
			bodyIx = cursor.getColumnIndex(bodyCol);
		}
		//
		int count = cursor.getCount();
		Settings.setPendingCounter(count);
		if (D) Wow.v(TAG, "Reading Smss from inbox. New count = " + count, "where = " + where);
		//
		MessageSh msg = null;
		if (cursor.moveToNext()) {
//		StringBuilder text = new StringBuilder();
//		for (int $ = 0; $ < cursor.getColumnCount(); $++) {
//			text.append(text.length() > 0 ? "; " : "").append(cursor.getColumnName($)).append(" = ").append(cursor.getString($));
//		}
//		if (D) Wow.v(TAG, "SMS >>    ", text.toString());
			String text = cursor.getString(bodyIx);
			msg = MessageSh.fromSms(text);
			msg._id = cursor.getLong(idIx);
			msg.datetime = cursor.getString(dateIx);
			msg.phone = cursor.getString(addrIx);
			msg.smss = Settings.phone.get();
			if (D) Wow.v(TAG, "checkInbox", "Text = " + text, "\nMsg = " + Tool.printObject(msg));
		}
		return msg;
	} finally {
		cursor.close();
	}
}
private boolean isValid(MessageSh msg) {
	return msg.uid != null;
}






/** SEND TASK* */

private class SendTask extends InetSequence {
	private MessageSendRmi rmi;
	long msgId;

	public SendTask(MessageSh msg) {
		msgId = msg._id;
		rmi = new MessageSendRmi();
		rmi.request.message = msg;
	}
	@Override protected void doInBackground() throws Exception {
		RmiUtils.rmiRequest(rmi, App.getRmiUrl(), MessageSendRmi.Response.class);
		// keep some pause if error
		if (!rmi.isSuccess()) {
			long waitUntill = Tool.now() + 5000;
			while (state.has(Stt.STARTED) && Tool.now() < waitUntill) Thread.sleep(1000);
			throw new Exception("Rmi failed with resultCode " + rmi.resultCode);
		}
	}
	@Override protected void onSuccess() throws Exception {
		onTaskDone(msgId, true, false);
	}
	@Override protected void onFail(Throwable error) throws Exception {
		onTaskDone(msgId, false, false);
	}



	/** RMI */

	class MessageSendRmi extends RmiTargetInterface.MessageSendRmi {}
}








/** BOOT and SMS  RECEIVER */

public static class Receiver extends BroadcastReceiver {

	@Override public void onReceive(Context context, Intent intent) {
		App.wakeup();
		SmsService.lazyCheck();
	}
}

}