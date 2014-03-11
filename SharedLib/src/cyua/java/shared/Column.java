package cyua.java.shared;

import java.lang.reflect.Field;


public class Column implements Comparable<Column> {

private static final String TAG = Column.class.getSimpleName();
//public static final Column ROWID = new Column();//"FusionTable's rowid";
//Sql types
public static enum SqlType {
	NONE, TEXT, REAL, INTEGER
}
// NONE for non-Sql tables
//public static final String INTEGER = "INTEGER";// maps to Bundle.Long
//public static final String REAL = "REAL";// maps to Bundle.Double
//public static final String TEXT = "TEXT";// maps to Bundle.String
// FusionTable types
public static enum FtType {
	NONE, STRING, NUMBER, DATETIME, LOCATION
}
//NONE for non-FTTables
//public static final String F_STRING = "STRING";
//public static final String F_NUMBER = "NUMBER";
//public static final String F_DATETIME = "DATETIME";
//public static final String F_LOCATION = "LOCATION";




/** INSTANCE */

public int order;
public Field objField;
public String name;
public String localName;
public SqlType type;
public FtType fttype;
public String constraint;
public int indexId;
public String oldname;



public Column(int order) {
	this.order = order;
}


public Column name(String val) {
	name = val;
	return this;
}
public Column localName(String val) {
	localName = val;
	return this;
}
public Column type(SqlType val) {
	type = val;
	return this;
}
public Column fttype(FtType val) {
	fttype = val;
	return this;
}
public Column constraint(String val) {
	constraint = val;
	return this;
}
public Column indexId(int val) {
	indexId = val;
	return this;
}
public Column oldname(String val) {
	oldname = val;
	return this;
}
public Column notSql() {
	type = SqlType.NONE;
	return this;
}
public Column notFt() {
	fttype = FtType.NONE;
	return this;
}

/*
public Column ord(int val) {
	order = val;
	return this;
}
*/
public String getLocalName() {
	return "'"+localName+"'";
}

@Override public String toString() {
	return name;
}

public void set(Object object, Object value) {
	if (objField == null) return;
	try {
		objField.setAccessible(true);
		objField.set(object, value);
	} catch (Exception ex) {BaseTool.log(TAG, "Can't set column value to object. " + ex.getMessage());}
}

public <T> T get(Object object) {
	if (objField == null) return null;
	try {
		objField.setAccessible(true);
		@SuppressWarnings("unchecked")
		T val = (T) objField.get(object);
		return val;
	} catch (Exception ex) {BaseTool.log(TAG, "Can't get column value. " + ex.getMessage());}
	return null;
}



/** ORDER IMPLEMENTATION */

@Override public int compareTo(Column another) {
	return order - another.order;
}





/** COLUMN SUBTYPES */

public static final class IdColumn extends Column {
	public IdColumn() {
		super(-1);
		name = "_id";
		type = SqlType.INTEGER;
		constraint = "PRIMARY KEY AUTOINCREMENT";
		fttype = FtType.NONE;
	}
}




public static class RowidColumn extends Column {
	public static final String NAME = "rowid";

	public RowidColumn() {
		super(Integer.MAX_VALUE);
		name = NAME;
		type = SqlType.TEXT;
		fttype = FtType.STRING;
	}
}
}
