package cyua.android.smsserver;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import java.util.LinkedList;

import cyua.android.core.ActivityCore;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiService;


public class Ui extends UiCore<UiState> implements View.OnKeyListener {

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
	cxt.setKeyListener(this);
}



@Override protected UiTransition processExtraAction(UiAction action) throws Exception {
	UiTransition trans = null;
	//
	switch ((UiOp) action.op) {
		case STOP:
			SmsService.stop();
			UiService.finish();
			break;
		default:
			break;
	}
	return trans;
}




/** ACTIONS */



@Override public boolean onKey(View nul, int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_BACK) {
		if (!App.isInitFinished() || App.isInitFailed()) return false;
		else if (backStack.size() <= 1) return false;
		else {
			new UiAction(UiCoreOp.BACK).execute();
			return true;
		}
	}
	return false;
}



/** UIOPs */

public static enum UiOp {
	STOP
}


}
