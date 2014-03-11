package cyua.android.client;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.LinkedList;

import cyua.android.core.ActivityCore;
import cyua.android.core.keepalive.KeepAliveService;
import cyua.android.core.location.LocationService;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.PanelManager;
import cyua.android.core.ui.UiCore;


public class Ui extends UiCore<UiState> {

private static final String TAG = Ui.class.getSimpleName();
static {
	r_drawable_bg_app = R.drawable.bg_app;
	r_drawable_bg_titlebar = R.drawable.bg_titlebar;
	r_drawable_bg_toolbar = R.drawable.bg_toolbar;
	r_drawable_leftshad = R.drawable.leftshad;
	r_drawable_rightshad = R.drawable.rightshad;
	r_drawable_fade_anima = R.drawable.fade_anima;
	r_drawable_titlebar_context = R.drawable.titlebar_context;
	r_drawable_ic_titlebar = R.drawable.ic_titlebar;
	r_layout_base_layout = R.layout.base_layout;
	r_layout_imagebutton = R.layout.imagebutton;
	r_id_toplay = R.id.topLay;
	r_id_centlay = R.id.centLay;
	r_id_botlay = R.id.botLay;
	r_dimen_left_panel_width = R.dimen.left_panel_width;
	r_dimen_right_panel_width = R.dimen.right_panel_width;
	r_string_app_name = R.string.app_name;
	r_string_title_main = R.string.title_main;
	r_string_title_fatalerror = R.string.title_fatalerror;
	r_string_message_fatalerror = R.string.message_fatalerror;
	r_color_title_text = R.color.while_a80;
	pageMainClas = Fmt.PageMain.class;
	pageInitClas = Fmt.PageInitializing.class;
	sidePanelClas = Fmt.SidePanel.class;
}


/** INSTANCE */


public Ui(ActivityCore _cxt, LinkedList _actionStack, Object cfg, Bundle savedState) {
	super(_cxt, _actionStack, cfg, savedState);
	disableTitleNavigation(true);
	//
	boolean firstInit = savedState == null;
	boolean timedOut = Settings.lastSend.get() + Settings.msgExpireTimeout < Tool.now();
	boolean isOk = !SendTask.isFailed() && !SendTask.isActive();
	if (firstInit && (timedOut || isOk)) {
		Settings.task.remove();
		Settings.clearMessage();
	}
}

@Override public void onBack(boolean isGesture) {
	if (!isGesture && !SendTask.isActive()) {
		Settings.task.remove();
		Settings.clearMessage();
		if (!KeepAliveService.isKeepAlive()) LocationService.pauseService();
	}
	super.onBack(isGesture);
}


@Override protected UiTransition processExtraAction(UiAction action) throws Exception {
	UiTransition trans = null;
	switch ((UiOp) action.op) {
		case UPDATE_THUMBS:
			Fragment f = getFragment(PanelManager.Panel.CENTER);
			if (f != null && f instanceof Fmt.PageMain) ((Fmt.PageMain)f).updateThumbs();
			break;
		case APPLY_USER_INPUT:
			Fragment f2 = getFragment(PanelManager.Panel.CENTER);
			if (f2 != null && f2 instanceof Fmt.PageMain) ((Fmt.PageMain)f2).applyUserInput();
			break;
//		case SEND_TASK:
////			trans = new UiTransition(action, Fmt.PageTaskInfo.class) { @Override public void onSuccess() { } };
//			new SendTask() {
//				@Override protected void onFinish() { super.onFinish(); }
//			}.execute();
//			break;
		default:
			break;
	}
	return trans;
}




/** ACTIONS */






/** UIOPs */

public static enum UiOp {
	APPLY_USER_INPUT, UPDATE_THUMBS
}


}
