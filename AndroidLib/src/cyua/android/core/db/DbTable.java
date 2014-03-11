package cyua.android.core.db;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import cyua.java.shared.Column;
import cyua.java.shared.Column.FtType;
import cyua.java.shared.Column.SqlType;
import cyua.java.shared.ObjectSh;
import cyua.java.shared.Column.IdColumn;

import android.content.ContentValues;
import android.database.Cursor;
import static cyua.android.core.AppCore.D;

import android.util.SparseArray;



/** WARN: each table subclass must be registered with DbCore.registerTable(tabClas). */

public abstract class DbTable<DbObject extends ObjectSh> {
private static final String TAG = DbTable.class.getSimpleName();


protected DbCore db;
public Class<DbObject> objectClass;
public List<Column> columns, sqlColumns;
public IdColumn idColumn;
protected HashMap<String, List<Column>> indexMap;
protected boolean inited;

public abstract String tableName();

public boolean isInited() {
	return inited;
}

@SuppressWarnings("unchecked")
public DbTable() throws Exception {
	objectClass = (Class<DbObject>) Tool.getGenericParamClass(getClass(), DbTable.class, Object.class);
	ObjectSh.Schema<DbObject> schema = ObjectSh.getSchema(objectClass);
	columns = schema.columns;
	sqlColumns = schema.sqlColumns;
	idColumn = schema.idColumn;
	indexMap = schema.indexMap;
}


protected void onInit(DbCore _db) {
	db = _db;
	Cursor infoCursor = null, indexCursor = null;
	try {
		// get OLD SCHEMA
		infoCursor = db.select("PRAGMA table_info(" + tableName() + ")", null);
		if (infoCursor == null) throw new Exception("Can't load tables info. " + db.getLastError());
		if (infoCursor.getCount() == 0) onCreate();
		else {
			indexCursor = db.select("PRAGMA index_list(" + tableName() + ")", null);
			if (indexCursor == null) throw new Exception("Can't load index info. Error=" + db.getLastError());
			onUpgrade(infoCursor, infoCursor);
		}
		onOpen();
		//
		inited = true;
	} catch (Exception ex) {Wow.e(ex);} finally {
		if (infoCursor != null) infoCursor.close();
		if (indexCursor != null) indexCursor.close();
		indexMap = null;
	}
}
protected void onCreate() throws Exception {
	if (D) Wow.i(TAG, "createRoot", tableName());
	StringBuilder sql = new StringBuilder();
	for (Column col : sqlColumns) {
		sql.append(sql.length() > 0 ? "," : "").append(col.name).append(" ").append(col.type);
		if (col.constraint != null) sql.append(" " + col.constraint);
	}
	//
	sql.insert(0, "CREATE TABLE IF NOT EXISTS " + tableName() + " (").append(");");
	//if (D) Wow.v(TAG, "[generateCreate]: table:"+table+" SQL:"+expr.toString());
	if (!db.execSql(sql.toString()))
		throw new Exception("Can't Create table. SQL=" + sql + ". Error=" + db.getLastError());
	// create INDEXES if any
	for (Entry<String, List<Column>> entry : indexMap.entrySet()) {
		String q = "CREATE INDEX IF NOT EXISTS " + entry.getKey() + " ON " + tableName() + " (" + Tool.join(entry.getValue(), ",") + ")";
		if (!db.execSql(q)) throw new Exception("Can't Create indexes. SQL=" + q + ". Error=" + db.getLastError());
	}
}
protected void onUpgrade(Cursor infoCursor, Cursor indexCursor) throws Exception {
	if (D) Wow.i(TAG, "onUpgrade", tableName());
	// Creates new table and Copies data from old table's matched columns
	// if only new columns added and/or indexes changed > no drop table will happen
	// get OLD SCHEMA
	int cursorNameIx = indexCursor.getColumnIndex("name");
	int nameIx = infoCursor.getColumnIndex("name");
	int typeIx = infoCursor.getColumnIndex("type");
	boolean recreate = false;
	ArrayList<Column> newCols = new ArrayList<Column>();
	for (Column col : sqlColumns) newCols.add(col);
	StringBuilder newColsStr = new StringBuilder(), oldColsStr = new StringBuilder();
	// COMPARE OLD AND NEW SCHEMAS
	while (infoCursor.moveToNext()) {
		String oldName = infoCursor.getString(nameIx);
		String oldType = infoCursor.getString(typeIx);
		Column matchCol = null;
		// find matching new column
		ListIterator<Column> newIterator = newCols.listIterator();
		while (newIterator.hasNext()) {
			Column newCol = newIterator.next();
			if (newCol.name.equals(oldName)) ;
			else if (Tool.safeEquals(newCol.oldname, oldName)) recreate = true;
			else continue;
			//
			matchCol = newCol;
			newIterator.remove();
			break;
		}
		if (matchCol == null) {recreate = true; continue;}
		// else
		if (!matchCol.type.name().equalsIgnoreCase(oldType)) ;// TODO cast type
		newColsStr.append((newColsStr.length() > 0 ? "," : "") + matchCol.name);
		oldColsStr.append((oldColsStr.length() > 0 ? "," : "") + oldName);
	}
	//
	//
	if (!recreate) {
		// may be JUST REPLACE INDEXES
		while (indexCursor.moveToNext()) {
			String oldIndex = indexCursor.getString(cursorNameIx);
			List<Column> ixCols = indexMap.remove(oldIndex);
			if (ixCols == null) {
				String q = "DROP INDEX IF EXISTS " + oldIndex;
				if (!db.execSql(q)) throw new Exception("Can't drop index. SQL=" + q + ". Error=" + db.getLastError());
			}
		}
		// add left new Indexes
		for (Entry<String, List<Column>> entry : indexMap.entrySet()) {
			String q = "CREATE INDEX IF NOT EXISTS " + entry.getKey() + " ON " + tableName() + " (" + Tool.join(entry.getValue(), ",") + ")";
			if (!db.execSql(q)) throw new Exception("Can't Create indexes. SQL=" + q + ". Error=" + db.getLastError());
		}
		// may be JUST ADD NEW COLUMNS
		for (Column newCol : newCols) {
			String q = "ALTER TABLE " + tableName() + " ADD COLUMN " + newCol.name + " " + newCol.type + (newCol.constraint == null ? "" : " " + newCol.constraint);
			if (!db.execSql(q)) throw new Exception("Can't add column. SQL=" + q + ". Error=" + db.getLastError());
		}
		//
		return;
	}
	//
	// else RECREATE SCHEMA
	// DROP Indexes
	while (indexCursor.moveToNext()) {
		String oldIndex = indexCursor.getString(cursorNameIx);
		String q = "DROP INDEX IF EXISTS " + oldIndex;
		if (!db.execSql(q)) throw new Exception("Can't drop index. SQL=" + q + ". Error=" + db.getLastError());
	}
	// rename old table
	String oldTable = "_" + tableName();
	String q = "ALTER TABLE " + tableName() + " RENAME TO " + oldTable;
	if (!db.execSql(q)) throw new Exception("Can't alter table. SQL=" + q + ". Error=" + db.getLastError());
	// create empty new table and indexes
	onCreate();
	// copy old values to new table
	q = "INSERT INTO " + tableName() + "(" + newColsStr + ") SELECT " + oldColsStr + " FROM " + oldTable;
	if (!db.execSql(q)) throw new Exception("Can't copy data. SQL=" + q + ". Error=" + db.getLastError());
	// drop old table
	q = "DROP TABLE IF EXISTS " + oldTable;
	if (!db.execSql(q)) throw new Exception("Can't drop old table. SQL=" + q + ". Error=" + db.getLastError());
}
protected void onOpen() {}

protected void onExit() {}







/** **   OPERATIONS */

public List<DbObject> select(String where, String orderBy, int limit) {
	return select(tableName(), where, orderBy, limit);
}

public List<DbObject> select(String tableName, String where, String orderBy, int limit) {
	ArrayList<String> cols = new ArrayList<String>();
	for (Column col : sqlColumns) cols.add(col.name);
	Cursor cursor = db.select(tableName, cols.toArray(new String[]{}), where, null, null, null, orderBy, limit > 0 ? limit + "" : null, false);
	if (cursor == null) return null;
	try {
		ArrayList<DbObject> list = new ArrayList<DbObject>();
		while (cursor.moveToNext()) {
			DbObject obj = objectClass.newInstance();
			list.add(obj);
			int $ = -1;
			for (Column col : sqlColumns) {
				if (cursor.isNull(++$)) continue;
				Field field = col.objField;
				Class<?> type = field.getType();
				Object val = null;
				if (type == String.class) val = cursor.getString($);
				else if (type == Long.class || type == long.class) val = cursor.getLong($);
				else if (type == Double.class || type == double.class) val = cursor.getDouble($);
				else if (type == Integer.class || type == int.class) val = cursor.getInt($);
				else if (type == Float.class || type == float.class) val = cursor.getFloat($);
				else if (type == Short.class || type == short.class) val = cursor.getShort($);
				else if (type == Byte.class || type == byte.class) val = (byte) cursor.getShort($);
				else if (type == Boolean.class || type == boolean.class) val = Tool.notNothing(cursor.getString($));
				//
				field.set(obj, val);
			}
		}
		if (D) Wow.i(TAG, "select", "[List]  size=" + list.size());
		//
		return list;
	} catch (Throwable ex) {
		if (D) Wow.w(TAG, "select", Tool.stackTrace(ex));
		return null;
	} finally {cursor.close();}
}

public boolean insert(DbObject object) {
	if (object == null) return false;
	ContentValues values = getValues(object, null, true, true);
	if (values == null) values = getValues(object, null, false, true);// empty object
	object._id = db.insert(tableName(), values);
	return object._id != -1;
}
public boolean update(DbObject object) {
	if (object == null || object._id == 0) return false;
	ContentValues values = getValues(object, null, true, true);
	if (values != null) return db.updateById(tableName(), values, object._id) > 0;
	return false;
}
public boolean update(DbObject object, Column... column) {
	if (object == null || object._id == 0 || column == null) return false;
	ContentValues values = getValues(object, Arrays.asList(column), false, true);
	if (values != null) return db.updateById(tableName(), values, object._id) > 0;
	return false;
}
public boolean replace(DbObject object) {
	if (object == null || object._id == 0) return false;
	ContentValues values = getValues(object, null, false, true);
	return db.updateById(tableName(), values, object._id) > 0;
}
public boolean delete(final DbObject object) {
	if (object == null || object._id == 0) return false;
	return db.deleteById(tableName(), object._id) > 0;
}

private ContentValues getValues(DbObject object, Iterable<Column> cols, boolean skipNull, boolean skipId) {
	if (cols == null) cols = sqlColumns;
	ContentValues values = new ContentValues();
	int count = 0;
	for (Column col : cols) {
		SqlType sqlType = col.type;
		String name = col.name;
		if (skipId && col instanceof IdColumn) continue;
		try {
			Field field = col.objField;
			Object value = field.get(object);
			if (value == null && skipNull) continue;
			// else
			if (sqlType == SqlType.INTEGER) values.put(name, Tool.cast(value, Long.class));
			else if (sqlType == SqlType.REAL) values.put(name, Tool.cast(value, Double.class));
			else values.put(name, Tool.cast(value, String.class));
			count++;
		} catch (Throwable ex) {Wow.e(ex);}
	}
	return count > 0 || !skipNull ? values : null;
}

public DbObject copy(DbObject srcObj) {
	DbObject resObj = null;
	try {resObj = objectClass.newInstance();} catch (Throwable ex) {Wow.e(ex);}
	if (srcObj == null || resObj == null) return null;
	for (Column col : columns) {
		try {
			Field field = col.objField;
			field.set(resObj, field.get(srcObj));
		} catch (Throwable ex) {Wow.e(ex);}
	}
	return resObj;
}






/** MISC UTILS */


public int deleteRecords(String where, String[] whereArgs) {
	return db.delete(tableName(), where, whereArgs);
}

public boolean renameTable(String newName) {
	String q = "ALTER TABLE " + tableName() + " RENAME TO " + newName;
	return db.execSql(q);
}

}
