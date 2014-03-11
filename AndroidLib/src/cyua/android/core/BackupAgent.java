package cyua.android.core;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupManager;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import static cyua.android.core.AppCore.D; import cyua.android.core.log.Wow;

import java.io.IOException;
import java.util.Map;

import cyua.android.core.misc.Tool;


/** Created by far.be on 7/10/13. */
// WARNING : Backup Restore is starting before Activity.createRoot ie after AppCore.preInit
// WARNING : Glich ! if before Restore there was reference to SharedPrefs object it will not restored
// > so this is the workaround

public class BackupAgent extends BackupAgentHelper {
private static final String TAG = "BackupAgent";

public static void requestBackup() {
	BackupManager.dataChanged(AppCore.context().getPackageName());
}


/** INSTANCE */

private final String RESTORE_PREFS = "_backup";

@Override public void onCreate() {
	// if  init finished that means we Backingup else we Restoring
	SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, RESTORE_PREFS);
	addHelper("prefs", helper);
//	if (D) Wow.d("BACKUP", ">>>APPCORE BACKUP                                               createRoot ::   " );
}


@Override
public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
	copyPrefs(CacheVar.DEFAULT_PREFS, RESTORE_PREFS);
	super.onBackup(oldState, data, newState);
}

@Override
public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
	super.onRestore(data, appVersionCode, newState);
	copyPrefs(RESTORE_PREFS, CacheVar.DEFAULT_PREFS);
	CacheVar.reinit();
}

private void copyPrefs(String srcPrefsName, String destPrefsName) {
	Tool.setTimer();
	SharedPreferences srcPrefs = AppCore.context().getSharedPreferences(srcPrefsName, 0);
	SharedPreferences destPrefs = AppCore.context().getSharedPreferences(destPrefsName, 0);
	if (D) Wow.d(TAG, "copyPrefs", "BACKUP                                               copy Prefs..." + srcPrefs.getAll().size() + "    from " + srcPrefsName + "   to  " + destPrefsName);
	SharedPreferences.Editor editor = destPrefs.edit();
	for (Map.Entry<String, ?> entry : srcPrefs.getAll().entrySet()) {
		String key = entry.getKey();
		Object val = entry.getValue();
//		if (D) Wow.d("BACKUP", ">>>APPCORE BACKUP      restore >               key = "+key+",  val = "+val);
		if (val == null) continue;
		else if (val instanceof Float) editor.putFloat(key, (Float) val);
		else if (val instanceof Integer) editor.putInt(key, (Integer) val);
		else if (val instanceof Long) editor.putLong(key, (Long) val);
		else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
		else editor.putString(key, val.toString());
	}
	editor.commit();
//	if (D) Wow.d("BACKUP", ">>>APPCORE BACKUP                                               copy Prefs..  time = " + Tool.getTimer(true));
}

}