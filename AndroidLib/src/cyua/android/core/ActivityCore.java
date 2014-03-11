package cyua.android.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;


// WARNING compatibility donwngrade: for API > 10 use Activity
public class ActivityCore extends FragmentActivity {
private static final String TAG = ActivityCore.class.getSimpleName();
private static WeakHashMap<ActivityResultListener, Integer> listenersMap = new WeakHashMap<ActivityResultListener, Integer>();



/** STATIC */

static void cleanup() {
	listenersMap.clear();
}


/** INSTANCE */

private View.OnKeyListener keyListener;



/** **   ACTIVITY LIFE CYCLE CALLBACKS */

@Override protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	AppCore.context().onActivityCreated(this, savedInstanceState);
}
@Override protected void onStart() {
	AppCore.context().onActivityStarted(this);
	super.onStart();
}
@Override protected void onResume() {
	AppCore.context().onActivityResumed(this);
	super.onResume();
}
@Override protected void onPause() {
	AppCore.context().onActivityPaused(this);
	super.onPause();
}
@Override protected void onSaveInstanceState(Bundle outState) {
	AppCore.context().onActivitySaveInstanceState(this, outState);
	super.onSaveInstanceState(outState);
}
@Override protected void onStop() {
	AppCore.context().onActivityStopped(this);
	super.onStop();
}
@Override protected void onDestroy() {
	AppCore.context().onActivityDestroyed(this);
	super.onDestroy();
	AppCore.context().onActivityDestroyFinished(this);
}

//WARNING compatibility donwngrade: for API > 10 remove method
//@SuppressLint("Override")
public FragmentManager fragmentManager() {
	return getSupportFragmentManager();
}


/** KEY CATCH & DISPATCH */
public void setKeyListener(View.OnKeyListener listener) {
	keyListener = listener;
}
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
//	if (D) Wow.v(TAG, "[onKeyDown]: "+keyCode);
	boolean consumed = keyListener != null && keyListener.onKey(null, keyCode, event);
	if (!consumed && keyCode == KeyEvent.KEYCODE_BACK) AppCore.context().onCloseByBackPressed();
	return consumed || super.onKeyDown(keyCode, event);
}


/** ACTIVITY RESULT */
public void setActivityResultListener(ActivityResultListener resultListener, int requestCode) {
	// for cases when system takes the call to start on itself
	listenersMap.put(resultListener, requestCode);
}
public void startActivityForResult(ActivityResultListener resultListener, Intent intent, int requestCode) {
	listenersMap.put(resultListener, requestCode);
	startActivityForResult(intent, requestCode);
}
@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	ActivityResultListener resultListener = null;
	for (Map.Entry<ActivityResultListener, Integer> entry : listenersMap.entrySet()) {
		if (entry.getValue() != requestCode) continue;
		resultListener = entry.getKey();
		listenersMap.remove(resultListener);
		break;
	}
	if (resultListener != null) resultListener.onActivityResult(requestCode, resultCode, data);
	else super.onActivityResult(requestCode, resultCode, data);
}



/**  RESULT LISTENER */
public static interface ActivityResultListener {
	public void onActivityResult(int requestCode, int resultCode, Intent data);
}

}
