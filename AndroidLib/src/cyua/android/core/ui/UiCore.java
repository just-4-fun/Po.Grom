package cyua.android.core.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import cyua.android.core.ActivityCore;
import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.map.MapCore;
import cyua.android.core.misc.Tiker;

import static cyua.android.core.AppCore.D;
import static cyua.android.core.ui.PanelManager.Action;
import static cyua.android.core.ui.PanelManager.Panel;
import static cyua.android.core.ui.PanelManager.Transition;
import static cyua.android.core.ui.PanelManager.iTransitionFragment;
import static cyua.android.core.ui.UiService.UiStateCore;


public class UiCore<UiState extends UiStateCore> implements PanelManager.Adapter, View.OnKeyListener {
private static final String TAG = UiCore.class.getSimpleName();
//private static final String KEY_RECONFIGURING = "RESTORING";
protected static final int TITLE_NONE = -1;
//
private static UiCore instance;


protected ActivityCore cxt;
protected LinkedList<BackStackItem> backStack;
protected UiState uiState;
protected PanelManager panels;
public ToolbarCore titleBar;
public ToolbarCore toolBar;
protected final Panel mainPanel = Panel.CENTER;
protected ViewGroup overlay;
protected UiTransition transAction;
protected Enum lastOp;
protected Tiker<Op> tiker;
public boolean titleNavigationEnabled = true;
private ViewGroup root;
private FrameLayout interceptor;

protected enum Op {
	EXEC, /*INIT_FRAGMENT*/
}

// R config
protected static int r_drawable_bg_app;
protected static int r_layout_base_layout;
protected static int r_id_toplay;
protected static int r_id_centlay;
protected static int r_id_botlay;
protected static int r_drawable_bg_titlebar;
protected static int r_drawable_bg_toolbar;
protected static int r_dimen_left_panel_width;
protected static int r_dimen_right_panel_width;
protected static int r_drawable_leftshad;
protected static int r_drawable_rightshad;
protected static int r_drawable_fade_anima;
protected static int r_string_app_name;
protected static int r_drawable_titlebar_context;
protected static int r_layout_imagebutton;
protected static int r_drawable_ic_titlebar;
protected static int r_string_title_main;
protected static int r_string_title_fatalerror;
protected static int r_string_message_fatalerror;
protected static int r_color_title_text;
protected static Class<? extends iFragment> pageMainClas;
protected static Class<? extends iFragment> pageInitClas;
protected static Class<? extends iFragment> sidePanelClas;




public UiCore(ActivityCore _cxt, LinkedList<BackStackItem> _actionStack, Object cfg, Bundle savedState) {
	instance = this;
	cxt = _cxt;
	cxt.setKeyListener(this);
	uiState = (UiState) cfg;
	backStack = _actionStack;
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op op, Object obj, Bundle data) {
			switch (op) {
//				case INIT_FRAGMENT: initFragment((Integer) obj, data.getBoolean(KEY_RECONFIGURING));
//					break;
				case EXEC:
					((UiAction) obj).execute();
					break;
			}
		}
	};
	//
	// ROOT
	root = createRoot(savedState);
	cxt.setContentView(root);
	// OVERLAY
//	overlay = (ViewGroup) UiHelper.inflate(R.layout.overlay);
//	overlay.setVisibility(View.GONE);
//	overlay.setOnTouchListener(new View.OnTouchListener() {
//		@Override public boolean onTouch(View v, MotionEvent event) {
//			requestShowMainInfo(false);
//			return false;
//		}
//	});
//	cxt.addContentView(overlay, lp);
	// INTERCEPTOR
	ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	interceptor = new FrameLayout(cxt);
	interceptor.setClickable(false);
	interceptor.setFocusable(true);
	interceptor.setFocusableInTouchMode(true);
	// close soft keyboard on touch
	interceptor.setOnTouchListener(new View.OnTouchListener() {
		@Override public boolean onTouch(View v, MotionEvent event) {
			testTextUnderPoint((int) event.getX(), (int) event.getY());
			return false;
		}
	});
	cxt.addContentView(interceptor, lp);
	// WAIT for activate() from UiService
	if (D) Wow.i(TAG, "Ui", "backstack size =  " + backStack.size());
}


protected ViewGroup createRoot(Bundle savedState) {
	boolean firstTime = savedState == null;
	// set bg texture if App is not inited yet
	try {
//		cxt.getWindow().getDecorView().setBackgroundResource(r_drawable_bg_app);
	} catch (Exception ex) {Wow.e(ex);}
	// BASE LAYOUT
	ViewGroup root = (ViewGroup) UiHelper.inflate(r_layout_base_layout);
	ViewGroup topLay = (ViewGroup) root.findViewById(r_id_toplay);
	ViewGroup centLay = (ViewGroup) root.findViewById(r_id_centlay);
	ViewGroup botLay = (ViewGroup) root.findViewById(r_id_botlay);
	// MAIN BAR
	titleBar = new ToolbarCore(cxt);
	titleBar.setBackground(r_drawable_bg_titlebar);
	titleBar.setLogo(buidLogo());
	View barView = titleBar.getRootView();
	topLay.addView(barView);
	// TOOL BAR
	toolBar = new ToolbarCore(cxt);
	toolBar.setBackground(r_drawable_bg_toolbar);
	toolBar.setLogo(null);
	barView = toolBar.getRootView();
	botLay.addView(barView);
	// CONTENT PANELS
	int wL = (int) UiHelper.dimension(r_dimen_left_panel_width);// width in dencity independant pixels (dip)
	int wR = (int) UiHelper.dimension(r_dimen_right_panel_width);// width in dencity independant pixels (dip)
	// if first time
	if (firstTime) {
		uiState.panelManagerCfg.LP.lockPosition(true).lockLeft(true).lockCenter(true).lockRight(true).visible(true).fragmentClass(sidePanelClas);
		uiState.panelManagerCfg.RP.lockPosition(true).lockLeft(true).lockCenter(true).lockRight(true).visible(true).fragmentClass(sidePanelClas);
		uiState.panelManagerCfg.CP.visible(true).fragmentClass(pageInitClas).width(wL);
		// Special mode config
//		if (D) Wow.i(TAG, ">>> constructRoot :: defW = " +uiState.panels.CP.defaultW+ ",  wl = " + wL + ",  rw = "+wR);
		// normal config
//		uiState.panels.LP.width(wL).visible(true);
//		uiState.panels.CP.visible(true).lockCenter(true).fragmentClass(Fmt.PageInitializing.class);
//		uiState.panels.RP.width(wR).visible(false);
	}
	//
	//
	panels = new PanelManager(cxt, this, uiState.panelManagerCfg);
	panels.setSpecialMode();
	panels.setShadowDrawables(r_drawable_leftshad, r_drawable_rightshad);
	panels.setFadeDrawables(r_drawable_fade_anima);
	panels.init(firstTime);
	//
	centLay.addView(panels);
	updateTitlebar(0);
	//
	return root;
}





/** PUBLIC API */

public void disableTitleNavigation(boolean disable) {
	titleNavigationEnabled = !disable;
}





/** INSTANCE API */


protected void onDestroy() {
	panels.onDestroy();
	titleBar.onDestroy();
	toolBar.onDestroy();
	if (tiker != null) tiker.clear();
	instance = null;
}

protected void onResume(boolean reconfiguring) {}

protected void onPause(boolean reconfiguring) {
	if (transAction != null) finishUiTransition();
}

protected void onSaveState(Bundle state) {}

public void activate() {
	new UiAction(UiCoreOp.MAIN).execute();
}

public void onBack(boolean isGesture) {
	BackStackItem item = calcBackOp(-1);
	if (item != null) new UiAction(item.op).pageBack().execute();
	else if (!isGesture) UiService.finish();
}

protected void onFathalError() {}

public String getStateInfo() {
	return "Top Op = " + topBackStackOp() + "; Last Op = " + lastOp + "; inTrans = " + (transAction != null);
}



protected UiTransition processExtraAction(UiAction action) throws Exception {
	return null;
}


private void execAction(UiAction action) throws Exception {
	boolean isMain = AppCore.isMainThread();
	if (D)
		Wow.i(TAG, "execAction", " inTrans = " + (transAction != null), "isMainThread ? " + isMain, action.toString());
	if ((transAction != null && !action.safeOp) || !isMain) {
		tiker.setTik(Op.EXEC, action, isMain ? 0 : 100);
		if (D) Wow.i(TAG, "execAction", " > retry");
		return;
	}
	//
	lastOp = action.op;
	UiTransition trans = null;
	//
	if (!(action.op instanceof UiCoreOp)) {
		trans = processExtraAction(action);
	}
	else switch ((UiCoreOp) action.op) {
		case BACK:
			onBack(true);
			break;
		case MAIN:
			trans = new UiTransition(action, pageMainClas, r_string_title_main) {
				@Override public void onSuccess() {
//					backStack.clear();
				}
			};
			break;
		case FATAL_ERROR:
			new FloatAlert(r_string_title_fatalerror, r_string_message_fatalerror) {
				@Override public void onDismiss(DialogInterface dialog) {
					super.onDismiss(dialog);
					AppCore.exit();
				}
			}.show();
			break;
	}
	//
	if (trans != null && trans.isRequired()) startUiTransition(trans);
}



/** TRANSITION */

protected void startUiTransition(UiTransition t) {
//	if (transAction != null);//	TODO throw new Exception("Ui Transition can not be complete immediately");
	transAction = t;
	panels.startTransition(transAction, true);
}
protected void finishUiTransition() {
	panels.finishTransition(true);
}
private void cancelUiTransition() {
	panels.finishTransition(false);
}
@Override public boolean requestManualTransition(Panel panel, Action act, boolean left2right) {
	if (panel != Panel.CENTER || act != Action.PAGE || !left2right) return true;// not allow open/close action
	else if (MapCore.class.isAssignableFrom(getCenterFragmentClass())) return true; // not allow on map
	new UiAction(UiCoreOp.BACK).manual().execute();
	//		if (tp.isLeftPanel()) tp.assignPage(null, false); else tp.assignPage(null, true);
	return true;// not allow open/close action
}
@Override public void onTransitionFinish(Transition t) {
	// TODO more reliable mechanism to ensure it is same action
	if (transAction == t) {
		if (!transAction.isCanceled()) {
			updateBackStack(transAction.uiAction);
			if (transAction.titleRid > 0) updateTitlebar(transAction.titleRid);
		}
		transAction = null;
	}
}


/** BACKSTACK */

protected void onBackStackIndex(int index) {
	BackStackItem item = calcBackOp(index);
	if (item != null) new UiAction(item.op).pageBack().execute();
}

protected BackStackItem calcBackOp(int i) {
	if (i < 0) i = backStack.size() - 1 + i;// neg i means i steps back
	return i < 0 || i >= backStack.size() ? null : backStack.get(i);
}
protected Enum topBackStackOp() {
	return backStack.isEmpty() ? null : backStack.getLast().op;
}

protected void setBackStack(Collection<BackStackItem> items) {
	backStack = new LinkedList<BackStackItem>(items);
}

protected void updateBackStack(UiAction action) {
	int ix = -1;
	for (int i = 0; i < backStack.size(); i++) {
		if (backStack.get(i).op == action.op) ix = i;
	}
	if (ix < 0) backStack.add(new BackStackItem(action.op));
	else for (int num = backStack.size() - 1; ix < num; ix++) backStack.removeLast();

}

protected void clearBackstack() {
	LinkedList<BackStackItem> newStack = new LinkedList<BackStackItem>();
	if (!backStack.isEmpty()) newStack.add(backStack.getLast());
	backStack = newStack;
}




/** FRAGMENTS */

protected <T extends Fragment> T getFragment(Panel panel) {
	Fragment currentF = panels.getFragment(panel);
	return (T) currentF;
}

protected Class<?> getLeftFragmentClass() {
	return getFragmentClass(Panel.LEFT);
}
protected Class<?> getRightFragmentClass() {
	return getFragmentClass(Panel.RIGHT);
}
protected Class<?> getCenterFragmentClass() {
	return getFragmentClass(Panel.CENTER);
}

protected Class<?> getFragmentClass(Panel panel) {
	Fragment currentF = getFragment(panel);
	return currentF == null ? null : currentF.getClass();
}




/** ACTIONS */

protected void updateTitlebar(int titleRid) {
	if (backStack.isEmpty()) {
		titleBar.setTitle(UiHelper.string(titleRid > 0 ? titleRid : r_string_app_name));
		return;
	}
	BackStackItem item = backStack.getLast();
	if (titleRid > 0) item.title = UiHelper.string(titleRid);
	titleBar.setTitle(item.title);
	titleBar.leftClear();
	//
	if (!titleNavigationEnabled) return;
	// skip 0th op since it's represented by logo icon and last as it's title
	for (int i = 1; i < backStack.size() - 1; i++) {
		BackStackItem it = backStack.get(i);
		View titleV = buildTitleItem(it, i);
		titleBar.leftAppend(titleV);
	}
}
View buildTitleItem(BackStackItem item, final int ix) {
	Button b = new Button(cxt);
	b.setBackgroundResource(r_drawable_titlebar_context);
	b.setText(item.title == null ? "Some Text" : item.title);
	b.setTextColor(cxt.getResources().getColor(r_color_title_text));
	b.setOnClickListener(new View.OnClickListener() {
		@Override public void onClick(View v) {
			onBackStackIndex(ix);
		}
	});
	return b;
}
View buidLogo() {
	ImageButton b = (ImageButton) UiHelper.inflate(r_layout_imagebutton);
	b.setImageResource(r_drawable_ic_titlebar);
	b.setOnClickListener(new View.OnClickListener() {
		@Override public void onClick(View v) {
			onBackStackIndex(0);
		}
	});
	return b;
}




public boolean onKey(View nul, int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_BACK) {
		if (!AppCore.isInitFinished() || AppCore.isInitFailed()) return false;
		onBack(false);
		return true;
	}
	return false;
}





/** INTERCEPTOR */

protected void testTextUnderPoint(int x, int y) {
	if (!hasEditTextUnderPoint(root, x, y)) hideKeyboard();
}
public void hideKeyboard() {
	InputMethodManager imm = (InputMethodManager) cxt.getSystemService(Context.INPUT_METHOD_SERVICE);
	if (imm != null && imm.isActive()) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
	// remove focus from text field
	interceptor.requestFocus();
}
protected boolean hasEditTextUnderPoint(ViewGroup parent, int x, int y) {
	if (parent == null) return false;// FIXME can't be but was in err report
	int len = parent.getChildCount();
	for (int $ = 0; $ < len; $++) {
		try {
			View child = parent.getChildAt($);
			Rect rect = new Rect();
			child.getGlobalVisibleRect(rect);
			//		if (D) Wow.v(TAG, "[onTouch]: rect:"+rect+"  x:"+x+"  y:"+y+"   ok:"+rect.contains(x,y)+"  child:"+child);
			if ((child instanceof EditText && rect.contains(x, y)) ||
					(child instanceof ViewGroup && rect.contains(x, y) && hasEditTextUnderPoint((ViewGroup) child, x, y)))
				return true;
		} catch (Throwable ex) {}
	}
	return false;
}






/** ACTION */

public static class UiAction {
	public Enum op;
	private boolean isBack;
	private boolean automatic = true;
	public boolean safeOp;// !! should not mess content if in transition

	public UiAction(Enum _op) { op = _op; }
	public UiAction op(Enum _op) { op = _op; return this;}
	public UiAction manual() { automatic = false; return this;}
	public UiAction anyway() { safeOp = true; return this;}
	public void execute() {
		if (instance != null) try {
			instance.execAction(this);
		} catch (Exception ex) {Wow.e(ex);}
	}

	public UiAction pageBack() {
		isBack = true;
		return this;
	}
	public UiAction redirect(Enum _op) {
		op = _op;
		return this;
	}

	@Override public String toString() {
		return "Op = " + op + ",  isAuto = " + automatic + ", isSafe = " + safeOp + ", isBack = " + isBack;
	}
}




/** UI OPERATIONS */

public static enum UiCoreOp {
	BACK,
	MAIN,
	FATAL_ERROR;
}





/** Ui CONNECTOR */

public interface iFragment<Ui extends UiCore, UiState> extends iTransitionFragment {
	public void init(Ui ui, UiState state, boolean firstInit);
	public List<View> getToolbarLefts();
	public List<View> getToolbarRights();
	public List<View> getToolbarCenters();
	public List<View> getTitlebarLefts();
	public List<View> getTitlebarRights();
	public List<View> getTitlebarCenters();
}



/** BACKSTACK ITEM */

public static class BackStackItem {
	public Enum op;
	public String title;
	public BackStackItem(Enum _op) {
		op = _op;
	}
}






public static class UiTransition extends Transition {
	private UiAction uiAction;
	private int titleRid;

	public UiTransition(UiAction action, Class<? extends iFragment> clas) {
		this(action, clas, Panel.CENTER);
	}

	public UiTransition(UiAction action, Class<? extends iFragment> clas, int _titleRid) {
		this(action, clas, Panel.CENTER);
		titleRid = _titleRid;
	}

	public UiTransition(UiAction action, Class<? extends iFragment> clas, Panel panelId, int _titleRid) {
		this(action, clas, panelId);
		titleRid = _titleRid;
	}

	public UiTransition(UiAction _uiAction, Class<? extends iFragment> _clas, Panel panelId) {
		super(instance.panels, _clas, panelId);
		uiAction = _uiAction;
		action = Action.PAGE;
		if (uiAction.isBack) {
			dirLeft2right = true;
			newOnTop = false;
		}
		else {
			dirLeft2right = true;
			newOnTop = true;
		}
		isAutomatic = uiAction.automatic;
	}
}

}
