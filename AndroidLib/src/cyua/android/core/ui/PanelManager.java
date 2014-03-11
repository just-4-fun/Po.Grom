package cyua.android.core.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.DisplayMetrics;
import android.util.FloatMath;

import static cyua.android.core.AppCore.D;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;

import cyua.android.core.R;
import cyua.android.core.misc.Tiker;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;


/** Created by far.be on 5/26/13. */
public class PanelManager extends ViewGroup {
private static final String TAG = PanelManager.class.getSimpleName();
private static final boolean USE_CACHE = false;
private static final int MAX_SETTLE_DURATION = 600; // ms
private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
private static final int EDGE_SIZE = 40; // dips
private static final int SHADOW_WIDTH = 10; // dips
private static final int INVALID_POINTER = -1;
private static final int TAG_PANEL = R.id.tag_panel;
private static final int TAG_FRAGMENT = R.id.tag_fragment;
private static int maxId = 1000;
private int leftShadRid = R.drawable.shad_l;
private int rightShadRid = R.drawable.shad_r;
private int dimRid = 0;
//
public static enum Panel {
	LEFT, CENTER, RIGHT,
}
private enum Op {REMOVE_VIEW_LAZY, FOCUS, INIT_FRAGMENT_LAZY, REMOVE_FOREGROUND}




/** INSTANCE */

/** Each panel should be configured individually */
private Cfg cfg = new PanelManager.Cfg();// must be static to keep panels info
private FragmentActivity context;
private Tiker<Op> tiker;
private Adapter adapter;
private PanelInfo LP, CP, RP;
private View leftShad, rightShad;
private GestInfo gest;
private Transition transition;
private int minVelocity;
private int maxVelocity;
private int touchSlop;
private int edgeSize, halfEdgeSize;
private int shadWidth;
private int flingDistance;
private boolean scrollingCacheEnabled;
//
private boolean specialMode;


public PanelManager(FragmentActivity _context, Adapter _validator, Cfg _cfg) {
	super(_context);
	cfg = _cfg;
	setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	context = _context;
//	if (cfg == null) throw new IllegalArgumentException("Cfg can not be null.");
	adapter = _validator;
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			switch (operation) {
				case FOCUS:
					doFocusPanel((PanelInfo) obj);
					break;
				case REMOVE_VIEW_LAZY:
					Object[] vals = (Object[]) obj;
					removePanelLayout((Integer) vals[0], (Boolean) vals[1]);
					break;
				case REMOVE_FOREGROUND:
					removePanelForeground((Integer) obj);
					break;
				case INIT_FRAGMENT_LAZY:
					Object[] args = (Object[])obj;
					initFragment((Integer) (Integer)args[0], (Boolean)args[1]);
					break;
			}
		}
	};
}



/** PUBLIC API */

public void setContent(PanelInfo panel, Class<? extends iTransitionFragment> contentClas) {
	if (transition != null && transition.panel == panel) finishTransition(true);
	FragmentManager fman = context.getSupportFragmentManager();
	FragmentTransaction ftn = fman.beginTransaction();
	panel.contentClass = contentClas;
	createInitialPanelLayout(panel, fman, ftn, true);
	if (!ftn.isEmpty()) ftn.commitAllowingStateLoss();
}

public void setSpecialMode() {
	specialMode = true;
}

public void setShadowDrawables(int leftRid, int rightRid) {
	leftShadRid = leftRid; rightShadRid = rightRid;
}
public void setFadeDrawables(int rid) {
	dimRid = rid;
}


public Fragment getFragment(Panel panelId) {
	PanelInfo panel = panelId == Panel.LEFT ? LP : panelId == Panel.CENTER ? CP : RP;
	Fragment f = UiHelper.fragment(panel.contentId);
	if (f == null || f instanceof FragmentStub) return null;
	return f;
}

public int getContentId(Panel panelId) {
	PanelInfo panel = panelId == Panel.LEFT ? LP : panelId == Panel.CENTER ? CP : RP;
	return panel.contentId;
}

public void startTransition(Transition t, boolean commitPrev) {
	if (t.isAutomatic()) endGesture();
	finishTransition(commitPrev);
	if (AppCore.isExitStarted()) return;
	startNewTransition(t);
}

public void finishTransition(boolean commit) {
	if (transition == null) return;
	if (commit) transition.isAutomatic = true;// forward to end
	else transition.isCanceled = true;
	finishCurrentTransition();
}



private void startNewTransition(Transition t) {
	if (transition != null) finishCurrentTransition();
	transition = t;
	transition.startExecute();
}

private void finishCurrentTransition() {
	if (gest != null && gest.tz == transition) endGesture();
	transition.finishExecute();
	if (!transition.isSilent) {
		adapter.onTransitionFinish(transition);
		// Overridable callbacks
		if (transition.isCanceled) transition.onCancel();
		else transition.onSuccess();
	}
	transition = null;
}

private void requestManualTransition(Panel panel, Action act, boolean left2right) {
	boolean notConsumed = !adapter.requestManualTransition(panel, act, left2right);
	if (notConsumed && (act == Action.OPEN || act == Action.CLOSE)) {
		startTransition(new Transition(this, panel, act, left2right, false, true), true);
	}
}


/** PROCESSING API */

public void onDestroy() {
	tiker.clear();
}

public void init(boolean firstInit) {
	/** CALC DEFAULT PANEL SIZES */
	Resources rs = context.getResources();
	DisplayMetrics metrics = rs.getDisplayMetrics();
	float scale = metrics.density;
	edgeSize = (int) (EDGE_SIZE * scale + .5f);
	halfEdgeSize = edgeSize / 2;
	shadWidth = (int) (SHADOW_WIDTH * scale + .5f);
	/** INIT OTHER STAFF */
	setWillNotDraw(false);
	setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
	setFocusable(true);
//	setClipChildren(false);
	ViewConfiguration viewConfig = ViewConfiguration.get(context);
	touchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfig);
	minVelocity = viewConfig.getScaledMinimumFlingVelocity();
	maxVelocity = viewConfig.getScaledMaximumFlingVelocity();
	flingDistance = (int) (MIN_DISTANCE_FOR_FLING * scale);
	/** SET / RESTORE CONTENT */
	LP = cfg.LP; CP = cfg.CP; RP = cfg.RP;
	PanelInfo[] panels = new PanelInfo[3];
	panels[0] = CP;
	panels[1] = RP.focused ? LP : RP;
	panels[2] = RP.focused ? RP : LP;
	if (!RP.focused) LP.focused = true;
	//
	FragmentManager fman = context.getSupportFragmentManager();
	FragmentTransaction ft = fman.beginTransaction();
	for (PanelInfo p : panels) {
		if (p.realW == 0 && p.defaultW != 0) {
			p.realW = p.defaultW;
//			p.realW = (int) (p.defaultW * scale + .5f);
		}
		View content = createInitialPanelLayout(p, fman, ft, firstInit);
		Fragment f = (Fragment) content.getTag(TAG_FRAGMENT);
		if (!p.visible) {
			if (!f.isHidden()) ft.hide(f);
			content.setVisibility(View.GONE);
		}
		// setup SHADOWS
		if (p == LP) leftShad = addShadow(p);
		else if (p == RP) rightShad = addShadow(p);
		if (D)
			Wow.i(TAG, "init", "firstInit ? " + firstInit, "panel = " + p.id + ", frgm = " + (f == null ? "null" : f.getClass().getSimpleName()) + ",  realW = " + p.realW + ",  deaultW = " + p.defaultW);
	}
	//
	if (!ft.isEmpty()) {
		ft.commitAllowingStateLoss();
		// to be ready for search through FragmentManager later in this call chain
		fman.executePendingTransactions();
	}
//	if (D) Wow.i(TAG, ">>>PPP init :: scale = " +scale);
}

private View addShadow(PanelInfo panel) {
	int resId = panel == LP ? leftShadRid : rightShadRid;
	ImageView shad = new ImageView(context);
	shad.setBackgroundResource(resId);
	addView(shad);
	return shad;
}

private Fragment newFragment(Class<? extends iTransitionFragment> cls) {
	try {return (Fragment) cls.newInstance(); } catch (Exception ex) { Wow.e(ex); }
	return null;
}

private View createInitialPanelLayout(PanelInfo panel, FragmentManager fman, FragmentTransaction ft, boolean firtstInit) {
	boolean hasId = panel.contentId > 0;
	View content = null;
	if (hasId) content = findViewById(panel.contentId);
	if (content == null || !(content instanceof PanelLayout)) {
		if (!hasId) panel.contentId = generateId();
		content = new PanelLayout(context);
		content.setId(panel.contentId);
		content.setTag(TAG_PANEL, panel);
		addView(content);
	}
	//
	Fragment f = null;
	if (hasId) f = fman.findFragmentById(panel.contentId);
	if (panel.contentClass == null) panel.contentClass = FragmentStub.class;
	if (f != null && f.getClass() != panel.contentClass) {
		if (f instanceof iTransitionFragment) ((iTransitionFragment) f).removeContent();
		ft.remove(f); f = null;
	}
	if (f == null) {
		f = newFragment(panel.contentClass);
		ft.add(panel.contentId, f);
	}
	content.setTag(TAG_FRAGMENT, f);
	lazyInitFragment(panel.contentId, firtstInit);
	return content;
}

protected void lazyInitFragment(int contentId, boolean firstInit) {
	Object[] args = new Object[]{contentId, firstInit};
	tiker.addTik(Op.INIT_FRAGMENT_LAZY, args, 0);
}
private void initFragment(int contentId, boolean firstInit) {
	Fragment f = UiHelper.fragment(contentId);
	if (D)
		Wow.i(TAG, "initFragment", "id = " + contentId + ",  fragm = " + (f == null ? "null" : f.getClass().getSimpleName() + ",  removing ? " + f.isRemoving() + ",  added ? " + f.isAdded() + ", hidden ? " + f.isHidden() + ", visible ? " + f.isVisible()) + ", hasView ? " + (f.getView() != null));
	if (f == null || f.isRemoving() || AppCore.isUiDestroyed()) return;
	else if (f.getView() != null) {
		if (f instanceof iTransitionFragment) ((iTransitionFragment) f).initContent(firstInit);
	}
	else lazyInitFragment(contentId, firstInit);
}


private PanelLayout createTransPanelLayout() {
	if (transition == null) return null;
	PanelInfo panel = transition.panel;
	int newId = generateId();
	PanelLayout content = new PanelLayout(context);
	content.setId(newId);
	content.setTag(TAG_PANEL, panel);
	addView(content);
	//
	FragmentManager fman = context.getSupportFragmentManager();
	FragmentTransaction ftn = fman.beginTransaction();
	transition.fragment = newFragment(transition.contentClass);
	ftn.add(newId, transition.fragment);
	ftn.commitAllowingStateLoss();
	content.setTag(TAG_FRAGMENT, transition.fragment);
	return content;
}
private int generateId() {
	int id = maxId++;
	if (findViewById(id) == null) return id;
	else return generateId();
}

private void removePanelLayout(int viewId, boolean removeContent) {
	View content = findViewById(viewId);
	FragmentManager fman = context.getSupportFragmentManager();
	FragmentTransaction ftn = fman.beginTransaction();
	Fragment f = fman.findFragmentById(viewId);
	if (f == null && content != null && content.getTag(TAG_FRAGMENT) != null) {
		content.setVisibility(View.GONE);
		tiker.addTik(Op.REMOVE_VIEW_LAZY, new Object[]{viewId, removeContent}, 0);
	}
	else {
		if (f != null) {
			if (removeContent && f instanceof iTransitionFragment) ((iTransitionFragment) f).removeContent();
			ftn.remove(f);
			ftn.commitAllowingStateLoss();
		}
		if (content != null) removeView(content);
	}
}

private void focusPanel(final PanelInfo panel) {
	doFocusPanel(panel);
//	tiker.setTik(Op.FOCUS, panel, 0);
}
private void doFocusPanel(final PanelInfo panel) {
	// brings views to from (bringToFront method gleeches)
	View cont = findViewById(panel.contentId);
	if (cont == null) return;
	int pos = getChildCount() - 1;
	if (getChildAt(pos) != cont) {
		// shadow
		if (panel != CP) bringOnTop(panel == RP ? rightShad : leftShad);
		// Transition Content
		if (transition != null && transition.panel == panel && transition.newContent != null) {
			if (transition.newOnTop) {
				bringOnTop(transition.currContent); // cont
				bringOnTop(transition.newContent);
			}
			else {
				bringOnTop(transition.newContent);
				bringOnTop(transition.currContent); // cont
			}
		}
		else bringOnTop(cont); // content
		// bring both shadows
		if (panel == CP) {bringOnTop(rightShad); bringOnTop(leftShad);}
	}
	if (!panel.visible) changePanelVisible(panel, true);
	if (panel != CP) panel.focused = true;
	if (panel == LP) RP.focused = false;
	else if (panel == RP) LP.focused = false;

	if (D) Wow.i(TAG, "doFocusPanel", "panel = [" + panel.id + "]");
}
private void bringOnTop(View view) {
	if (view == null) return;
	removeView(view);
	addView(view);
}

private void changePanelVisible(final PanelInfo panel, final boolean visible) {
	FragmentManager fman = context.getSupportFragmentManager();
	FragmentTransaction ftn = fman.beginTransaction();
	Fragment f = fman.findFragmentById(panel.contentId);
	if (f != null) {
		if (visible && f.isHidden()) ftn.show(f);
		else if (!visible && !f.isHidden()) ftn.hide(f);
		if (!ftn.isEmpty()) ftn.commitAllowingStateLoss();
	}
	else if (D) Wow.i(TAG, "changePanelVisible", "CONTENT LOST?");// TODO throw ERR
	//
	panel.visible = visible;
	PanelLayout content = (PanelLayout) findViewById(panel.contentId);
	content.setVisibility(panel.visible ? View.VISIBLE : View.GONE);
}

private void removePanelForeground(int contentId) {
	PanelLayout cont = (PanelLayout) findViewById(contentId);
	if (cont != null) cont.setForeground(null);
}






@Override public boolean onInterceptTouchEvent(MotionEvent ev) {
	//
	int actionMask = MotionEventCompat.getActionMasked(ev);
	//
	if ((gest == null && actionMask != MotionEvent.ACTION_DOWN) || actionMask == MotionEvent.ACTION_CANCEL || actionMask == MotionEvent.ACTION_UP || (transition != null && transition.isAutomatic)) {
		endGesture();
		return false;
	}
	//
	switch (actionMask) {
		case MotionEvent.ACTION_MOVE:
			if (transition == null) gest.detectDrag(ev);
			break;
		case MotionEvent.ACTION_DOWN:
/*
			if (transition != null) {
				transition.isAutomatic = true;// fprward to end
				finishTransition();
			}
*/
			beginGesture(ev);
/*
			if (transition == null) beginGesture(ev);
			else {
				transition.stopScroll();
				gest.restart(ev, false);
			}
*/
			break;
		case MotionEventCompat.ACTION_POINTER_UP:
			gest.reassignPointer(ev);
			break;
	}
	//
	if (gest != null) gest.addMovement(ev);
	return gest != null && transition != null;
}

@Override public boolean onTouchEvent(MotionEvent ev) {
	if (gest == null) return false;// Theoretically transition != null
	//
	int actionMask = MotionEventCompat.getActionMasked(ev);
	//
	switch (actionMask) {
		case MotionEvent.ACTION_MOVE:
			if (gest.tz == null) gest.detectDrag(ev);
			if (gest != null && gest.tz != null) gest.keepDragging(ev);
			break;
		case MotionEvent.ACTION_UP:
			if (gest.tz != null) gest.finishDrag(ev);
			else endGesture();
			break;
		case MotionEvent.ACTION_DOWN:
			if (gest.tz != null) gest.tz.stopScroll();
			gest.restart(ev, false);
			break;
		case MotionEvent.ACTION_CANCEL:
			if (gest.tz != null) gest.cancel();
			else endGesture();
			break;
		case MotionEventCompat.ACTION_POINTER_DOWN:
			gest.restart(ev, true);
			break;
		case MotionEventCompat.ACTION_POINTER_UP:
			gest.reassignPointer(ev);
			break;
	}
	//
	if (gest != null) gest.addMovement(ev);
	return gest != null;
}


private void beginGesture(MotionEvent ev) {
	if (gest != null) endGesture();
	/** DETECT PANEL AND AREA Under Touch Down */
	float x = ev.getX(), y = ev.getY();
	PanelInfo panel = null;
	int area = 0;
	boolean obscured = false;
	View cont = null;
	// traverse from top looking focused view
	for (int num = getChildCount(), i = num - 1; i >= 0; i--) {
		cont = getChildAt(i);
		if (!(cont instanceof PanelLayout) || cont.getVisibility() != View.VISIBLE) continue;
		if (x < cont.getLeft() - halfEdgeSize || x > cont.getRight() + halfEdgeSize) continue;
		panel = (PanelInfo) cont.getTag(TAG_PANEL);
		// check if screen edge or between panel
		boolean isScreenEdge = x < getLeft() + edgeSize || x > getRight() - edgeSize;
		int edgeW = isScreenEdge ? edgeSize : halfEdgeSize;
		edgeW = edgeSize;
		if (x < cont.getLeft() + edgeW) area |= LEFT_EDGE;
		if (x > cont.getRight() - edgeW) area |= RIGHT_EDGE;
		if (area == 0) area = CENTER;
		// check is focused
		if (panel != CP && !panel.focused) focusPanel(panel);
		break;
	}
	//
	if (panel == null || (panel.lockedArea & area) != 0) ;// return
	else {
		gest = new GestInfo(panel, area, x, y);
		int pointerIndex = MotionEventCompat.getActionIndex(ev);
		gest.pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

	}

	String areas = area == 0 ? "NONE" : area == 1 ? "LEFT" : area == 2 ? "CENTER" : area == 4 ? "RIGHT" : area == 5 ? "LEFT&RIGHT" : "? " + area;
	if (D)
		Wow.i(TAG, "beginGesture", "GEST INFO:  panel = [" + (panel == null ? "null" : panel.id) + "], Area = [" + areas + "], NEW = " + (gest != null) + "]");
}

private void endGesture() {
	if (gest != null) gest.drop();// is set to null
}

/** Called on invalidate */
@Override public void computeScroll() {
	if (transition != null) transition.nextScroll();
}

private void setScrollingCacheEnabled(boolean enabled) {
	// TODO can be optimized only to scrollable content
	if (scrollingCacheEnabled != enabled) {
		scrollingCacheEnabled = enabled;
		if (USE_CACHE) {
			int size = getChildCount();
			for (int i = 0; i < size; ++i) {
				View child = getChildAt(i);
				if (child.getVisibility() != GONE) {
					child.setDrawingCacheEnabled(enabled);
				}
			}
		}
	}
}

@Override protected void onMeasure(final int widthSpec, final int heightSpec) {
	int width = MeasureSpec.getSize(widthSpec);
	int height = MeasureSpec.getSize(heightSpec);
	setMeasuredDimension(width, height);
	//
	PanelLayout contL = (PanelLayout) findViewById(LP.contentId);
	PanelLayout contC = (PanelLayout) findViewById(CP.contentId);
	PanelLayout contR = (PanelLayout) findViewById(RP.contentId);
	//
	if (specialMode) { onMeasureSpecial(width, height, contL, contC, contR); return;}
	//
	// measure LEFT Panel
	int wL = LP.realW > width ? width : LP.realW;
	contL.measure(makeMeasureSpec(wL, EXACTLY), makeMeasureSpec(height, EXACTLY));
	// measure RIGHT Panel
	int wR = RP.realW > width ? width : RP.realW;
	contR.measure(makeMeasureSpec(wR, EXACTLY), makeMeasureSpec(height, EXACTLY));
	// measure CENTER Panel
	int wC = width;
	if (LP.visible && (transition == null || transition.panel != LP || transition.action == Action.PAGE)) wC -= wL;
	if (RP.visible && (transition == null || transition.panel != RP || transition.action == Action.PAGE)) wC -= wR;
	contC.measure(makeMeasureSpec(wC, EXACTLY), makeMeasureSpec(height, EXACTLY));
	// measure NEW CONTENT if any
	if (transition != null) {
		int tmpW = transition.panel == LP ? wL : transition.panel == RP ? wR : wC;
		if (transition.newContent != null)
			transition.newContent.measure(makeMeasureSpec(tmpW, EXACTLY), makeMeasureSpec(height, EXACTLY));
	}

//		if (D) Wow.i(TAG, ">>>PPP onMeasure :: CW = " +contC.getMeasuredWidth() );
}
private void onMeasureSpecial(int width, int height, PanelLayout contL, PanelLayout contC, PanelLayout contR) {
	// measure CENTER Panel
	int wC = CP.realW + 40 > width ? width : CP.realW;
	contC.measure(makeMeasureSpec(wC, EXACTLY), makeMeasureSpec(height, EXACTLY));
	// measure LEFT & RIGHT Panel
	int w = (width - wC) / 2;
	contL.measure(makeMeasureSpec(w, EXACTLY), makeMeasureSpec(height, EXACTLY));
	contR.measure(makeMeasureSpec(w, EXACTLY), makeMeasureSpec(height, EXACTLY));
	// measure NEW CONTENT if any
	if (transition != null) {
		int tmpW = transition.panel == CP ? wC : w;
		if (transition.newContent != null)
			transition.newContent.measure(makeMeasureSpec(tmpW, EXACTLY), makeMeasureSpec(height, EXACTLY));
	}

}

@Override protected void onLayout(boolean changed, int lt, int tp, int rt, int bt) {
	PanelLayout contL = (PanelLayout) findViewById(LP.contentId);
	PanelLayout contC = (PanelLayout) findViewById(CP.contentId);
	PanelLayout contR = (PanelLayout) findViewById(RP.contentId);
	//
	if (specialMode) { onLayoutSpecial(lt, tp, rt, bt, contL, contC, contR); return;}
	//
	int width = rt - lt;
	// layout LEFT Panel
	int wL = LP.realW > width ? width : LP.realW;
	contL.layout(lt, tp, lt + wL, bt);
	// layout RIGHT Panel
	int wR = RP.realW > width ? width : RP.realW;
	contR.layout(rt - wR, tp, rt, bt);
	// layout CENTER Panel
	int leftC = LP.visible ? contL.getRight() : lt;
	int rightC = RP.visible ? contR.getLeft() : rt;
	int shadLx = -1, shadRx = -1;
	if (transition != null) {
		// draw scroll here to sync with shadow
		transition.scrollContent.scrollTo(transition.currX, 0);
		//
		PanelLayout contN = transition.newContent, contTmp = null;
		if (transition.panel == LP) {
			if (transition.action != Action.PAGE) {
				leftC = lt;
				shadLx = transition.scrollContent.getRight() - transition.currX;
			}
			contTmp = contL;
		}
		else if (transition.panel == RP) {
			if (transition.action != Action.PAGE) {
				rightC = rt;
				shadRx = transition.scrollContent.getLeft() - shadWidth - transition.currX;
			}
			contTmp = contR;
		}
		else contTmp = contC;
		// layout NEW CONTENT if any
		if (contN != null && contTmp != null)
			contN.layout(contTmp.getLeft(), contTmp.getTop(), contTmp.getRight(), contTmp.getBottom());
	}
	// Hide / show CENTER
	if (leftC < rightC) {
		if (!CP.visible) changePanelVisible(CP, true);
		contC.layout(leftC, tp, rightC, bt);
	}
	else if (CP.visible) changePanelVisible(CP, false);
	// SHADOW
	if (shadLx == -1) shadLx = LP.visible ? contL.getRight() : lt;
	if (shadRx == -1) shadRx = RP.visible ? contR.getLeft() - shadWidth : rt - shadWidth;
	leftShad.layout(shadLx, tp, shadLx + shadWidth, bt);
	rightShad.layout(shadRx, tp, shadRx + shadWidth, bt);
	//
	invalidate();// TODO optimize

//	if (D) Wow.i(TAG, ">>>PPP onLayout :: " );

}
private void onLayoutSpecial(int lt, int tp, int rt, int bt, PanelLayout contL, PanelLayout contC, PanelLayout contR) {
	int width = rt - lt;
	int wC = CP.realW + 40 > width ? width : CP.realW;
	int w = (width - wC) / 2;
	// layout LEFT & RIGHT Panel
	contL.layout(lt, tp, lt + w, bt);
	contR.layout(rt - w, tp, rt, bt);
	// layout LEFT & RIGHT Panel
	int leftC = contL.getRight();
	int rightC = contR.getLeft();
	contC.layout(leftC, tp, rightC, bt);
	// TEMP Content
	if (transition != null) {
		// draw scroll here to sync with shadow
		transition.scrollContent.scrollTo(transition.currX, 0);
		//
		transition.newContent.layout(contC.getLeft(), contC.getTop(), contC.getRight(), contC.getBottom());
	}
	// SHADOWS
	leftShad.layout(leftC, tp, leftC + shadWidth, bt);
	rightShad.layout(rightC - shadWidth, tp, rightC, bt);
	//
	invalidate();// TODO optimize
}






/** SUPPORT CLASSES */

public static final int LEFT_EDGE = 1 << 0;
public static final int CENTER = 1 << 1;
public static final int RIGHT_EDGE = 1 << 2;
public enum Action {PAGE, OPEN, CLOSE}



public static class Cfg {
	public PanelInfo LP, CP, RP;
	public Cfg() {
		LP = new PanelInfo(Panel.LEFT);
		CP = new PanelInfo(Panel.CENTER);
		RP = new PanelInfo(Panel.RIGHT);
	}
}


public interface Adapter {
	/** @return true if consumed by callee. Else caller will perform on it's consideration. */
	public boolean requestManualTransition(Panel panel, Action act, boolean left2right);
	public void onTransitionFinish(Transition transition);
}



/*Warn! Instances of PanelInfo are staticaly stored in Cfg*/
public static class PanelInfo {
	public Panel id;
	public int defaultW;// in dip
	private int realW;// in pix
	private Class<? extends iTransitionFragment> contentClass;
	private int contentId;
	private boolean locked;// no OPEN / CLOSE
	private boolean visible;
	private boolean focused;
	private int lockedArea;// no touch area (bits)
	//

	public PanelInfo(Panel panelId) { id = panelId; }

	public PanelInfo fragmentClass(Class<? extends iTransitionFragment> _contentClass) {
		contentClass = _contentClass;
		return this;
	}
	public String name() {return id.name();}
	public PanelInfo width(int width) { defaultW = width; return this; }
	public PanelInfo visible(boolean _visible) { visible = _visible; return this; }
	public PanelInfo lockPosition(boolean yes) { locked = yes; return this; }
	public PanelInfo lockLeft(boolean yes) {
		lockedArea = yes ? lockedArea | LEFT_EDGE : lockedArea & ~LEFT_EDGE; return this;
	}
	public PanelInfo lockCenter(boolean yes) {
		lockedArea = yes ? lockedArea | CENTER : lockedArea & ~CENTER; return this;
	}
	public PanelInfo lockRight(boolean yes) {
		lockedArea = yes ? lockedArea | RIGHT_EDGE : lockedArea & ~RIGHT_EDGE; return this;
	}
}




/** TRANSITION API */

public static class Transition {
	PanelManager man;
	PanelInfo panel; // Request to adapter
	Action action; // Request to adapter
	boolean dirLeft2right; // Request to adapter
	boolean newOnTop;// Response from adapter
	private Class<? extends iTransitionFragment> contentClass;// Response from adapter
	private boolean isCanceled;//Response to adapter
	private boolean isSilent;
	private Fragment fragment;//Response to adapter
	/**  */
	boolean isAutomatic;// caused by adapter not by dragging
	private boolean isInstant;// caused by adapter wo animation
	//	private boolean isManual;// manual by adapter
	private Scroller scroller;
	private PanelLayout currContent;
	private PanelLayout newContent;
	private PanelLayout scrollContent;
	private PanelLayout stillContent;
	private int startX, endX, currX;


	public Transition(PanelManager _man, Class<? extends iTransitionFragment> _clas, Panel panelId) {
		man = _man; contentClass = _clas;
		panel = panelId == Panel.LEFT ? man.LP : panelId == Panel.CENTER ? man.CP : man.RP;
	}

	private Transition(PanelManager _man, Panel panelId, Action act, boolean left2right, boolean auto, boolean silent) {
		man = _man; dirLeft2right = left2right; action = act;
		isAutomatic = auto; isSilent = silent;
		panel = panelId == Panel.LEFT ? man.LP : panelId == Panel.CENTER ? man.CP : man.RP;
	}
	//	public Transition(PanelManager _man, Class<? extends iTransitionFragment> _clas, Panel panelId, Action _act, boolean left2right, boolean placeOnTop, boolean auto) {
//		panel = panelId == Panel.LEFT ? man.LP : panelId == Panel.CENTER ? man.CP : man.RP;
//		contentClass = _clas;
//		action = _act;
//		dirLeft2right = left2right;
//		newOnTop = placeOnTop;
//		isAutomatic = auto;
//	}
	public boolean isRequired() { return panel.contentClass != contentClass; }
	public boolean isLeftPanel() {return panel.id == Panel.LEFT;}
	public boolean isCenterPanel() {return panel.id == Panel.CENTER;}
	public boolean isRightPanel() {return panel.id == Panel.RIGHT;}
	public boolean isOpenAction() {return action == Action.OPEN;}
	public boolean isCloseAction() {return action == Action.CLOSE;}
	public boolean isPageAction() {return action == Action.PAGE;}
	public boolean isLeft2Right() {return dirLeft2right;}
	public boolean isAutomatic() {return isAutomatic;}

	public void assignPage(Class<? extends iTransitionFragment> _contentClass, boolean _newOnTop) {
		contentClass = _contentClass; newOnTop = _newOnTop;
	}
	public boolean isCanceled() {return isCanceled;}
	public Fragment getFragment() {return fragment;}


	private void startScrollTo(int x, int velocity) {
		Interpolator interpolator = new Interpolator() {
			public float getInterpolation(float t) { t -= 1.0f; return t * t * t * t * t + 1.0f; }
		};
		scroller = new Scroller(man.context, interpolator);
		//
		int sx = currX;//scrollContent.getScrollX();
		int dx = x - sx;
		if (dx != 0) {
			man.setScrollingCacheEnabled(true);
			// CALC DURATION
			final int width = scrollContent.getWidth();
			final int halfWidth = width / 2;
			float distanceRatio = Math.min(1f, (float) Math.abs(dx) / width);
			distanceRatio -= 0.5f; // center the values about 0.
			distanceRatio *= 0.3f * Math.PI / 2.0f;
			distanceRatio = FloatMath.sin(distanceRatio);
			final float distance = halfWidth + halfWidth * distanceRatio;
			int duration = 0;
			velocity = Math.abs(velocity);
			if (velocity > 0) {
				duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
			}
			else {
				final float pageDelta = (float) Math.abs(dx) / width;
				duration = (int) ((pageDelta + 1) * 100);//TODO why
				duration = MAX_SETTLE_DURATION;
			}
			duration = Math.min(duration, MAX_SETTLE_DURATION);
			if (duration < 500) duration = 500;
			//
			scroller.startScroll(sx, 0, dx, 0, duration);
		}

		if (D)
			Wow.i(TAG, "startScrollTo", "To :: " + "x = [" + x + "], velocity = [" + velocity + "], dx = [" + dx + "]");

		//
		nextScroll();
	}
	private void nextScroll() {

//		if (D) Wow.i(TAG, ">>>PPP nextScroll ::scroller = [" + scroller+ "]");

		if (scroller == null) return;
		else if (scroller.isFinished()) {
			man.finishCurrentTransition();
		}
		else {
			scroller.computeScrollOffset();
			setScrollTo(scroller.getCurrX(), false);
		}
	}
	private void setScrollTo(final float scrollX, final boolean now) {
		currX = (int) scrollX;
		if (now) scrollContent.scrollTo(currX, 0);
		man.requestLayout();// causes sync content and shadows

//		if (D) Wow.i(TAG, ">>>PPP setScrollTo :: " + "currX = [" +currX + "], contScrollX = ["+scrollContent.getScrollX()+"]");

	}
	private void stopScroll() {
		if (scroller == null) return;
		scroller.abortAnimation();
		// Done with scroll, no longer want to cache view drawing.
		man.setScrollingCacheEnabled(false);

		if (D) Wow.i(TAG, "stopScroll", "At :: scrollX = [" + scrollContent.getScrollX() + "]");
	}

	private boolean isCloseToEnd() {
		if (isAutomatic) return true;
		float scrollX = currX;//scrollContent.getScrollX();
		return Math.abs(endX - scrollX) / scrollContent.getWidth() < 0.5;
	}

	private void startExecute() {
		if (contentClass == null) contentClass = FragmentStub.class;
		currContent = (PanelLayout) man.findViewById(panel.contentId);
		int width = currContent.getWidth();//rightX - leftX
		// TODO throw ERR: child should always be and of class PanelLayout
		if (action == Action.PAGE) {
			newContent = man.createTransPanelLayout();
			scrollContent = newOnTop ? newContent : currContent;
			stillContent = newOnTop ? currContent : newContent;
			stillContent.setDim(true, false);
			man.focusPanel(panel);
			if (dirLeft2right) {
				startX = newOnTop ? width : 0;
				endX = newOnTop ? 0 : -width;
			}
			else {
				startX = newOnTop ? -width : 0;
				endX = newOnTop ? 0 : width;
			}
		}
		else if (action == Action.OPEN) {
			man.changePanelVisible(panel, true);
			scrollContent = currContent;
			man.focusPanel(panel);
			startX = dirLeft2right ? width : -width;
			endX = 0;
		}
		else if (action == Action.CLOSE) {
			scrollContent = currContent;
			startX = 0;
			endX = dirLeft2right ? -width : width;
		}
		//
		if (D)
			Wow.i(TAG, "startExecute", "TRANS: panel = [" + panel.id + "], act = [" + action + "], onTop = [" + newOnTop + "], dir = [" + (dirLeft2right ? "LR" : "RL") + "],  from = [" + startX + "],  to = [" + endX + "],  auto = [" + (isAutomatic) + "]");
		setScrollTo(isInstant ? endX : startX, false);
		if (isAutomatic) startScrollTo(endX, 0);
	}

	private void finishExecute() {
		stopScroll();
		// Apply all fragmrnt ops done before
		FragmentManager fman = man.context.getSupportFragmentManager();
		fman.executePendingTransactions();
		// check if reached at least half the way
		if (!isCanceled && isCloseToEnd()) {
			if (action == Action.PAGE) {
				man.removePanelLayout(panel.contentId, !isCanceled);
				panel.contentId = newContent.getId();
				panel.contentClass = contentClass;
				man.lazyInitFragment(panel.contentId, true);
			}
			else if (action == Action.CLOSE) man.changePanelVisible(panel, false);
			setScrollTo(endX, true);
		}
		else {
			isCanceled = true;
			if (action == Action.PAGE) man.removePanelLayout(newContent.getId(), !isCanceled);
			else if (action == Action.OPEN) man.changePanelVisible(panel, false);
			setScrollTo(startX, true);
		}
		//
		if (stillContent != null) stillContent.setDim(false, stillContent.getId() == panel.contentId);
		if (D) Wow.i(TAG, "finishExecute", "TRANS :: ");
	}

	private void onCancel() { }

	public void onSuccess() { }
}








/** GESTURE API */

private class GestInfo {
	private PanelInfo panel;
	private int area;
	private Action act;
	private boolean left2right;
	private float initX, initY, lastX, lastY;
	private int pointerId = INVALID_POINTER;
	private VelocityTracker velocityTracker;
	private Transition tz;
	//

	private GestInfo(final PanelInfo _panel, final int _area, float x, float y) {
		panel = _panel; area = _area;
		initX = lastX = x;
		initY = lastY = y;
		velocityTracker = VelocityTracker.obtain();
	}
	private void drop() {
		velocityTracker.recycle();
		gest = null;
	}
	private Action calcAction(boolean lt2rt) {
		Action action = Action.PAGE;
		if (area == CENTER) action = Action.PAGE;
		else {
			boolean edgeR = (area & RIGHT_EDGE) != 0;
			boolean edgeL = (area & LEFT_EDGE) != 0;
			boolean edgeL2Left = edgeL && !lt2rt;
			boolean edgeL2Right = edgeL && lt2rt;
			boolean edgeR2Left = edgeR && !lt2rt;
			boolean edgeR2Right = edgeR && lt2rt;
			if (panel == CP) {
				if (edgeL2Right && !LP.visible && !LP.locked) {action = Action.OPEN; panel = LP;}
				else if (edgeR2Left && !RP.visible && !RP.locked) {action = Action.OPEN; panel = RP;}
				else if (edgeL2Left && LP.visible && !LP.locked) {action = Action.CLOSE; panel = LP;}
				else if (edgeR2Right && RP.visible && !RP.locked) {action = Action.CLOSE; panel = RP;}
			}
			else if ((panel == LP && edgeR2Left && !LP.locked)
					|| (panel == RP && edgeL2Right && !RP.locked)) action = Action.CLOSE;
			else if ((panel == LP && edgeL2Right && !LP.locked)
					|| (panel == RP && edgeR2Left && !RP.locked)) action = Action.PAGE;
		}
		return action;
	}

	private void addMovement(final MotionEvent ev) {
		velocityTracker.addMovement(ev);
	}
	private void clearMovement() {
		velocityTracker.clear();
	}
	private int getVelocity() {
		velocityTracker.computeCurrentVelocity(1000, maxVelocity);
		return (int) VelocityTrackerCompat.getXVelocity(velocityTracker, pointerId);
	}
	private void restart(MotionEvent ev, boolean justLast) {
		lastX = ev.getX();
		lastY = ev.getY();
		int pointerIndex = MotionEventCompat.getActionIndex(ev);
		pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (!justLast) {
			clearMovement();
			initX = lastX;
			initY = lastY;
		}
	}
	private void reassignPointer(MotionEvent ev) {
		int pointerIndex = MotionEventCompat.getActionIndex(ev);
		int currPointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == currPointerId || pointerId == INVALID_POINTER) {
			pointerIndex = pointerIndex == 0 ? 1 : 0;
			pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
			lastX = MotionEventCompat.getX(ev, pointerIndex);
			clearMovement();
		}
		else {
			pointerIndex = MotionEventCompat.findPointerIndex(ev, pointerId);
			if (pointerIndex == -1) pointerId = INVALID_POINTER;
		}
	}
	private void detectDrag(MotionEvent ev) {
		int activePointerIndex = MotionEventCompat.findPointerIndex(ev, pointerId);
		if (activePointerIndex == -1) pointerId = INVALID_POINTER;
		if (pointerId == INVALID_POINTER) return;
		//
		float x = MotionEventCompat.getX(ev, activePointerIndex);
		float xDiff = Math.abs(x - lastX);
		float y = MotionEventCompat.getY(ev, activePointerIndex);
		float yDiff = Math.abs(y - lastY);
		boolean detected = xDiff > touchSlop && xDiff > yDiff;/* *2 */
		//
		Transition oldT = transition;
		if (detected) {
			lastX = x; lastY = y;
			left2right = x > initX;
			act = calcAction(left2right);
			if (act != null) requestManualTransition(panel.id, act, left2right);
			// check if new Transition started
			if (transition == null || transition == oldT || transition.isAutomatic) drop();
			else tz = transition;
		}
		else if (yDiff > touchSlop) drop();

		if (D)
			Wow.i(TAG, "detectDrag", "info :: xDiff = " + xDiff + ", yDiff = " + yDiff + ",  tooVerical ? " + (yDiff > touchSlop) + ", DETECTED = " + detected + ",  STARTED = " + (gest != null && detected ? "YES" : (transition == null ? "T=null" : transition == oldT ? "OLD" : transition.isAutomatic ? "AUTO" : "???")));

	}
	private void keepDragging(MotionEvent ev) {
		int activePointerIndex = MotionEventCompat.findPointerIndex(ev, pointerId);
		if (activePointerIndex == -1) pointerId = INVALID_POINTER;
		if (pointerId == INVALID_POINTER) return;
		float x = MotionEventCompat.getX(ev, activePointerIndex);
		float deltaX = lastX - x;
		float oldScrollX = tz.currX;// tz.scrollContent.getScrollX();
		float scrollX = oldScrollX + deltaX;
		lastX = x + (scrollX - (int) scrollX);// Don't lose the rounded component
		//
		int leftBound = tz.dirLeft2right ? tz.endX : tz.startX;
		int rightBound = tz.dirLeft2right ? tz.startX : tz.endX;
		//
		if (scrollX < leftBound) scrollX = leftBound;
		else if (scrollX > rightBound) scrollX = rightBound;

//		if (D) Wow.i(TAG, ">>>PPP keepDragging :: scrollX = " +scrollX+",  calcScroll = "+(oldScrollX + deltaX) );

		//
		tz.setScrollTo(scrollX, false);
	}
	private void finishDrag(final MotionEvent ev) {
		int velocity = getVelocity();
		int activePointerIndex = MotionEventCompat.findPointerIndex(ev, pointerId);
		if (activePointerIndex == -1) pointerId = INVALID_POINTER;
		int destScrollX = tz.startX;
		if (pointerId != INVALID_POINTER) {
			/** CALC DEST SCROLL */
			float x = MotionEventCompat.getX(ev, activePointerIndex);
			boolean okDist = Math.abs(x - initX) > flingDistance;
			boolean okVelocity = Math.abs(velocity) > minVelocity;
			boolean move2end = tz.dirLeft2right == velocity > 0;
			if (okDist && okVelocity)
				destScrollX = move2end ? tz.endX : tz.startX;
			else if (!okVelocity)
				destScrollX = tz.isCloseToEnd() ? tz.endX : tz.startX;

			if (D)
				Wow.i(TAG, "finishDrag", "valid = " + (okDist && okVelocity) + ",  dx = " + Math.abs(x - initX) + ",  flingDistance = " + flingDistance + ",  minVelos = " + minVelocity + ",  move2end = " + move2end);

		}
		tz.startScrollTo(destScrollX, velocity);
	}
	private void cancel() {
		tz.startScrollTo(tz.startX, 0);
	}
}





/** PANEL LAYOUT */
private class PanelLayout extends FrameLayout {

	PanelLayout(final Context context) {
		super(context);
	}
	private void setDim(boolean dim, boolean animate) {
		Drawable d = null;
		if (dim) {
			d = new ColorDrawable(0x20FFFFFF);
			tiker.cancelTik(Op.REMOVE_FOREGROUND);
			animate = false;
		}
		else if (animate) {
			if (dimRid > 0) d = UiHelper.drawable(dimRid);
			if (d != null && d instanceof AnimationDrawable) animate = true;
			else d = null;
		}
		//
		setForeground(d);
		//
		if (animate) {
			post(new Runnable() {
				@Override public void run() {
					Drawable d = getForeground();
					if (d != null && d instanceof AnimationDrawable) {
						((AnimationDrawable) d).start();
						tiker.setTik(Op.REMOVE_FOREGROUND, getId(), 400);
					}
				}
			});
		}
	}
}

private static int colorCount;
private static final int[] colors = new int[]{0xffFF0000, 0xff00FF00, 0xff0000FF, 0xff0FF0FF, 0xff000FFF, 0xffFFF000};
public static class FragmentStub extends Fragment implements iTransitionFragment {

	public FragmentStub() {super(); }

	@Override
//	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
//		return new View(context);
//	}
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Button v = new Button(getActivity());
//		v.setBackgroundResource(R.drawable.ic_launcher);
		if (colorCount >= colors.length) colorCount = 0;
		int color = colors[colorCount++];
		v.setBackgroundColor(color);
		v.setTextColor(0xFF000000);
		v.setText("PANEL " + Integer.toHexString(color));
		v.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(final View v) {
//				if (D) Wow.i(TAG, "onClick", "v = [" + v + "]");
//				mainPanel.startTransition(cfg.LP, ExtFragment.class, PanelManager.Action.PAGE, true, true);
//				mainPanel.setContent(cfg.RP, ExtFragment.class);
			}
		});
		return v;
	}
	@Override public void initContent(boolean firstInit) { }
	@Override public void removeContent() { }

}




public interface iTransitionFragment {
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state);// to assure it's Fragment
	public void onDestroyView();// to assure it's Fragment
	public void initContent(boolean firstInit);// only after transition finished > time to init content
	public void removeContent();// only if replace with new content > time uninit content
}


}
