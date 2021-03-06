package cyua.android.client;

import android.net.Uri;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cyua.android.core.CacheVar;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.EasyList;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiService;
import cyua.java.shared.objects.ConfigSh;

import static cyua.android.core.CacheVar.ArrayVar;
import static cyua.android.core.CacheVar.LongVar;
import static cyua.android.core.CacheVar.ObjectVar;
import static cyua.android.core.CacheVar.StringVar;


/**
 Created by Marvell on 1/28/14.
 */
public class Settings {

public static long msgExpireTimeout = 15*60*1000L;// after this time message expired and should be cleared
public static long msgWaitTime = 0;//TODO 1*60*1000L;// after this time new message can be send

// CACHE VARS
public static ArrayVar operators;
public static ArrayVar types;
public static StringVar uid;
public static StringVar user;
public static StringVar phone;
public static StringVar contacts;
public static MsgVar msg;
public static TaskVar task;
public static LongVar lastSend;
// AWS S3 Creds
public static String S3_KEY_ID;
public static String S3_KEY;
public static String S3_BUCKET;
public static String S3_REGION;
public static String S3_HOST;
public static String S3_DIR;


public static class MsgVar extends ObjectVar {
	public StringVar text;

}

public static class TaskVar extends ObjectVar {
	public BooleanVar active;
	public BooleanVar failed;
	public StringVar status;
}

public static void initCache() {
	CacheVar.initVars(Settings.class, null, null);
	// set Defaults
	if (uid.isEmpty()) uid.set(App.initDeviceUID());
	if (phone.isEmpty()) phone.set(App.getPhone());
//	operators.defolt(new EasyList<String>().plus("+380685263264"));
}

public static void clearMessage() {
	msg.remove();
	UiState state = (UiState) UiService.getUiState();
	state.clearPhotos();
	state.type = null;
	state.mapLat = state.mapLng = 0;
	state.region = state.city = state.address = null;
}

public static List<ConfigSh.Type> getTypes() {
	List<ConfigSh.Type> list = new ArrayList<ConfigSh.Type>();
	if (Tool.notEmpty(types.get())) {
		for (String str : types.get()) {
			ConfigSh.Type tp = new Gson().fromJson(str, ConfigSh.Type.class);
			if (tp != null && Tool.notEmpty(tp.name)) list.add(tp);
		}
		Collections.sort(list, new Comparator<ConfigSh.Type>() {
			@Override public int compare(ConfigSh.Type cur, ConfigSh.Type nxt) {
				if (cur.index == nxt.index) return 0;
				else if (cur.index > nxt.index) return 1;
				return -1;
			}
		});
	}
	return list;
}

}
