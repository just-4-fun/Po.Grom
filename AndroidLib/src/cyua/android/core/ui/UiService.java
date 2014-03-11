package cyua.android.core.ui;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Constructor;
import java.util.LinkedList;

import cyua.android.core.ActivityCore;
import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;

import static cyua.android.core.AppCore.AppService;


public class UiService extends AppService {

private static final String TAG = UiService.class.getSimpleName();
//SINGLETON
private static UiService I;
private static Class<? extends UiCore> uiClas;
private static Class<? extends UiStateCore> stateClas;

public static  UiService instantiate(Class<? extends UiCore> _uiClas, Class<? extends UiStateCore> _stateClas) {
	if (I != null) return I;
	uiClas = _uiClas;
	stateClas = _stateClas;
	I = new UiService();
	I.initOrder = AppService.INIT_LAST;
	I.exitOrder = AppService.EXIT_FIRST;
	return I;
}

public static UiCore getUi() {
	return I != null && I.ui != null ? I.ui : null;
}

public static Object getUiState() {
	return I != null && I.config != null ? I.config : null;
}



/** INSTANCE */

private UiCore ui;
LinkedList<UiCore.BackStackItem> actionStack;
UiStateCore config;


public UiService() {
	initResources();
}

private void initResources() {
	try {
		if (config == null) config = stateClas.newInstance();
		if (actionStack == null) actionStack = new LinkedList<UiCore.BackStackItem>();
	} catch (Exception ex) { Wow.e(ex);}
}

/** APPSERVICE Methods */

@Override public void onInitStart(AppCore app) throws Throwable {
	initResources();
}
@Override public void onExitStart(AppCore app) throws Throwable {
	destroyUi();
	actionStack = null;
	config = null;
}
@Override public String getStateInfo() throws Throwable {
	String uistate = ui == null ? "null" : ui.getStateInfo();
	return "Ui state = " + uistate ;
}


@Override public void onActivityCreated(Activity activity, String cause, Bundle state) {
	destroyUi();
	try {
		Constructor<? extends UiCore> constr = (Constructor<? extends UiCore>)uiClas.getConstructor(ActivityCore.class, LinkedList.class, Object.class, Bundle.class);
		ui = constr.newInstance(AppCore.uiContext(), actionStack, config, state);
	} catch (Exception ex) { Wow.e(ex);}
}
@Override public void onActivityResumed(Activity activity, String cause) {
	if (ui != null) ui.onResume(AppCore.ACTIVITY_RECONF_START.equals(cause));
}
@Override public void onActivityPaused(Activity activity, String cause) {
	if (ui != null) ui.onPause(AppCore.ACTIVITY_RECONF_START.equals(cause));
}
@Override public void onSaveState(Activity activity, Bundle state) {
	if (ui != null) ui.onSaveState(state);
}
@Override public void onActivityDestroyed(Activity activity, String cause) {
	destroyUi();
}
private void destroyUi() {
	if (ui != null) ui.onDestroy();
	ui = null;
}


public static void finish() {
	if (AppCore.uiContext() != null) AppCore.uiContext().finish();
}





/** UISTATE */

public static class UiStateCore {
	public PanelManager.Cfg panelManagerCfg = new PanelManager.Cfg();
}



}
