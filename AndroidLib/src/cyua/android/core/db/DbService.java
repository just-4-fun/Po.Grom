package cyua.android.core.db;

import cyua.android.core.AppCore;
import cyua.android.core.AppHelper;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.log.Wow;

import static cyua.android.core.AppCore.D;


public class DbService extends AppService
{
private static final String TAG = DbService.class.getSimpleName();
static final String DB_THREAD_NAME = "DB_Thread";
// SINGLETON
private static DbService I;



/*****   STATIC INIT   */

public static DbService instantiate(Class<? extends DbCore> helperCls)
{
	if (I != null) return I;
	I = new DbService();
	I.dbClass = helperCls;
	I.initOrder = AppService.INIT_FIRST;
	I.exitOrder = AppService.EXIT_LAST;
	return I;
}

public static boolean isInitialized()
{
	return I.isInited(AppCore.context());
}



/*****   INSTANCE    */

private Class<? extends DbCore> dbClass;
private DbCore dbHelper;
String name = "main";
int version = 1;




/*****   APPSERVICE Methods   */

@Override public void onInitStart(AppCore app)
{
	if (D) Wow.i(TAG, "onInitStart");
	try {
		dbHelper = AppHelper.getSelfCleaningSingleton(dbClass);
		dbHelper.dbService = this;
		dbHelper.onInit();
	} catch (Throwable ex) {initError = ex.getMessage(); Wow.e(ex);}
}
@Override public boolean isInited(AppCore app)
{
	return dbHelper.isInited();
}
@Override public String onInitFinish(AppCore app) throws Throwable
{
	if (D) Wow.i(TAG, "onInitFinish");
	if (initError == null) initError = dbHelper.getLastError();
	return super.onInitFinish(app);
}

@Override public void onExitStart(AppCore app)
{
	if (D) Wow.i(TAG, "onExitStart");
	if (dbHelper != null) dbHelper.onExit();
}
@Override public boolean isExited(AppCore app)
{
	return dbHelper == null || dbHelper.isExited();
}
@Override public void onExitFinish(AppCore app)
{
	if (D) Wow.i(TAG, "onExitFinish");
	if (dbHelper != null) dbHelper.exit();
	dbHelper = null;
}
@Override public String getStateInfo() throws Throwable {
	return "version = "+version;
}
}
