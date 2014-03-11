package cyua.android.core.misc;

import cyua.android.core.AppCore;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import cyua.android.core.log.Wow;

import static android.os.Handler.Callback;
import static java.lang.Thread.UncaughtExceptionHandler;


public abstract class Sequence {
private static final String TAG = Sequence.class.getSimpleName();


/** STATIC API */

private static List<Sequence> actives = new ArrayList<Sequence>();
private static enum Op {START_SEQUENCE, FINISH_SEQUENCE}

private static Tiker<Op> tiker = new Tiker<Op>("SequenceTiker") {
	@Override public void handleTik(Op operation, Object obj, Bundle data) {
		Sequence sequence = (Sequence) obj;
		if (AppCore.isRecycled()) cancelAll();
		else switch (operation) {
			case START_SEQUENCE:
				startSequence(sequence);
				break;
			case FINISH_SEQUENCE:
				finishSequence(sequence);
				break;
		}
	}
};
private static void cancelAll() {
	tiker.clear();
	Exception ex = new Exception("App is exited.");
	for (Sequence seq : actives) { seq.error = ex; seq.callOnFailInForeground(); }
	actives.clear();
}
private static void startSequence(Sequence sequence) {
	if (actives.contains(sequence)) return;
	if (sequence.start()) actives.add(sequence);
}
private static void finishSequence(Sequence sequence) {
	if (!actives.contains(sequence)) return;
	if (sequence.finish()) actives.remove(sequence);
	else sequence.requestFinish(100);
}








/** INSTANCE API */

Looper looper;
Handler execHandler;
Thread execThread;
public String name = "";
public Throwable error;
public boolean cancelRequested;


public Sequence() {
	requestStart();
}

public Sequence(Looper _looper) {
	looper = _looper;
	requestStart();
}

public void requestStart() {
	tiker.addTik(Op.START_SEQUENCE, this, 0);
}
public void requestFinish(int delayMs) {
	tiker.addTik(Op.FINISH_SEQUENCE, Sequence.this, delayMs);
}
public void cancel() { cancelRequested = true; }
public boolean isCancelled() { return cancelRequested; }


protected boolean start() {
	try {
		if (looper != null) {
			execHandler = new Handler(looper, new Callback() {
				@Override public boolean handleMessage(Message msg) { callDoInBackground(); return true; }
			});
			if (!execHandler.sendEmptyMessage(0)) throw new Exception("Can't send message to Handler.");
		}
		else {
			execThread = new Thread(new Runnable() {
				@Override public void run() { callDoInBackground(); }
			});
			execThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override public void uncaughtException(Thread thread, Throwable ex) {
					error = ex;
					requestFinish(0);
				}
			});
			execThread.start();
		}
		return true;
	} catch (Throwable ex) {
		error = ex;
		callOnFailInForeground();
		return false;
	}
}
protected boolean finish() {
	execHandler = null; execThread = null; looper = null;
	// ! Can perform job in background wo ui visible at all
	if (error != null) callOnFailInForeground();
	else if (!isUiRequired() || AppCore.isUiStarted()) callDoInForeground();
	else {
		if (AppCore.isReconfiguring()) return false;
		else {
			error = new Exception("Ui required but not available.");
			callOnFailInForeground();
		}
	}
	return true;
}









protected void callDoInBackground() {
	try {doInBackground();} catch (Throwable ex) { error = ex; }
	requestFinish(0);
}

protected void callDoInForeground() {
	try {onSuccess();} catch (Throwable ex) {Wow.e(ex);}
}

protected void callOnFailInForeground() {
	try {
		Wow.e(error);
		onFail(error);
	} catch (Throwable ex) {Wow.e(ex);}
}



// TO OVERRIDE
/**
 Sets whether to check Ui availability before call onSuccess
 C an be overriden individually to return true or false
 */
protected boolean isUiRequired() {return false;}
// TO OVERRIDE
protected abstract void doInBackground() throws Exception;
// TO OVERRIDE
protected void onSuccess() throws Exception {onFinish(true);}
// TO OVERRIDE
protected void onFail(Throwable error) throws Exception {onFinish(false);}
// TO OVERRIDE either onFinish or pair of onSuccess and onFail
protected void onFinish(boolean isOk) {}

}
