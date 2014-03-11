package cyua.android.core.power;

import java.util.LinkedList;

import cyua.android.core.AppCore;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.R;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Listeners;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import static cyua.android.core.AppCore.D;


public class PowerService extends AppService {
private static final String TAG = PowerService.class.getSimpleName();
public static final int LOW_LEVEL = 20;
// SINGLETON
static PowerService I;
//
public static enum PwrSrc {
	Accum, AC, USB, Wireless, Undefined
}


/** **   STATIC */

public static AppService instantiate() {
	if (I != null) return I;
	I = new PowerService();
	return I;
}


public static boolean isPluged() {
	return I != null && I.pluged;
}
public static int getLevel() {
	return I != null ? I.level : -1;
}
public static PwrSrc getSource() {
	return I != null ? I.source : PwrSrc.Undefined;
}

public static String getInfo() {
	return UiHelper.string(R.string.power_info, I.level, I.source);
}
public static String getInfoExt() {
	return I == null ? "" : I.getStateInfo();
}




/** INSTANCE */

BroadcastReceiver receiver;
public boolean pluged;
public PwrSrc source = PwrSrc.Undefined;
public int level = -1;
int startLevel, lastLevel;
long lastTime;
int percentsPerHour;
int minutesLeft;
private LinkedList<Integer> stat = new LinkedList<Integer>();
private Listeners<PowerListener> listeners = new Listeners<PowerListener>();


/** **   SERVICE LIFE */

@Override public void onInitStart(AppCore app) throws Throwable {
	receiver = new BroadcastReceiver() {
		@Override public void onReceive(Context context, Intent intent) {
//			String action = intent.getAction();
			float secsPerPercent = 0;
			int maxLevel = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
			int levelVal = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			level = levelVal / (maxLevel / 100);
			int sourceVal = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			pluged = sourceVal > 0;
			source = sourceVal >= 0 && sourceVal < PwrSrc.values().length ? PwrSrc.values()[sourceVal] : PwrSrc.Undefined;
			if (lastTime == 0) lastTime = Tool.deviceNow();
			if (startLevel == 0) startLevel = level;
			//
			if (lastLevel != level) {
				secsPerPercent = ((Tool.deviceNow() - lastTime) / 1000) / (lastLevel - level);
				lastTime = Tool.deviceNow();
				boolean discharging = secsPerPercent > 0;
				//
				if (!discharging) {
					stat.clear();
					percentsPerHour = -1;
					minutesLeft = -1;
				}
				else if (secsPerPercent > 40) {// skip first result as uncomplete
					stat.add((int) secsPerPercent);
					if (stat.size() > 1) {
						if (stat.size() > 5) stat.removeFirst();
						int sum = 0;
						for (int n : stat) sum += n;
						float avgSecsPerPercent = sum / stat.size();
						minutesLeft = (int) (level * avgSecsPerPercent / 60f);
						percentsPerHour = (int) (3600 / avgSecsPerPercent);
					}
				}
				lastLevel = level;
			}
			//
			// Fire event
			while (listeners.hasNext()) {
				try { listeners.next().onPowerChanged(pluged, level); } catch (Exception ex) {Wow.e(ex);}
			}
			if (D)
				Wow.i(TAG, "onReceive", "pluged=" + pluged + "  %=" + level + ",  source=" + source + ",   loose=" + percentsPerHour + " %ph,   lost="+(lastLevel-startLevel)+" %;  leftMins=" + minutesLeft + ", secs% = " + secsPerPercent + ", statSize = " + stat.size());
		}
	};
	//
	AppCore.context().registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
}

@Override public void onExitStart(AppCore app) throws Throwable {
	listeners.clear();
	try { AppCore.context().unregisterReceiver(receiver); } catch (Exception ex) {}
}

@Override public String getStateInfo() {
	return "level = " + level + "; source = " + source + "; loose= " + percentsPerHour + " %ph; lost= "+(lastLevel-startLevel)+" %";
}



public static void addListener(PowerListener listener, boolean getNow) {
	if (I != null && I.listeners.add(listener)) {
		if (getNow) listener.onPowerChanged(isPluged(), getLevel());
	}
}
public static void removeListener(PowerListener listener) {
	if (I != null) I.listeners.remove(listener);
}


/** POWERLISTENER */

public static interface PowerListener {
	public void onPowerChanged(boolean pluged, int level);
}

}
