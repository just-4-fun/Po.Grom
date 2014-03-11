package cyua.android.core.misc;

import android.os.SystemClock;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.java.shared.BaseTool;


public class Tool extends BaseTool {

/** TIME UTILS ********************************** */

public static long deviceNow() {
	return SystemClock.elapsedRealtime();// since boot
}

public static boolean isEmulator() {
	return AppCore.D && (android.os.Build.PRODUCT.contains("sdk") || android.os.Build.PRODUCT.contains("vbox"));
}

}
