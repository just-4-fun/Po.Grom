package cyua.android.core.misc;

import cyua.android.core.AppCore;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import static cyua.android.core.AppCore.D;
import cyua.android.core.log.Wow;



/*WARNING Works with Integer operations as well as with Enum operations.
 * but Integer value must be > Integer.MIN_VALUE + ops.length
 */
public abstract class Tiker <OpEnum extends Enum<OpEnum>> extends Handler
{
private static final String TAG = Tiker.class.getSimpleName();
private static final int MIN = Integer.MIN_VALUE;
//
String name;
OpEnum[] ops;


public Tiker(String _name)
{
	super();
	name = _name;
	ops = getEnumValues();
}
public Tiker(String _name, Looper looper)
{
    super(looper);
	name = _name;
	ops = getEnumValues();
}

public boolean addTik(OpEnum operation, long delayMillis)
{
	return addTik(MIN+operation.ordinal(), delayMillis);
}
public boolean addTik(int operation, long delayMillis)
{
	return sendEmptyMessageDelayed(operation, delayMillis);
}
public boolean addTik(OpEnum operation, Object obj, long delayMillis)
{
	return addTik(MIN+operation.ordinal(), obj, delayMillis);
}
public boolean addTik(int operation, Object obj, long delayMillis)
{
	Message msg = obtainMessage(operation, obj);
	return sendMessageDelayed(msg, delayMillis);
}
public boolean addTik(OpEnum operation, Object obj, Bundle data, long delayMillis)
{
	return addTik(MIN+operation.ordinal(), obj, data, delayMillis);
}
public boolean addTik(int operation, Object obj, Bundle data, long delayMillis)
{
	Message msg = obtainMessage(operation, obj);
	if (data != null) msg.setData(data);
	return sendMessageDelayed(msg, delayMillis);
}

public boolean addTikToTop(OpEnum operation, Object obj, Bundle data)
{
	return addTikToTop(MIN+operation.ordinal(), obj, data);
}
public boolean addTikToTop(int operation, Object obj, Bundle data)
{
	Message msg = obtainMessage(operation, obj);
	if (data != null) msg.setData(data);
	return sendMessageAtFrontOfQueue(msg);
}

public boolean setTik(OpEnum operation, long delayMillis)
{
	return setTik(MIN+operation.ordinal(), delayMillis);
}
public boolean setTik(int operation, long delayMillis)
{
	cancelTik(operation);
	return addTik(operation, delayMillis);
}
public boolean setTik(OpEnum operation, Object obj, long delayMillis)
{
	return setTik(MIN+operation.ordinal(), obj, delayMillis);
}
public boolean setTik(int operation, Object obj, long delayMillis)
{
	cancelTik(operation);
	return addTik(operation, obj, delayMillis);
}
public boolean setTik(OpEnum operation, Object obj, Bundle data, long delayMillis)
{
	return setTik(MIN+operation.ordinal(), obj, data, delayMillis);
}
public boolean setTik(int operation, Object obj, Bundle data, long delayMillis)
{
	cancelTik(operation);
	return addTik(operation, obj, data, delayMillis);
}

public void cancelTik(OpEnum operation)
{
	cancelTik(MIN+operation.ordinal());
}
public void cancelTik(int operation)
{
	removeMessages(operation);
}

public boolean addRunnable(Runnable r, OpEnum type, long delayMs)
{
	return postAtTime(r, type, SystemClock.uptimeMillis() + delayMs);
}
public boolean replaceRunnable(Runnable r, OpEnum type, long delayMs)
{
	cancelRunnable(type);
	return addRunnable(r, type, delayMs);
}
public void cancelRunnable(OpEnum type)
{
	removeCallbacksAndMessages(type);
}

public void clear()
{
	try {removeCallbacksAndMessages(null);} catch (Throwable ex) { Wow.w(TAG, "clear", Tool.stackTrace(ex));}
}



/* Here Runnable.run is called or message is handled*/
@Override public void dispatchMessage(Message msg)
{
	String msgName = "["+name+".";
	try {
		if (AppCore.isExitFinished()) throw new Exception("App is dead.");
		if (msg.getCallback() == null)
		{
			if (msg.what < MIN + ops.length) {
				OpEnum op = ops[msg.what-MIN];
				msgName += op+"]";
				if (D) Wow.i(TAG, "dispatchMessage", msgName);
				handleTik(op, msg.obj, msg.peekData());
			}
			else {
				msgName += "0x" + Integer.toHexString(msg.what)+"]";
				if (D) Wow.i(TAG, "dispatchMessage", msgName);
				handleTik(msg.what, msg.obj, msg.peekData());
			}
		}
		else
		{
			if (msg.obj != null) msgName += msg.obj+"]";
			else msgName +=  getMessageName(msg)+"]";
			if (D) Wow.i(TAG, "dispatchMessage", msgName);
			msg.getCallback().run();
		}
	} catch (Throwable ex) {Wow.e(ex, "msg = " + msgName);}
}

//WARNING compatibility donwngrade: for API > 10 remove this method
@SuppressLint("Override")
public String getMessageName(Message message) {
    if (message.getCallback() != null) return message.getCallback().getClass().getName();
    return "0x" + Integer.toHexString(message.what);
}



public void handleTik(OpEnum operation, Object obj, Bundle data) {}

public void handleTik(int operation, Object obj, Bundle data) {}



/*****   MISC  **/ 

public OpEnum[] getEnumValues()
{
	try {
		@SuppressWarnings("unchecked")
		Class<OpEnum> enumCls = (Class<OpEnum>) Tool.getGenericParamClass(getClass(), Tiker.class, Enum.class);
		return enumCls.getEnumConstants();
	} catch (Exception ex) {Wow.e(ex);}
	return null;
}


}
