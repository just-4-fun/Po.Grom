package cyua.java.shared;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cyua.java.shared.Column.RowidColumn;


public abstract class ObjectSh<DbTable> implements Serializable, Cloneable
{
private static final String TAG = "ObjectSh";
private static final long serialVersionUID = 7263206395995704575L;
public static final String ROWID = RowidColumn.NAME;
// WARN is persisted after android app recycle
private static HashMap<Class<? extends ObjectSh>, Schema> clas2schema = new HashMap<Class<? extends ObjectSh>, Schema>();


public static final Column.IdColumn _ID = new Column.IdColumn();
public long _id;// SQL column for unique row id


public static boolean cacheable = false;// should be copied by cacheable class



// INSTANCE API

//	should be Overriden by extending class to be cacheable
// should return id field value
abstract public String getStorableID();

// should be copied by cacheable class
abstract public boolean isCacheable();




/** STATIC API */

//public static <T extends ObjectSh> boolean isCacheable(Class<T> clas)
//{
//	boolean cacheable = false;
//	try {cacheable = clas.getDeclaredField("cacheable").getBoolean(null);} catch(Throwable ex) {}
//	return cacheable;
//}






public static <DbObject extends ObjectSh> Schema<DbObject> getSchema(Class<DbObject> objClas) {
	Schema<DbObject> schema = clas2schema.get(objClas);
	if (schema != null) return schema;
	//
	schema = new Schema<DbObject>();
	clas2schema.put(objClas, schema);
	//
	StringBuilder error = new StringBuilder();
	HashMap<Integer, List<Column>> ixMap = new HashMap<Integer, List<Column>>();
	schema.columns = new ArrayList<Column>();
	schema.sqlColumns = new ArrayList<Column>();
	schema.ftColumns = new ArrayList<Column>();
	// build hierarhy chain > to start from top Superclass (_id Column to stay first)
	ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
//	Collections.reverse(classes);
	//
	LinkedHashMap<String, Field> colFields = new LinkedHashMap<String, Field>();
	HashMap<String, Field> objFields = new HashMap<String, Field>();
	//
	Class<?> currClas = objClas;
	while (currClas != Object.class) {
		// COLLECT Fields
		Field[] fields = currClas.getDeclaredFields();
		for (Field f : fields) {
			int mod = f.getModifiers();
			boolean isStatic = Modifier.isStatic(mod);
			String lowName = f.getName().toLowerCase();
			if (Column.class.isAssignableFrom(f.getType()) && isStatic) {
				colFields.put(lowName, f);
			}
			else if (!isStatic) objFields.put(lowName, f);
		}
		currClas = currClas.getSuperclass();
	}
	// LINK Fields
	for (Map.Entry<String, Field> pair : colFields.entrySet()) {
		try {
			String lowName = pair.getKey();
			Field colField = pair.getValue();
			Field objField = objFields.get(lowName);
			if (objField == null) throw new Exception("Column " + lowName + " has no matching property.");
			//
			colField.setAccessible(true);
			Column col = (Column) colField.get(null);// throws Ex if not assigned
			if (col instanceof Column.IdColumn) schema.idColumn = (Column.IdColumn) col;
			//
			String realName = objField.getName();
			if (col.name == null) col.name = realName;
			if (col.localName == null) col.localName = col.name;
			//
			col.objField = objField;
			Class<?> dataClas = col.objField.getType();
			//
			if (col.type == null) {
				if (dataClas == Double.class || dataClas == double.class || dataClas == Float.class || dataClas == float.class)
					col.type = Column.SqlType.REAL;
				else if (Number.class.isAssignableFrom(dataClas) || dataClas == long.class || dataClas == int.class || dataClas == short.class || dataClas == byte.class)
					col.type = Column.SqlType.INTEGER;
				else col.type = Column.SqlType.TEXT;
			}
			if (col.fttype == null) {
				if (col.type == Column.SqlType.INTEGER || col.type == Column.SqlType.REAL)
					col.fttype = Column.FtType.NUMBER;
				else col.fttype = Column.FtType.STRING;
			}
			// collect INDEX info
			if (col.indexId != 0) {
				List<Column> ixCols = ixMap.get(col.indexId);
				if (ixCols == null) {
					ixCols = new ArrayList<Column>();
					ixMap.put(col.indexId, ixCols);
				}
				ixCols.add(col);
			}
			//
			schema.columns.add(col);
			if (col.type != null && col.type != Column.SqlType.NONE) schema.sqlColumns.add(col);
			if (col.fttype != null && col.fttype != Column.FtType.NONE) schema.ftColumns.add(col);
		} catch (Exception ex) {
			error.append("Problem with field " + pair.getKey() + " in " + objClas.getSimpleName() + ". " + ex.getClass().getSimpleName() + " :: " + ex.getMessage() + ";  ");
		}
	}
	// sort Columns
	Collections.sort(schema.columns);
	Collections.sort(schema.sqlColumns);
	Collections.sort(schema.ftColumns);
	// convert INDEX info
	schema.indexMap = new HashMap<String, List<Column>>();
	if (ixMap.size() > 0) {
//		for (int $ = 0; $ < ixMap.size(); $++) {
		for (Map.Entry<Integer, List<Column>> pair : ixMap.entrySet()) {
			List<Column> cols = pair.getValue();
			String name = "";
			for (Column col : cols) {
				name += (name.length() > 0 ? "_" : "") + col.name;
			}
			schema.indexMap.put(name, cols);
		}
	}
	//
	try {
		schema.cacheable = ((ObjectSh) objClas.newInstance()).isCacheable();
	} catch (Exception ex) {error.append("Read cacheable property error: "+ex);}
	//
	if (BaseTool.notEmpty(error)) BaseTool.log(TAG, error.toString());
	//
	return schema;
}

public static <T extends ObjectSh> boolean isCacheable(Class<T> typeCls) {
	try {
		return getSchema(typeCls).cacheable;
	} catch (Exception e) {
		BaseTool.log(TAG, e.toString());
		return false;
	}
}
public static <T extends ObjectSh> Column[] getFTColumns(Class<T> objClas, String[] ftcolumns) {
	List<Column> cols = getSchema(objClas).columns;
	Column[] resCols = new Column[ftcolumns.length];
	for (int $ = 0; $ < ftcolumns.length; $++) {
		String colName = ftcolumns[$];
		for (Column col : cols) {
			if (col.localName.equals(colName)) {
				resCols[$] = col;
				break;
			}
		}
	}
	return resCols;
}






/** SCHEMA **/


public static class Schema<DbObject extends ObjectSh> {
	public Class<DbObject> objectClass;
	public List<Column> columns, sqlColumns, ftColumns;
	public Column.IdColumn idColumn;
	public HashMap<String, List<Column>> indexMap;
	public boolean cacheable;

	public Field[] getFieldsOfColumns(String[] colNames) {
		Field[] fields = new Field[colNames.length];
		int $ = 0;
		for (String colName : colNames) {
			Column col = findColumn(colName);
			fields[$++] = col != null ? col.objField : null;
		}
		return fields;
	}

	public Column findColumn(String colName) {
		for (Column col : columns)
			if (col.name.equals(colName)) return col;
		//
		return null;
	}

	public String toCSV(List<? extends ObjectSh> list) {
		String TAB = "\t";
		StringBuilder str = new StringBuilder();
		for (ObjectSh obj : list) {
			for (Column col : columns) {
				Object val = col.get(obj);
				String value = val == null ? "" : val.toString().replace('"', '\'').replace('\n', ' ').replace('\r', ' ');
				str.append("\"").append(value).append("\"").append(TAB);
			}
			str.append("\n");
		}
		return str.toString();
	}

}

}
