package cyua.android.smsserver;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import cyua.android.core.misc.EasyList;
import cyua.android.core.misc.Tiker;
import cyua.android.core.ui.ToolbarCore;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;

import static cyua.android.core.ui.UiCore.iFragment;


public class Fmt {

public static class SidePanel extends UiFragment {
	public SidePanel() { super(); }
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		return inflater.inflate(R.layout.side_panel, container, false);
	}
}




public static class PageInitializing extends UiFragment {

	@Override public void init(Ui ui, UiState state, boolean firstInit) {
		// this could be if app closed but KeepingAlive (not exited yet) and than launched again (being inited)
		if (App.isInitFinished() && !App.isInitFailed() && !App.isExitStarted())
			new UiCore.UiAction(UiCore.UiCoreOp.MAIN).execute();
	}
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		return inflater.inflate(R.layout.page_initializing, container, false);
	}
}





public static class PageMain extends UiFragment {
	private static final String TAG = "PageMain";
	enum Op {TIK,}
	Tiker<Op> tiker;
	TextView sentCountTv, pendCountTv;

	@Override public List<View> getToolbarCenters() {
		Button stopB = createToolButton(R.string.btn_stop_message);
		stopB.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {new UiCore.UiAction(Ui.UiOp.STOP).execute();}
		});
		return new EasyList<View>().plus(stopB);
	}
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		View root = inflater.inflate(R.layout.page_main, container, false);
		sentCountTv = (TextView) root.findViewById(R.id.sentCountTv);
		pendCountTv = (TextView) root.findViewById(R.id.pendCountTv);
		tiker = new Tiker<Op>(TAG) {
			@Override public void handleTik(Op operation, Object obj, Bundle data) { onTik(); }
		};
		return root;
	}
	@Override public void onPause() {
		super.onPause();
		if (tiker != null) tiker.clear();
	}
	@Override public void onResume() {
		super.onResume();
		if (tiker != null) tiker.setTik(Op.TIK, 0);
	}
	private void onTik() {
		sentCountTv.setText(String.valueOf(Settings.getSendCounter()));
		pendCountTv.setText(String.valueOf(Settings.getPendCounter()));
		tiker.setTik(Op.TIK, 2000);
	}

}







/** MISC */

private static Button createToolButton(int titleRid) {
	Button btn = new Button(App.uiContext());
	btn.setBackgroundResource(R.drawable.bg_underline);
	btn.setTextColor(App.context().getResources().getColor(R.color.while_a80));
	btn.setTypeface(null, Typeface.BOLD);
	btn.setText(UiHelper.string(titleRid));
	btn.setMinWidth((int) UiHelper.dimension(R.dimen.min_touchable_size));
	return btn;
}



public static class UiFragment extends Fragment implements iFragment<Ui, UiState> {
	List<View> toolbarLefts, toolbarRights, toolbarCenters, titlebarLefts, titlebarRights, titlebarCenters;
	@Override public void init(Ui ui, UiState uiState, boolean firstInit) { }
	@Override public List<View> getToolbarLefts() { return null; }
	@Override public List<View> getToolbarRights() { return null; }
	@Override public List<View> getToolbarCenters() { return null; }
	@Override public List<View> getTitlebarLefts() { return null; }
	@Override public List<View> getTitlebarRights() { return null; }
	@Override public List<View> getTitlebarCenters() { return null; }
	@Override public void initContent(boolean firstInit) {
//		if (D) Wow.v(TAG, "initContent", this.getClass().getSimpleName());
		Ui ui = (Ui) UiService.getUi();
		toolbarLefts = getToolbarLefts();
		toolbarRights = getToolbarRights();
		toolbarCenters = getToolbarCenters();
		titlebarLefts = getTitlebarLefts();
		titlebarRights = getTitlebarRights();
		titlebarCenters = getTitlebarCenters();
		ToolbarCore titleBar = ui.titleBar;
		ToolbarCore toolBar = ui.toolBar;
		toolBar.leftAdd(toolbarLefts).rightAdd(toolbarRights).centerAdd(toolbarCenters);
		titleBar.leftAdd(titlebarLefts).rightAdd(titlebarRights).centerAdd(titlebarCenters);
		init(ui, (UiState) UiService.getUiState(), firstInit);
	}
	@Override public void removeContent() {
//		if (D) Wow.v(TAG, "removeContent", this.getClass().getSimpleName());
		Ui ui = (Ui) UiService.getUi();
		ToolbarCore toolBar = ui.toolBar;
		ToolbarCore titleBar = ui.titleBar;
		toolBar.leftRemove(toolbarLefts).rightRemove(toolbarRights).centerRemove(toolbarCenters);
		titleBar.leftRemove(titlebarLefts).rightRemove(titlebarRights).centerRemove(titlebarCenters);
	}
}

}
