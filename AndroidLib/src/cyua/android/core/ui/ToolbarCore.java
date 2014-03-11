package cyua.android.core.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.R;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;



/** Created by far.be on 6/6/13. */
public class ToolbarCore {
private static final String TAG = ToolbarCore.class.getSimpleName();
private static final int LAY_RID = R.layout.toolbal_layout;


/** STATIC */

public static View space(int width) {
	Context cxt = AppCore.uiContext();
	Resources rs = cxt.getResources();
	DisplayMetrics metrics = rs.getDisplayMetrics();
	float scale = metrics.density;
	width = (int) (width * scale + .5f);
	//
	ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(width, 0);
	View v = new View(cxt);
	v.setLayoutParams(lp);
	return v;
}





/** INSTANCE */

private enum Op {ALIGN}

private Activity context;
private Tiker<Op> tiker;
private ViewGroup root;
private LinearLayout LLay, CLay, RLay;
private FrameLayout logoLay;
private TextView titleTv;
private HorizontalScrollView LScroller;


public ToolbarCore(Activity _context) {
	context = _context;
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op op, Object obj, Bundle data) {
			switch (op) {
				case ALIGN: align(); break;
			}
		}
	};
	createView();
}

public void onDestroy() {
	if (tiker != null) tiker.clear();
}

public View createView() {
	root = (ViewGroup) UiHelper.inflate(LAY_RID);
	if (root == null) return null;// TODO throw error
//	//
//	ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//	root.setLayoutParams(lp);
//	//
	logoLay = (FrameLayout) root.findViewById(R.id.logoLay);
	titleTv = (TextView) root.findViewById(R.id.titleTv);
	LScroller = (HorizontalScrollView) root.findViewById(R.id.LScroller);
	LLay = (LinearLayout) root.findViewById(R.id.LLay);
	RLay = (LinearLayout) root.findViewById(R.id.RLay);
	CLay = (LinearLayout) root.findViewById(R.id.CLay);
	LLay.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
		@Override public void onChildViewAdded(final View parent, final View child) { align(); }
		@Override public void onChildViewRemoved(final View parent, final View child) { }
	});
	return root;
}

public ViewGroup getRootView() {
	return root;
}

public void setBackground(int rid) {
	if (root != null) root.setBackgroundResource(rid);
}

/** LEFT PANEL */

public int leftIndexOf(View item) {
	for (int i = 0, num = LLay.getChildCount(); i < num; i++) {
		if (LLay.getChildAt(i) == item) return i;
	}
	return 0;
}
public ToolbarCore leftAdd(List<View> items) {
	if (Tool.notEmpty(items)) for (View item : items) leftAppend(item);
	return this;
}
public ToolbarCore leftPrepend(View item) {
	addLeftItem(item, 0);
	return this;
}
public ToolbarCore leftAppend(View item) {
	addLeftItem(item, LLay.getChildCount()-1);// last is titleTv
	return this;
}
public ToolbarCore leftReplace(View item, int index) {
	if (index > LLay.getChildCount()-1) index = LLay.getChildCount()-1;// last is titleTv
	View v = LLay.getChildAt(index);
	if (v != null) LLay.removeView(v);
	addLeftItem(item, index);
	return this;
}
public ToolbarCore leftRemove(int index) {
	if (index < LLay.getChildCount()-1) LLay.removeViewAt(index);// last is titleTv
	return this;
}
public ToolbarCore leftRemove(List<View> vList) {
	if (!Tool.isEmpty(vList)) for (View v : vList) LLay.removeView(v);
	return this;
}
public ToolbarCore leftClear() {
	for (int i = 0, num = LLay.getChildCount()-1; i < num; i++) {
		LLay.removeViewAt(0);// last is titleTv
	}
	return this;
}
private void addLeftItem(View item, int index) {
	LLay.addView(item, index);
	tiker.setTik(Op.ALIGN, 1000);
}


/** RIGHT PANEL */

public int rightIndexOf(View item) {
	for (int i = 0, num = RLay.getChildCount(); i < num; i++) {
		if (RLay.getChildAt(i) == item) return i;
	}
	return 0;
}
public ToolbarCore rightAdd(List<View> items) {
	if (Tool.notEmpty(items)) for (View item : items) rightAppend(item);
	return this;
}
public ToolbarCore rightPrepend(View item) {
	RLay.addView(item, 0);
	return this;
}
public ToolbarCore rightAppend(View item) {
	RLay.addView(item, RLay.getChildCount());
	return this;
}
public ToolbarCore rightReplace(View item, int index) {
	if (index > RLay.getChildCount()) index = RLay.getChildCount();
	View v = RLay.getChildAt(index);
	if (v != null) RLay.removeView(v);
	RLay.addView(item, index);
	return this;
}
public ToolbarCore rightRemove(int index) {
	RLay.removeViewAt(index);
	return this;
}
public ToolbarCore rightRemove(List<View> vList) {
	if (!Tool.isEmpty(vList)) for (View v : vList) RLay.removeView(v);
	return this;
}
public ToolbarCore rightClear() {
	RLay.removeAllViews();
	return this;
}


/** RIGHT PANEL */

public int centerIndexOf(View item) {
	for (int i = 0, num = CLay.getChildCount(); i < num; i++) {
		if (CLay.getChildAt(i) == item) return i;
	}
	return 0;
}
public ToolbarCore centerAdd(List<View> items) {
	if (Tool.notEmpty(items)) for (View item : items) centerAppend(item);
	return this;
}
public ToolbarCore centerPrepend(View item) {
	CLay.addView(item, 0);
	return this;
}
public ToolbarCore centerAppend(View item) {
	CLay.addView(item, CLay.getChildCount());
	return this;
}
public ToolbarCore centerReplace(View item, int index) {
	if (index > CLay.getChildCount()) index = CLay.getChildCount();
	View v = CLay.getChildAt(index);
	if (v != null) CLay.removeView(v);
	CLay.addView(item, index);
	return this;
}
public ToolbarCore centerRemove(int index) {
	CLay.removeViewAt(index);
	return this;
}
public ToolbarCore centerRemove(List<View> vList) {
	if (!Tool.isEmpty(vList)) for (View v : vList) CLay.removeView(v);
	return this;
}
public ToolbarCore centerClear() {
	CLay.removeAllViews();
	return this;
}


/** TITLE */

public ToolbarCore setTitle(int rid) {
	return setTitle(UiHelper.string(rid));
}
public ToolbarCore setTitle(CharSequence title) {
	if (title == null) {
		titleTv.setText("");
		titleTv.setVisibility(View.GONE);
	}
	else {
		titleTv.setVisibility(View.VISIBLE);
		titleTv.setText(title);
	}
	return this;
}



/** LOGO */

public ToolbarCore setLogo(View logo) {
	if (logoLay.getChildCount() > 0)
		for (int i = 0, num = logoLay.getChildCount(); i< num; i++) logoLay.removeViewAt(0);
	if (logo != null) logoLay.addView(logo);
	logoLay.setVisibility(logoLay.getChildCount() == 0 ? View.GONE : View.VISIBLE);
	return this;
}


/** LEFT SCROLLER */

public void align() {
	LScroller.smoothScrollTo(10000, 0);
}


}
