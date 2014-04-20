package cyua.android.core.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Sequence;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;
import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import static cyua.android.core.AppCore.D;



/** *   HELPER */

public abstract class DbCore extends SQLiteOpenHelper {
private static final String TAG = DbCore.class.getSimpleName();
private static DbCore I;
public static final String ID = "_id";
protected static final String dbName = "main";
protected static final int dbVersion = 1;
private enum Op {INIT, CLOSE_QUEUE, EXIT_TABLE, INIT_TABLE}





/*****   PUBLIC STATIC   */

@SuppressWarnings("unchecked")
public static <DBH extends DbCore> DBH getInstance(Class<DBH> thisClas) {
	return (DBH) I;
}
public static DbCore getInstance() {
	return I;
}



/*****   INSTANCE   */
protected DbService dbService;
protected SQLiteDatabase db;
protected Tiker<Op> dbTiker;
private boolean inited, exiting, finalOpDone;
protected HashMap<Class<? extends ObjectSh>, DbTable<?>> tableMap;
private String lastError;


public DbCore() {
	super(AppCore.context(), dbName, null, dbVersion);
	if (D) Wow.i(TAG, "DbCore");
	//
	HandlerThread dbThread = new HandlerThread(DbService.DB_THREAD_NAME);
	dbThread.start();
	//
	dbTiker = new Tiker<Op>(TAG, dbThread.getLooper()) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			switch (operation) {
				case INIT_TABLE: initTable((DbTable<?>) obj); break;
				case EXIT_TABLE: ((DbTable<?>) obj).onExit(); break;
				case INIT: init(); break;
				case CLOSE_QUEUE: doFinalOp(); break;
			}
		}
	};
}



/*****   INIT & EXIT   */

//NOTE is called in main thread
final void onInit() {
	dbTiker.setTik(Op.INIT, 0);
}
boolean isInited() {return inited;}
/*NOTE is called in Db Thread. All calls must be made directly on db not via tiker */
final void init() {
	if (D) Wow.i(TAG, "init");
	lastError = null;
	try {
		tableMap = new HashMap<Class<? extends ObjectSh>, DbTable<?>>();
		db = getWritableDatabase();
	} catch (Throwable ex) {
		lastError = ex.getMessage();
		if (D) Wow.w(TAG, "init", lastError);
	} finally {inited = true;}
	//
	onInited();
}
@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
@Override public void onCreate(SQLiteDatabase db) {}
protected void onInited() {} // to Override
public static String getLastError() {return I != null ? I.lastError : null;}


// NOTE is called in main thread
final void onExit() {
	dbTiker.cancelTik(Op.INIT);
	//
	for (DbTable<?> table : tableMap.values())
		try {table.onExit();} catch (Exception ex) {Wow.e(ex);}
}
boolean isExited() {
	// WARNING: all db operations must be called before that point as it's last final op in db queue
	if (!exiting) {
		exiting = true;
		dbTiker.setTik(Op.CLOSE_QUEUE, 0);// WARNING it's last event in dbMessageQuue
	}
	return finalOpDone;
}
private void doFinalOp() {
	finalOpDone = true;
}
/*NOTE is called in Db thread. All calls must be made directly on db not via tiker */
final void exit() {
	if (D) Wow.i(TAG, "exit");
	try {
		if (dbTiker != null) {
			dbTiker.clear();
			dbTiker.getLooper().quit();
		}
		//
	} finally {
		if (db != null && db.isOpen()) close();
//		exited = true;
//		dbTiker = null;
		// mainTiker = null;
//		db = null;
	}
}



/** TABLE INIT */

/** Table goes through 2 steps: 1. Creation, and 2. Initialization. It can be used only after init. So check if it's inited. */
@SuppressWarnings("unchecked")
public static <T extends ObjectSh> DbTable<T> getTable(Class<T> objClass) {
	DbTable<T> table = (DbTable<T>) I.tableMap.get(objClass);
	//
//	if (table == null) Wow.w("DbCore", "getTable", "Table of "+objClass.getSimpleName()+" should be registered before usage.");
	if (table == null) Wow.e(new Exception("Table of "+objClass.getSimpleName()+" should be registered before usage."));
		try {
		Class<? extends DbTable<T>> tabClas = (Class<? extends DbTable<T>>) Tool.getGenericParamClass(objClass, ObjectSh.class, Object.class);
		table = (DbTable<T>) registerTable(tabClas);
	} catch (Exception ex) {Wow.e(ex);}
	return table;
}

/** NOTE can be called in Main thread*/
public static DbTable<?> registerTable(Class<? extends DbTable<?>> tabClas) {
	for (DbTable<?> table : I.tableMap.values()) if (table.getClass() == tabClas) return table;
	//
	try {
		DbTable<?> table = tabClas.newInstance();
		I.tableMap.put(table.objectClass, table);
		I.initTable(table);
		return table;
	} catch (Exception ex) {Wow.e(ex);}
	return null;
}
/**NOTE executed in DB Thread*/
private void initTable(DbTable<?> table) {
	if (!isDbThread()) dbTiker.setTik(Op.INIT_TABLE, table, 0);
	else table.onInit(this);
}
/** NOTE can be called in Main thread*/
public static void unregisterTable(Class<? extends ObjectSh> objClass) {
	DbTable<?> table = I.tableMap.get(objClass);
	if (table == null) return;
	//
	I.tableMap.remove(objClass);
	if (!I.isDbThread()) I.dbTiker.setTik(Op.EXIT_TABLE, table, 0);
	else try {table.onExit();} catch (Exception ex) {Wow.e(ex);}
}








/*****   OPERATIONS   */

public boolean execSql(String sql) {
	if (D) Wow.i(TAG, "execSql", sql);
	lastError = null;
	try {
		db.execSQL(sql);
		return true;
	} catch (Throwable e) {
		lastError = Tool.stackTrace(e);
		if (D) Wow.w(TAG, "execSql", lastError);
		return false;
	}
}

public Cursor select(String sql, String[] whereArgs) {
	if (D) Wow.i(TAG, "select", sql);
	lastError = null;
	try {return db.rawQuery(sql, whereArgs);} catch (Throwable e) {
		lastError = Tool.stackTrace(e);
		if (D) Wow.w(TAG, "select", lastError);
		return null;
	}
}
public Cursor select(String table, String[] columns, String where, String[] whereArgs, String groupBy, String having, String orderBy, String limit, boolean distinct) {
	String sql = SQLiteQueryBuilder.buildQueryString(distinct, table, columns, where, groupBy, having, orderBy, limit);
	return select(sql, whereArgs);
}

public long insert(String table, ContentValues values) {
	if (D) Wow.i(TAG, "insert", "Tab:" + table + ",  values:" + Tool.printObject(values));
	lastError = null;
	try {return db.insert(table, null, values);} catch (Throwable e) {
		lastError = Tool.stackTrace(e);
		if (D) Wow.w(TAG, "insert", lastError);
		return -1;
	}
}

public int update(String table, ContentValues values, String where, String[] whereArgs) {
	if (D) Wow.i(TAG, "update", "Tab:" + table + ",  where:" + where + ",  values:" + Tool.printObject(values));
	lastError = null;
	try {return db.update(table, values, where, whereArgs);} catch (Throwable e) {
		lastError = Tool.stackTrace(e);
		if (D) Wow.w(TAG, "update", lastError);
		return -1;
	}
}
public int updateById(String table, ContentValues values, Object id) {
	return update(table, values, ID + "=" + id, null);
}
public int updateByColumn(String table, ContentValues values, String pty, Object value) {
	return update(table, values, pty + "=?", new String[]{value.toString()});
}

public int delete(String table, String where, String[] whereArgs) {
	if (D) Wow.i(TAG, "delete", "Tab:" + table + ",  where:" + where);
	lastError = null;
	try {return db.delete(table, where, whereArgs);} catch (Throwable e) {
		lastError = Tool.stackTrace(e);
		if (D) Wow.w(TAG, "delete", lastError);
		return -1;
	}
}
public int deleteById(String table, Object id) {
	return delete(table, ID + "=" + id, null);
}

public boolean dropTable(String tableName) {
	String q = "DROP TABLE IF EXISTS " + tableName;
	return execSql(q);
}



/*****   DB UTILS   */

public boolean isDbThread() {
	return dbTiker.getLooper().getThread().equals(Thread.currentThread());
}


public void attachDbFromAssets(String dbFileName, boolean forceUpdate) {
	// or from sdcard
	// File dbFile = new File(Environment.getExternalStorageDirectory(), fileName);
	try {
		String mainDbPath = db.getPath();
		String dbPath = mainDbPath.substring(0, mainDbPath.lastIndexOf(File.separatorChar) + 1) + dbFileName;
		File dbFile = new File(dbPath);
		// copy db from assets
		if (!dbFile.exists() || forceUpdate) {
			InputStream is = null; OutputStream os = null;
			try {
				is = AppCore.context().getAssets().open(dbFileName);
				os = new FileOutputStream(dbPath);
				//transfer bytes from the inputfile to the outputfile
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
				os.flush();
			} finally {
				//Close the streams
				if (os != null) os.close();
				if (is != null) is.close();
			}
		}
		// attach db
		String dbName = dbFileName.substring(0, dbFileName.indexOf('.'));
		db.execSQL("ATTACH DATABASE '" + dbPath + "' AS " + dbName);
		// test
//		Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM streets", null);
//		int count = 0;
//		if (cursor.moveToFirst()) count = cursor.getInt(0);
	} catch (Exception ex) {Wow.e(ex);}
}
public void attachDb(File dbFile) {
	try {
		String dbName = dbFile.getName();
		int ix = dbName.indexOf('.');
		if (ix > 0) dbName = dbName.substring(0, ix);
		db.execSQL("ATTACH DATABASE '" + dbFile.getAbsolutePath() + "' AS " + dbName);
		// test
//		Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM streets", null);
//		int count = 0;
//		if (cursor.moveToFirst()) count = cursor.getInt(0);
	} catch (Exception ex) {Wow.e(ex);}
}
public void detachDb(File dbFile) {
	try {
		String dbName = dbFile.getName();
		int ix = dbName.indexOf('.');
		if (ix > 0) dbName = dbName.substring(0, ix);
		db.execSQL("DETACH DATABASE " + dbName);
	} catch (Exception ex) {Wow.e(ex);}
}
public void backupDb(String fileName) {
	try {
		if (Environment.getExternalStorageDirectory().canWrite()) {
			String bkPath = Environment.getExternalStorageDirectory().getPath();
			//
			File currentDB = new File(db.getPath());
			File backupDB = new File(bkPath, fileName);
			//
			if (currentDB.exists()) {
				FileChannel src = null, dst = null;
				try {
					src = new FileInputStream(currentDB).getChannel();
					dst = new FileOutputStream(backupDB).getChannel();
					dst.transferFrom(src, 0, src.size());
				} catch (Exception ex) { Wow.w(TAG, "backupDb", "Error: " + Tool.stackTrace(ex));} finally {
					if (src != null) src.close();
					if (dst != null) dst.close();
				}
			}
		}
	} catch (Exception e) {if (D) Wow.w(TAG, Tool.stackTrace(e));}
}






/**  ASYNC DB OPERATIONS */

public static abstract class DbOperation extends DbSequence {
	Class<? extends ObjectSh> objClass;
	DbTable<? extends ObjectSh> table;

	@SuppressWarnings("unchecked")
	private <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void assignTable() {
		Class<Obj> objClas = (Class<Obj>) objClass;
		table = DbCore.getTable(objClas);
	}
	@Override protected void callDoInBackground() {
		assignTable();
		super.callDoInBackground();
	}
	@Override protected void doInBackground() {
		doOperation();
	}
	abstract protected <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void doOperation();
}


public static abstract class Select<O extends ObjectSh> extends DbOperation {
	String where, orderBy, tableName;
	int limit;
	public List<O> list;
	public Select(String _tableName, String _where, String _orderBy, int _limit) {
		this(_where, _orderBy, _limit);
		tableName = _tableName;
	}
	@SuppressWarnings("unchecked")
	public Select(String _where, String _orderBy, int _limit) {
		try {
			objClass = (Class<O>) Tool.getGenericParamClass(getClass(), Select.class, ObjectSh.class);
		} catch (Exception ex) { Wow.e(ex);}
		where = _where; orderBy = _orderBy; limit = _limit;
	}
	@SuppressWarnings("unchecked")
	@Override protected <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void doOperation() {
		if (tableName == null) tableName = table.tableName();
		list = (List<O>) table.select(tableName, where, orderBy, limit);
	}
	@Override protected void onSuccess() {
		onSelect(list);
	}
	public abstract void onSelect(List<O> list);
}



public static class Insert extends DbOperation {
	ObjectSh object;
	List<? extends ObjectSh> objects;
	public Insert(List<? extends ObjectSh> _objects) {
		objects = _objects;
		if (Tool.notEmpty(objects)) {
			ObjectSh obj = objects.get(0);
			objClass = (Class<? extends ObjectSh>) obj.getClass();
		}
	}
	@SuppressWarnings("unchecked")
	public Insert(ObjectSh _object) {
		objClass = (Class<? extends ObjectSh>) _object.getClass();
		object = _object;
	}
	@SuppressWarnings("unchecked")
	@Override protected <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void doOperation() {
		Tab tab = (Tab) table;
		if (objects != null) {
			for (ObjectSh obj : objects) tab.insert((Obj) obj);
		}
		else if (object != null) tab.insert((Obj) object);
	}
}



public static class Update extends DbOperation {
	ObjectSh object;
	Column[] columns;
	@SuppressWarnings("unchecked")
	public Update(ObjectSh _object, Column... column) {
		objClass = (Class<? extends ObjectSh>) _object.getClass();
		object = _object;
		columns = column;
	}
	@SuppressWarnings("unchecked")
	@Override protected <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void doOperation() {
		if (Tool.isEmpty(columns)) ((Tab) table).update((Obj) object);
		else ((Tab) table).update((Obj) object, columns);
	}
}



public static class Replace extends DbOperation {
	ObjectSh object;
	@SuppressWarnings("unchecked")
	public Replace(ObjectSh _object) {
		objClass = (Class<? extends ObjectSh>) _object.getClass();
		object = _object;
	}
	@SuppressWarnings("unchecked")
	@Override protected <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void doOperation() {
		((Tab) table).replace((Obj) object);
	}
}



public static class Delete extends DbOperation {
	ObjectSh object;
	@SuppressWarnings("unchecked")
	public Delete(ObjectSh _object) {
		objClass = (Class<? extends ObjectSh>) _object.getClass();
		object = _object;
	}
	@SuppressWarnings("unchecked")
	@Override protected <Obj extends ObjectSh, Tab extends DbTable<Obj>>
	void doOperation() {
		((Tab) table).delete((Obj) object);
	}
}








/*****   INTER THREAD CALLBACK INTERFACE  */

public static abstract class DbSequence extends Sequence {
	protected long startedMs = Tool.deviceNow();
	public DbSequence() {
		super(I.dbTiker.getLooper());
	}
	@Override protected boolean start() {
		if (I.inited) return super.start();
		else if (Tool.deviceNow() - startedMs < 5000) requestStart();
		return false;
	}
}

}