package cyua.android.client;

import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.inet.RmiUtils;
import cyua.android.core.log.LogService;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import cyua.java.shared.RmiTargetInterface;
import cyua.java.shared.objects.LogRecordSh;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.inet.InetService.InetSequence;


public class LogPersistAgent implements LogService.IPersistAgent {
private static final String TAG = "LogPersistAgent";

boolean cacheSent, isBussy;


@Override public boolean isBussy() {
	return isBussy;
}


@Override public void persist(LogService logService) {
	if (isBussy) return;
	final boolean isExiting = AppCore.isExitStarted();
	if (!isExiting && cacheSent) return;
	//
	final List<LogRecordSh> list = isExiting ? Wow.getCurrent() : Wow.readCache();
//	final LogsTable logsTab = Db.logsTable();
	if (D) Wow.i(TAG, "persist", "list = [" + (list == null ? "null" : list.size()));
	if (Tool.isEmpty(list)) return;
	//
	isBussy = true;
	final LogsSendRmi rmi = new LogsSendRmi();
	rmi.request.list = list;
	//
	new InetSequence() {
		@Override protected void doInBackground() throws Exception {
			RmiUtils.rmiRequest(rmi, App.getRmiUrl(), LogsSendRmi.Response.class);
			isBussy = false;
		}
		@Override protected void onSuccess() throws Exception {
			isBussy = false;
			cacheSent = true;
			if (isExiting) Wow.clearCurrent();
		}
		@Override protected void onFail(Throwable error) {
			isBussy = false;
		}
	};
}


class LogsSendRmi extends RmiTargetInterface.LogsSendRmi {}

}
