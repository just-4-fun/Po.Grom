package cyua.android.core.log;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.misc.CachedFileProvider;
import cyua.android.core.misc.Timer;
import cyua.android.core.misc.Tool;
import cyua.java.shared.ObjectSh;
import cyua.java.shared.objects.LogRecordSh;

import static cyua.android.core.AppCore.D;



public class Wow {
private static final String TAG = "Wow";
private static final int MAX_SIZE = 12;
private static final LinkedList<LogRecordSh> revolver = new LinkedList<LogRecordSh>();
private static boolean syncRequired;
public static String lastError;



public static void v(String clas, String method, String... details) {
	Log.v(clas, compile(clas, method, details));
}
public static void i(String clas, String method, String... details) {
	Log.i(clas, compile(clas, method, details));
}
public static void d(String clas, String method, String... details) {
	Log.d(clas, compile(clas, method, details));
}
public static void w(String clas, String method, String... details) {
	Log.w(clas, compile(clas, method, details));
}
private static String compile(String clas, String method, String... details) {
	return ">>> [ " + clas + " ] :: [ " + method + (details == null ? " ]" : " ] :: " + Tool.join(details, "; "));
}

public static void e(String msg) { e(new Exception(msg)); }
public static void e(Throwable ex, String... details) {
	// [file] [clas] [meth] [line]\n[exept] [msg]\ndetails...\nstack
	String file = AppCore.packageRoot;
	String clas = null, meth = "", line = "", except = "", msg = null;
	StringBuilder text = new StringBuilder();
	LogRecordSh rec = null;
	try {
		if (details != null) text.append(Tool.join(details, "; ")).append("\n");
		while (ex != null) {
			except = ex.getClass().getName();
			msg = ex.toString();
			if (Tool.isEmpty(msg)) msg = except + ";  " + ex.getMessage();
			boolean prev = false, first = true;
			for (StackTraceElement elt : ex.getStackTrace()) {
				String pack = elt.getClassName();
				boolean curr = pack.startsWith(AppCore.packageRoot);
				if (!curr && prev) break;// all trace up to current package
				prev = curr;
				if (curr && first) {
					first = false;
					file = elt.getFileName();
					int dotIx = file.lastIndexOf('.');
					if (dotIx > 0) file = file.substring(0, dotIx);
					clas = elt.getClassName();
					meth = elt.getMethodName();
					line = String.valueOf(elt.getLineNumber());
				}
				text.append(curr ? "!" : "").append("    ").append(elt).append("\n");
			}
			ex = ex.getCause();
			if (ex != null) text.append("\nCAUSED BY:\n");
		}
		//
		if (msg != null) text.insert(0, msg + "\n");
		String place = null;
		if (clas != null)
			place = "[ " + file + " ] :: [ " + clas + " ] :: [ " + meth + " ] :: [ " + line + " ]";
		//
		rec = newLogRecord(place, except, text.toString());
		//
		if (place != null) text.insert(0, place + "\n");
	} catch (Throwable exx) {
		text.append("Exception in logger e. " + Tool.stackTrace(exx));
		rec = newLogRecord("logger", exx.toString(), text.toString());
	}
	Log.e(file, ">>>> " + text);
	lastError = file + "\n" + text;
	//
	if (!syncRequired) revolve(rec);
	else synchronized (revolver) { revolve(rec); }
//	if (D) Wow.i(TAG, "e", "size " + revolver.size());
}
private static void revolve(LogRecordSh rec) {
	revolver.add(rec);
	if (revolver.size() > MAX_SIZE) revolver.removeFirst();
}

private static LogRecordSh newLogRecord(String place, String except, String msg) {
	LogRecordSh r = new LogRecordSh();
	r.place = place;
	r.exception = except;
	r.message = msg;
	r.datetime = Tool.now();
	String[] ymdt = Tool.YMDT(r.datetime);
	r.month = ymdt[1];
	r.date = ymdt[2];
	r.time = ymdt[3] + ":" + ymdt[4] + ":" + ymdt[5];
	r.device = AppCore.deviceUidVar.get();
	r.deviceInfo = AppCore.deviceInfo;
	r.appVersion = AppCore.version + "";
	r.apiVersion = AppCore.apiVersion + "";
	try { r.appState = AppCore.getAppState(); } catch (Exception ex) {r.appState = "Oops.. " + ex.toString();}
	return r;
}






/** PERSISTING */

public static List<LogRecordSh> getCurrent() {
	List<LogRecordSh> list = null;
	syncRequired = true;
	synchronized (revolver) { list = (List<LogRecordSh>) revolver.clone(); }
	syncRequired = false;
	return list;
}

public static void clearCurrent() {
	if (revolver == null) return;
	syncRequired = true;
	synchronized (revolver) { revolver.clear(); }
	syncRequired = false;
}

public static void writeCache() {
	List<LogRecordSh> list = null;
	Timer timer = new Timer();
	try {
		list = getCurrent();
		clearCurrent();
		Type type = new TypeToken<List<LogRecordSh>>() {}.getType();
		String logs = list.isEmpty() ? "" : new Gson().toJson(list, type);
		FileOutputStream fout = AppCore.context().openFileOutput("elog", 0);
		fout.write(logs.getBytes(Tool.UTF8));
		fout.close();
	} catch (Exception ex) {Log.e(TAG, ">>>LOH writeCache :: " + ex.getClass().getName() + ", " + ex.getMessage());}
	if (D)
		Wow.i(TAG, "writeCache", "Time = " + timer.getSpan(false) + ",  logSize = " + (list == null ? "null" : list.size()));
}
public static List<LogRecordSh> readCache() {
	List<LogRecordSh> list = null;
	Timer timer = new Timer();
	int size = 0;
	try {
		FileInputStream fin = AppCore.context().openFileInput("elog");
		FileChannel fileChannel = fin.getChannel();
		size = (int) fileChannel.size();
		if (size > 0) {
			byte[] bytes = new byte[size];
			fin.read(bytes);
			String logs = new String(bytes, Tool.UTF8);
			Type type = new TypeToken<List<LogRecordSh>>() {}.getType();
			list = new Gson().fromJson(logs, type);
		}
		fileChannel.close();
		fin.close();
	} catch (Exception ex) {Log.e(TAG, ">>>LOH readCache :: " + ex.getClass().getName() + ", " + ex.getMessage());}
	if (D)
		Wow.i(TAG, "readCache", "Time = " + timer.getSpan(false) + ",  fileSize = " + size + ",  logSize = " + (list == null ? "null" : list.size()));
	return list;
}
public static Uri getCacheFile() {
	FileOutputStream fout = null;
	List<LogRecordSh> list = getCurrent();
	if (list.isEmpty()) return null;
	try {
//		DbCore.registerTable(LogsTable.class);
//		DbTable<LogRecordSh> logTab = DbCore.getTable(LogRecordSh.class);
		String logs = ObjectSh.getSchema(LogRecordSh.class).toCSV(list);
		if (Tool.isEmpty(logs)) return null;
//		File tempDir = AppCore.context().getExternalCacheDir();
		File tempDir = AppCore.context().getCacheDir();
		File tempFile = File.createTempFile("errorlog", ".txt", tempDir);
		fout = new FileOutputStream(tempFile);
//		fout = AppCore.context().openFileOutput("errorlog.txt", Context.MODE_WORLD_READABLE);
		fout.write(logs.getBytes(Tool.UTF8));
		fout.close();
//		return Uri.fromFile(new File("/mnt/sdcard/../.."+AppCore.context().getFilesDir()+"/"+"errorlog.txt"));
//		return Uri.fromFile(tempFile);
		return Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + tempFile.getName());
	} catch (Exception ex) {Wow.e(ex);}
	return null;
}






/** LOGS TABLE */

/*
public static class LogsTable extends DbTable<LogRecordSh> {

	public LogsTable() throws Exception {super();}

	@Override public String tableName() { return "logs"; }

}
*/

}
