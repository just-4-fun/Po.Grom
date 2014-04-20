package cyua.android.client;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import cyua.android.core.inet.InetService;
import cyua.android.core.location.Fix;
import cyua.android.core.location.LocationService;
import cyua.android.core.misc.EasyList;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.HintNumField;
import cyua.android.core.ui.HintTextField;
import cyua.android.core.ui.ToolbarCore;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;
import cyua.java.shared.objects.MessageSh;

import static cyua.android.core.location.LocationService.hasGpsSignal;
import static cyua.android.core.location.LocationService.isGpsActive;
import static cyua.android.core.location.LocationService.isGpsAvailable;
import static cyua.android.core.ui.UiCore.UiAction;
import static cyua.android.core.ui.UiCore.UiCoreOp;
import static cyua.android.core.ui.UiCore.iFragment;


public class Fmt {
private static final String TAG = "Fmt";

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
			new UiAction(UiCoreOp.MAIN).execute();
	}
	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		return inflater.inflate(R.layout.page_initializing, container, false);
	}
}








public static class PageMain extends UiFragment {
	private static final String TAG = "PageMain";
	Ui ui;
	UiState uiState;
	enum Op {TIK,}
	Tiker<Op> tiker;
	ToolbarCore toolBar, titleBar;
	TextView infoTV;
	Button typeBtn, mapBtn;
	HintTextField messageET, userET, contactsET;
	HintNumField phoneET;
	boolean msgChanged = true;
	String smsInfo = "";
	Button startTrackBtn;
	Button sendBtn, clearBtn, okBtn, cancelTrackBtn, stopTrackBtn;
	Button openGps, openInet;
	ProgressBar progressBar;
	LinearLayout photoLay;
	PhotoPicker photo1, photo2, photo3, photo4;
	StringBuilder tmpInfo = new StringBuilder();

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		uiState = (UiState) UiService.getUiState();
		View root = inflater.inflate(R.layout.page_main, container, false);
		messageET = (HintTextField) root.findViewById(R.id.messageET);
		phoneET = (HintNumField) root.findViewById(R.id.phoneET);
		contactsET = (HintTextField) root.findViewById(R.id.contactsET);
		userET = (HintTextField) root.findViewById(R.id.userET);
		infoTV = (TextView) root.findViewById(R.id.infoTV);
		photoLay = (LinearLayout) root.findViewById(R.id.photoLay);
		typeBtn = (Button) root.findViewById(R.id.typeBtn);
		typeBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onTypeBtnClick();}
		});
		typeBtn.setText(Tool.isEmpty(uiState.type) ? UiHelper.string(R.string.btn_type) : uiState.type.name);
		mapBtn = (Button) root.findViewById(R.id.mapBtn);
		if (App.isPlayServiceAvailable()) {
			mapBtn.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {onOpenMapClick();}
			});
			mapBtn.setText(getAddressInfo());
		}
		else mapBtn.setVisibility(View.GONE);
		//
		phoneET.setAsHint(Settings.phone.get(), "0000000000");
		if (App.isPhoneReal()) phoneET.setEnabled(false);
		else phoneET.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		userET.setAsHint(Settings.user.get());
		contactsET.setAsHint(Settings.contacts.get());
		// init before listeners could touche them
		openGps = createIconButton(R.string.text_no_gps, R.drawable.warn_icon, R.color.red_a80);
		openGps.setVisibility(View.GONE);
		openGps.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {openGpsSettings(); }
		});
		openInet = createIconButton(R.string.text_no_inet, R.drawable.warn_icon, R.color.red_a80);
		openInet.setVisibility(View.GONE);
		openInet.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {openInetSettings(); }
		});
		//
		if (true || /*TODO */App.hasCamera()) initPhotoSlots(root);
		else photoLay.setVisibility(View.GONE);
		//
		return root;
	}
	@Override public void init(Ui _ui, UiState state, boolean firstInit) {
		ui = _ui;
		uiState = state;
		toolBar = ui.toolBar;
		titleBar = ui.titleBar;
		if (firstInit) messageET.setAsHint(Settings.msg.text.get());
		messageET.addTextChangedListener(new SimpleWatcher() {
			@Override public void onTextChanged(Editable s) { if (messageET.isFocused()) msgChanged = true; }
		});
		//
		tiker = new Tiker<Op>(TAG) {
			@Override public void handleTik(Op operation, Object obj, Bundle data) { setTik(Op.TIK, 1000); tik(); }
		};
		//
		requestTik();
	}
	private void initPhotoSlots(View root) {
		ImageButton btn = (ImageButton) root.findViewById(R.id.photo1Btn);
		photo1 = new PhotoPicker(btn, 0);
		btn = (ImageButton) root.findViewById(R.id.photo2Btn);
		photo2 = new PhotoPicker(btn, 1);
		btn = (ImageButton) root.findViewById(R.id.photo3Btn);
		photo3 = new PhotoPicker(btn, 2);
		btn = (ImageButton) root.findViewById(R.id.photo4Btn);
		photo4 = new PhotoPicker(btn, 3);
	}
	private void requestTik() {
		tiker.setTik(Op.TIK, 0);
	}
	private void tik() {
		// init toolbar
		boolean tracking = Tracker.isActive();
		boolean working = Tracker.isWorking();
		boolean canCancelTrack = tracking && !working;
		boolean active = SendTask.isActive();
		boolean failed = SendTask.isFailed();
		String status = SendTask.status();
		boolean hasStatus = Tool.notEmpty(status);
		boolean hadMsg = !Settings.msg.text.isEmpty();
		boolean hasMsg = messageET.hasValue();
		boolean showNoGps = isGpsAvailable() && !isGpsActive();
		Fix fix = LocationService.getLastAvailableFix();
		boolean noActualFix = fix == null || !fix.isActual(60000);
		boolean noGpsSignal = isGpsActive() && (!hasGpsSignal() || noActualFix);
		boolean showNoInet = !InetService.isOnline();
		long lastSend = Settings.lastSend.get();
		boolean justClear = hasStatus && lastSend > 0 && lastSend + Settings.msgExpireTimeout < Tool.now();
		boolean wait = lastSend > 0 && lastSend + Settings.msgWaitTime > Tool.now();
		boolean possibeSend = InetService.isOnline() || (App.hasTelephony() && App.canSMS());
		boolean canSend = possibeSend && !justClear && !wait && hasMsg;
		boolean hasPhotos = uiState.processing || uiState.hasPhotos();
		//
		show(openGps, showNoGps);
		show(openInet, showNoInet);
		//
		show(progressBar, !tracking && active);
		show(okBtn, !tracking && !active && hasStatus);
		show(sendBtn, !tracking && !active && !hasStatus && canSend);
		show(clearBtn, !tracking && !active && !hasStatus && hadMsg);
		show(startTrackBtn, !tracking && !active && !hasStatus);
		//
		show(cancelTrackBtn, canCancelTrack);
		show(stopTrackBtn, working);
		//
		String tmp = null;
		if (!canCancelTrack) addInfo(status);
		if (!canCancelTrack && !hasStatus) {
			if (!possibeSend) addInfo(R.string.taskinfo_global_fail);
			if (!hasMsg) addInfo(R.string.info_need_text);
			addInfo(validatePhone());
			if (showNoGps) addInfo(R.string.info_mk_gps);
			if (noGpsSignal) addInfo(R.string.info_nogpssignal);
			addInfo(calcSmsInfo());
			if (hasPhotos && showNoInet) addInfo(R.string.info_photo_noway);
			if (wait && !active && !hasStatus) addInfo(R.string.info_wait_send);
		}
		addInfo(Tracker.getStatus());
		String info = tmpInfo.toString();
		if (!Tool.safeEquals(info, infoTV.getText())) infoTV.setText(info);
		tmpInfo.delete(0, 100000);
//		if (D) Wow.v(TAG, "tik", "active ? "+active, "failed ? "+failed, "hasStatus ? "+hasStatus, "hasMsg ? "+hasMsg, "justClear ? "+justClear, "wait ? "+wait, "canSend ? "+canSend);
	}
	private void show(View v, boolean cond) {
		v.setVisibility(cond ? View.VISIBLE : View.GONE);
	}
	private void addInfo(String info) {
		if (Tool.notEmpty(info)) tmpInfo.append("- ").append(info).append("\n");
	}
	private void addInfo(int infoRid) {
		if (infoRid > 0) tmpInfo.append("- ").append(UiHelper.string(infoRid)).append("\n");
	}

	@Override public List<View> getToolbarCenters() {
		sendBtn = createToolButton(R.string.btn_send_message);
		sendBtn.setVisibility(View.GONE);
		sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onSendButtonClick(); }
		});
		clearBtn = createToolButton(R.string.btn_clear_message);
		clearBtn.setVisibility(View.GONE);
		clearBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onClearButtonClick();}
		});
		okBtn = createToolButton(R.string.btn_ok);
		okBtn.setVisibility(View.GONE);
		okBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onOkButtonClick(); }
		});
		cancelTrackBtn = createIconButton(R.string.btn_cancel_track, R.drawable.sos_icon, R.color.while_a80);
		cancelTrackBtn.setVisibility(View.GONE);
		cancelTrackBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onStopTrackButtonClick(); }
		});
		stopTrackBtn = createIconButton(R.string.btn_stop_track, R.drawable.sos_icon, R.color.while_a80);
		stopTrackBtn.setVisibility(View.GONE);
		stopTrackBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onStopTrackButtonClick(); }
		});
		progressBar = createProgressBar();
		progressBar.setVisibility(View.GONE);
		return new EasyList<View>().plus(clearBtn).plus(toolBar.space(10)).plus(sendBtn).plus(okBtn).plus(progressBar).plus(cancelTrackBtn).plus(stopTrackBtn);
	}
	@Override public List<View> getToolbarLefts() {
		startTrackBtn = createIconButton(R.string.btn_start_track, R.drawable.sos_icon, R.color.red_a80);
		startTrackBtn.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {onStartTrackButtonClick(); }
		});
		return new EasyList<View>().plus(startTrackBtn);
	}
	@Override public List<View> getTitlebarRights() {
		return new EasyList<View>().plus(openGps).plus(openInet);
	}
	private void openInetSettings() {
		Intent intent = null;
		if (InetService.isMobileAvailable()) {
			intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
			String setting = App.apiVersion >= 16 ? "MobileNetworkSettings" : "Settings";
			ComponentName cName = new ComponentName("com.android.phone", "com.android.phone." + setting);
			intent.setComponent(cName);
		}
		else {
			intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		App.context().startActivity(intent);
	}
	private void openGpsSettings() {
		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		App.context().startActivity(intent);
	}

	@Override public void onResume() {
		super.onResume();
		if (tiker != null) requestTik();
	}
	@Override public void onPause() {
		super.onPause();
		if (tiker != null) tiker.clear();
		saveUserSettings();
	}

	private String calcSmsInfo() {
		if (InetService.isOnline()) return "";
		else if (!App.hasTelephony()) return UiHelper.string(R.string.info_nowaytosend);
		else if (!msgChanged) return smsInfo;
		//
		String text = Tool.asString(messageET.getText());
		Tool.setTimer();
		String msg = MessageSh.toSms(012345, Settings.uid.get(), Settings.user.get(), Settings.phone.get(), text, 30.01234567891234, 30.01234567891234, true, 911);
		int[] res = SmsMessage.calculateLength(msg, false);
//				if (D) Wow.v(TAG, "calcSmsInfo", "Num = "+res[0], "unitsUsed = "+res[1], "unitsRemains = "+res[2], "codeSize = "+res[3]);
//		if (D) Wow.v(TAG, "calcSmsInfo", "calc sms time = "+Tool.getTimer(true), "text = "+msg);
		msgChanged = false;
		return smsInfo = UiHelper.string(R.string.info_calcsms, res[0]);
	}
	private String validatePhone() {
		if (!phoneET.hasValue()) return UiHelper.string(R.string.info_need_phone);
		else if (phoneET.getValue().length() < 10) return UiHelper.string(R.string.info_phone_bad);
		return null;
	}
	private void onSendButtonClick() {
		// TODO Validate input
		sendBtn.setVisibility(View.GONE);// avoid double click
		Settings.msg.text.set(messageET.getValue());
		saveUserSettings();
		new SendTask();
		requestTik();
	}
	private void onClearButtonClick() {
		clearMsg();
		Settings.lastSend.remove();
		requestTik();
	}
	private void onOkButtonClick() {
		if (!SendTask.isFailed()) clearMsg();
		Settings.task.remove();
		requestTik();
	}
	private void onStopTrackButtonClick() {
		Tracker.stop();
	}
	private void onStartTrackButtonClick() {
		Settings.msg.text.set(messageET.getValue());
		saveUserSettings();
		requestTik();
		Tracker.start();
	}
	private void onTypeBtnClick() {
		new TypeSelectorDialog().show();
	}
	private void onOpenMapClick() {
		Settings.msg.text.set(messageET.getValue());
		saveUserSettings();
		new UiAction(Ui.UiOp.OPEN_MAP).execute();
	}
	private void clearMsg() {
		Settings.clearMessage();
		messageET.setAsHint("");
		updatePage();
	}
	private void saveUserSettings() {
		if (!App.isPhoneReal()) Settings.phone.set(phoneET.getValue());
		Settings.user.set(userET.getValue());
		Settings.contacts.set(contactsET.getValue());
	}
	public void updatePage() {
		photo1.updateState(); photo2.updateState(); photo3.updateState(); photo4.updateState();
		typeBtn.setText(Tool.isEmpty(uiState.type) ? UiHelper.string(R.string.btn_type) : uiState.type.name);
		mapBtn.setText(getAddressInfo());
	}
	public void applyUserInput() {
		Settings.msg.text.set(messageET.getValue());
	}
	public String getAddressInfo() {
		String title = UiHelper.string(uiState.mapLat == 0 ? R.string.btn_location : R.string.btn_location_ok);
		if (Tool.notEmpty(uiState.region)) title = uiState.region + ", " + uiState.city + ", " + uiState.address;
		return title;
	}

}








/*
public static class PageTaskInfo extends UiFragment {
	private static final String TAG = "PageTaskInfo";
	enum Op {TIK,}
	Tiker<Op> tiker;
	Button okB;

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
		View root = inflater.inflate(R.layout.page_taskinfo, container, false);
		return root;
	}
	@Override public void init(Ui ui, UiState uiState) {
		tiker = new Tiker<Op>(TAG) {
			@Override public void handleTik(Op operation, Object obj, Bundle data) { onTik(); }
		};
		tiker.setTik(Op.TIK, 0);
	}
	@Override public List<View> getToolbarCenters() {
		okB = createToolButton(R.string.btn_ok);
		okB.setVisibility(View.GONE);
		okB.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {new UiAction(UiCoreOp.BACK).execute();}
		});
		return new EasyList<View>().plus(okB);
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
		View root = getView();
		TextView statusTV = (TextView) root.findViewById(R.id.statusTV);
		statusTV.setText(SendTask.status());
		//
		if (SendTask.isActive()) tiker.setTik(Op.TIK, 500);
		else {
			tiker.clear();
			ProgressBar bar = (ProgressBar) root.findViewById(R.id.progressBar);
			bar.setVisibility(View.GONE);
			okB.setVisibility(View.VISIBLE);
		}
	}
}
*/





/** SIMPLIFIED TEXT WATCHER */

static abstract class SimpleWatcher implements TextWatcher {
	boolean bySelf;
	@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	@Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
	@Override public void afterTextChanged(Editable s) {
		if (bySelf) return;
		bySelf = true;
		onTextChanged(s);
		bySelf = false;
	}
	abstract protected void onTextChanged(Editable s);
}




/** MISC */

static Button createToolButton(int titleRid) {
	return createIconButton(titleRid, R.drawable.button_icon, R.color.while_a80);
}
private static Button createIconButton(int titleRid, int iconRid, int colorRid) {
	Button btn = new Button(App.uiContext());
	btn.setBackgroundResource(iconRid);
	if (titleRid > 0) {
		btn.setTextColor(App.context().getResources().getColor(colorRid));
		btn.setTypeface(null, Typeface.BOLD);
		btn.setText(UiHelper.string(titleRid));
	}
	btn.setMinWidth((int) UiHelper.dimension(R.dimen.min_touchable_size));
	return btn;
}

private static ProgressBar createProgressBar() {
	ProgressBar bar = new ProgressBar(App.uiContext());
	bar.setIndeterminate(true);
	return bar;
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

