package cyua.gae.appserver.fusion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.gson.Gson;

import cyua.gae.appserver.App;
import cyua.gae.appserver.Tool;
import cyua.gae.appserver.urlfetch.Account;
import cyua.gae.appserver.urlfetch.Credentials;
import cyua.gae.appserver.urlfetch.HttpRequest;
import cyua.gae.appserver.urlfetch.HttpRequest.ContentType;
import cyua.gae.appserver.urlfetch.HttpResponse;
import cyua.gae.appserver.urlfetch.HttpResponse.RsStatus;
import cyua.gae.appserver.urlfetch.NVPair;
import cyua.gae.appserver.urlfetch.NVPairs;
import cyua.gae.appserver.urlfetch.Scope;
import cyua.java.shared.Column;
import cyua.java.shared.RMIException;
import cyua.java.shared.ObjectSh;


public class FTOperation
{
static final Logger log = Logger.getLogger(FTOperation.class.getName());
//
protected static final String BASE_URL = "https://www.googleapis.com/fusiontables/v1/";
//
protected static final String SQL_PARAM = "sql";
protected static final String JSON_PARAM = "json";
protected static final String KEY_PARAM = "key";
// object types
static enum TargetType {ROW, TABLE, VIEW, COLUMN, TEMPL, STYLE}
// action type
static enum ActionType {LIST, GET, INSERT, INSERT_BATCH, UPDATE, DELETE}
//types
static enum GType {NUMBER, STRING, LOCATION, DATETIME}
// sql constants
public static final String CREATE_VIEW = "CREATE VIEW ";//CREATE VIEW <view_name> AS (SELECT  <column_spec> {, <column_spec>}* FROM <table_id> {WHERE <filter_condition> { AND <filter_condition> }*})
public static final String SELECT = "SELECT ", FROM = " FROM ",  WHERE = " WHERE ";
public static final String ORDERBY = " ORDER BY ", ASC = " ASC ", DESC = " DESC ", LIMIT = " LIMIT ", GROUPBY = " GROUP BY ";
public static final String INSERT = "INSERT INTO ", VALUES = " VALUES ";
public static final String UPDATE = "UPDATE ", SET = " SET ";
public static final String DELETE = "DELETE FROM ";
public static final String DROP = "DROP TABLE ";
public static final String UpROWID = "ROWID";// in select
public static final String LwROWID = "rowid";// in where and response
public static final String AS = " AS ";
//
public static final String Q = "'";
//
public static enum OpErrCause {OTHER, FORMAT, BAD_REQUEST, NOT_FOUND}
//
static final int NORM_RETRIES = 4;
static final int MAX_RETRIES = 8;
public static final long MOD_RATE_LIMIT = 2000;
public static final long SELECT_RATE_LIMIT = 200;
public static final int MAX_SELECT_LIMIT = 1000;


//===================================================
//VIEW // TODO deprecated
private static Pattern pttn = Pattern.compile("([^,\\r\\n\"]*|\"(([^\"]*\"\")*[^\"]*)\")(,|\\r?\\n|\\z)");



// -------------------------------------------------------------------
public static void createView(FTTable view) throws FTException
{
	StringBuilder buff = new StringBuilder();
	if (view.columns == null) buff.append("*");
	else for (FTColumn col : view.columns) buff.append((buff.length()>0?",":"")+Q+col+Q);
	buff.insert(0, CREATE_VIEW+Q+view.name+Q+AS+"("+SELECT);
	buff.append(FROM+view.baseTable.tableId);
	if (!Tool.isEmpty(view.filter)) buff.append(WHERE+view.filter);
	buff.append(")");
	//
	FTOperation op = new FTOperation(TargetType.VIEW, ActionType.INSERT, buff.toString(), view, null, 0);
	op.execute();
	FTRows res = op.getResult(FTRows.class);
	String viewId = res == null ? null : res.getFirstValue();
	if (Tool.isEmpty(viewId)) throw new FTException("Insert View failed. No tableid.");
	view.tableId = viewId;
	try {view.updateDefaultStyle();} catch (Throwable ex) {}
}


// ===================================================
// TABLE
public static FTTable[] listTables(Account _acc) throws FTException
{
	FTOperation op = new FTOperation(TargetType.TABLE, ActionType.LIST, null, null, null, 0);
	op.account = _acc;
	op.execute();
	FTTableList list = op.getResult(FTTableList.class);
	if (Tool.isEmpty(list) || Tool.isEmpty(list.items)) return new FTTable[0];
	for (FTTable tab : list.items) tab.account = _acc;
	return list.items;
}
// -------------------------------------------------------------------
public static FTTable getTable(FTTable tab) throws FTException
{
	FTOperation op = new FTOperation(TargetType.TABLE, ActionType.GET, null, tab, null, 0);
	op.execute();
	FTTable newTab = op.getResult(FTTable.class);
	if (newTab == null) return null;
	newTab.account = tab.account;
	return newTab;
}
//-------------------------------------------------------------------
public static FTTable insertTable(FTTable tab) throws FTException
{
	String jsonData = toJson(tab);
	FTOperation op = new FTOperation(TargetType.TABLE, ActionType.INSERT, jsonData, tab, null, 0);
	op.execute();
	FTTable newTab = op.getResult(FTTable.class);
	if (newTab == null) return null;
	newTab.account = tab.account;
	try {newTab.updateDefaultStyle();} catch (Throwable ex) {}
	return newTab;
}
//-------------------------------------------------------------------
public static FTTable updateTable(FTTable tab) throws FTException
{
	String jsonData = toJson(tab);
	FTOperation op = new FTOperation(TargetType.TABLE, ActionType.UPDATE, jsonData, tab, null, 0);
	op.execute();
	FTTable newTab = op.getResult(FTTable.class);
	if (newTab == null) return null;
	newTab.account = tab.account;
	return newTab;
}
//-------------------------------------------------------------------
public static void deleteTable(FTTable tab) throws FTException
{
	FTOperation op = new FTOperation(TargetType.TABLE, ActionType.DELETE, null, tab, null, 0);
	op.execute();
}

//===================================================
//COLUMN
public static FTColumn[] listColumns(FTTable tab) throws FTException
{
FTOperation op = new FTOperation(TargetType.COLUMN, ActionType.LIST, null, tab, null, 0);
op.execute();
FTColumnList list = op.getResult(FTColumnList.class);
return list == null ? null : list.items;
}
//-------------------------------------------------------------------
public static FTColumn getColumn(FTTable tab, String columnId) throws FTException
{
FTOperation op = new FTOperation(TargetType.COLUMN, ActionType.GET, null, tab, columnId, 0);
op.execute();
return op.getResult(FTColumn.class);
}
//-------------------------------------------------------------------
public static FTColumn insertColumn(FTTable tab, FTColumn column) throws FTException
{
if (column.name.toLowerCase().equals(LwROWID)) return null;
String jsonData = toJson(column);
FTOperation op = new FTOperation(TargetType.COLUMN, ActionType.INSERT, jsonData, tab, null, 0);
op.execute();
return op.getResult(FTColumn.class);
}
//-------------------------------------------------------------------
public static FTColumn updateColumn(FTTable tab, FTColumn column) throws FTException
{
if (column.name.toLowerCase().equals(LwROWID)) return null;
String jsonData = toJson(column);
FTOperation op = new FTOperation(TargetType.COLUMN, ActionType.UPDATE, jsonData, tab, column.columnId, 0);
op.execute();
return op.getResult(FTColumn.class);
}
//-------------------------------------------------------------------
public static void deleteColumn(FTTable tab, String columnId) throws FTException
{
FTOperation op = new FTOperation(TargetType.COLUMN, ActionType.DELETE, null, tab, columnId, 0);
op.execute();
}

//===================================================
//TEMPLATE
public static FTTemplate[] listTemplates(FTTable tab) throws FTException
{
FTOperation op = new FTOperation(TargetType.TEMPL, ActionType.LIST, null, tab, null, 0);
op.execute();
FTTemplList list = op.getResult(FTTemplList.class);
return list == null ? null : list.items;
}
//-------------------------------------------------------------------
public static FTTemplate getTemplate(FTTable tab, String templId) throws FTException
{
FTOperation op = new FTOperation(TargetType.TEMPL, ActionType.GET, null, tab, templId, 0);
op.execute();
return op.getResult(FTTemplate.class);
}
//-------------------------------------------------------------------
public static FTTemplate insertTemplate(FTTable tab, FTTemplate templ) throws FTException
{
String jsonData = toJson(templ);
FTOperation op = new FTOperation(TargetType.TEMPL, ActionType.INSERT, jsonData, tab, null, 0);
op.execute();
return op.getResult(FTTemplate.class);
}
//-------------------------------------------------------------------
public static FTTemplate updateTemplate(FTTable tab, FTTemplate templ) throws FTException
{
String jsonData = toJson(templ);
FTOperation op = new FTOperation(TargetType.TEMPL, ActionType.UPDATE, jsonData, tab, templ.templateId, 0);
op.execute();
return op.getResult(FTTemplate.class);
}
//-------------------------------------------------------------------
public static void deleteTemplate(FTTable tab, String templId) throws FTException
{
FTOperation op = new FTOperation(TargetType.TEMPL, ActionType.DELETE, null, tab, templId, 0);
op.execute();
}

//===================================================
//STYLE
public static FTStyle[] listStyles(FTTable tab) throws FTException
{
FTOperation op = new FTOperation(TargetType.STYLE, ActionType.LIST, null, tab, null, 0);
op.execute();
FTStyleList list = op.getResult(FTStyleList.class);
return list == null ? null : list.items;
}
//-------------------------------------------------------------------
public static FTStyle getStyle(FTTable tab, String styleId) throws FTException
{
FTOperation op = new FTOperation(TargetType.STYLE, ActionType.GET, null, tab, styleId, 0);
op.execute();
return op.getResult(FTStyle.class);
}
//-------------------------------------------------------------------
public static FTStyle insertStyle(FTTable tab, FTStyle style) throws FTException
{
String jsonData = toJson(style);
FTOperation op = new FTOperation(TargetType.STYLE, ActionType.INSERT, jsonData, tab, null, 0);
op.execute();
return op.getResult(FTStyle.class);
}
//-------------------------------------------------------------------
public static FTStyle updateStyle(FTTable tab, FTStyle style) throws FTException
{
String jsonData = toJson(style);
FTOperation op = new FTOperation(TargetType.STYLE, ActionType.UPDATE, jsonData, tab, style.styleId, 0);
op.execute();
return op.getResult(FTStyle.class);
}
//-------------------------------------------------------------------
public static void deleteStyle(FTTable tab, String styleId) throws FTException
{
FTOperation op = new FTOperation(TargetType.STYLE, ActionType.DELETE, null, tab, styleId, 0);
op.execute();
}

// ===================================================
// ROW
public static <T extends ObjectSh> String selectRowid(FTTable tab, String where, int retryms) throws FTException
{
	StringBuilder buff = new StringBuilder();
	buff.append(SELECT+ LwROWID+FROM+tab.tableId);
	if (!Tool.isEmpty(where)) buff.append(WHERE+where);
	//
	FTOperation op = new FTOperation(TargetType.ROW, ActionType.GET, buff.toString(), tab, null, retryms);
	op.execute();
	FTRows rows = op.getResult(FTRows.class);
	return rows == null ? null : rows.getFirstValue();
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> List<T> selectRows(Class<T> rowClass, FTTable tab, List<FTColumn> columns, String where, String orderBy, boolean descOrder, int limit, int retryms) throws FTException
{
	if (Tool.isEmpty(columns)) columns = tab.getColumns(true);
	StringBuilder buff = new StringBuilder();
	for (FTColumn col : columns) buff.append((buff.length()>0?",":"")+Q+col+Q);
	FTRows rows = selectRows(tab, buff.toString(), where, orderBy, descOrder, limit, retryms, null);
	return rows == null ? new ArrayList<T>() : rows.toList(rowClass);
}
//-------------------------------------------------------------------
public static FTRows selectRows(FTTable tab, String columns, String where, String orderBy, boolean descOrder, int limit, int retryms, String groupBy) throws FTException
{
StringBuilder buff = new StringBuilder(SELECT);
buff.append(Tool.isEmpty(columns) ? "*" : columns);
buff.append(FROM+tab.tableId);
if (!Tool.isEmpty(where)) buff.append(WHERE+where);
if (!Tool.isEmpty(groupBy)) buff.append(GROUPBY+groupBy);
if (!Tool.isEmpty(orderBy)) buff.append(ORDERBY+Q+orderBy+Q+(descOrder?DESC:""));
if (limit == 0) limit = MAX_SELECT_LIMIT;// to  avoid ResponseTooLargeException 32Mb limit
buff.append(LIMIT+limit);
//
FTOperation op = new FTOperation(TargetType.ROW, ActionType.GET, buff.toString(), tab, null, retryms);
op.execute();
FTRows rows = op.getResult(FTRows.class);
return rows != null ? rows : new FTRows();
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> T selectRow(Class<T> rowClass, FTTable tab, List<FTColumn> columns, String rowid, int retryms) throws FTException
{
StringBuilder buff = new StringBuilder();
if (Tool.isEmpty(columns)) buff.append("*");
else for (FTColumn col : columns) buff.append((buff.length()>0?",":"")+Q+col+Q);
buff.insert(0, SELECT);
buff.append(FROM+tab.tableId);
//
FTOperation op = new FTOperation(TargetType.ROW, ActionType.GET, buff.toString(), tab, null, retryms);
op.execute();
FTRows rows = op.getResult(FTRows.class);
if (rows == null) return null;
List<T> list = rows.toList(rowClass);
return Tool.isEmpty(list) ? null : list.get(0);
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> String selectValue(FTTable tab, String expr, String where, int retryms) throws FTException
{
String sql = SELECT+expr+FROM+tab.tableId;
if (!Tool.isEmpty(where)) sql += WHERE+where;
//
FTOperation op = new FTOperation(TargetType.ROW, ActionType.GET, sql, tab, null, retryms);
op.execute();
FTRows rows = op.getResult(FTRows.class);
return rows == null ? null : rows.getFirstValue();
}
// -------------------------------------------------------------------
public static <T extends ObjectSh> String  insertRow(FTTable tab, T obj) throws FTException
{
	if (obj == null) return null;
	String sql = null;
	StringBuilder cols = new StringBuilder();
	StringBuilder vals = new StringBuilder();
	for (Column col: ObjectSh.getSchema(obj.getClass()).ftColumns)
	{
		try
		{
			if (col.name.equals(Column.RowidColumn.NAME)) continue;
			String value = FTUtils.convert2FT(col.get(obj));
			if (value == null) continue;
			//
			cols.append((cols.length()>0?",":"")+Q+col.localName+Q);
			vals.append((vals.length()>0?",":"")+Q+value+Q);
		}
		catch (Throwable ex) {log.severe("Failed to parse value. Table:"+tab.name+",  column:"+col.localName+",  obj:"+Tool.printObject(obj)+",  Error:"+ex);}
	}
	sql = INSERT+tab.tableId+" ("+cols+")"+VALUES+"("+vals+")";
	if (cols.length() == 0 || vals.length() == 0) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "[insertRow] no sql.");
	//
	FTOperation op = new FTOperation(TargetType.ROW, ActionType.INSERT, sql, tab, null, 0);
	op.execute();
	FTRows rows = op.getResult(FTRows.class);
	return rows == null ? null : rows.getFirstValue();
}

//-------------------------------------------------------------------
public static <T extends ObjectSh> void insertRows(FTTable tab, FTRows rows, int retryms) throws FTException
{
	if (rows.isEmpty()) return;
	//
	StringBuilder cols = new StringBuilder();
	int rowix = -1, colsLen = rows.columns.length;
	for (int $ = 0; $ < colsLen; $++) {
		String col = rows.columns[$];
		if (col.equals(LwROWID) || col.equals(UpROWID)) {rowix = $; continue;}
		cols.append((cols.length()>0?",":"")+Q+col+Q);
	}
	//
	int step = 100, fromIx = 0, toIx = step;
	while (fromIx < rows.length())
	{
		Object[][] subset = rows.subset(fromIx, toIx);
		StringBuilder sql = new StringBuilder();
		//
		for (int $ = 0; $ < subset.length; $++) {
			Object[] row = subset[$];
			StringBuilder vals = new StringBuilder();
			for (int $$ = 0; $$ < colsLen; $$++) {
				if ($$ == rowix) continue;
				String value = null;
				try
				{
					value = FTUtils.convert2FT(row[$$]);
					vals.append((vals.length()>0?",":"")+Q+(value==null?"":value)+Q);
				}
				catch (Throwable ex) {log.severe("Failed to parse value. Table:"+tab.name+",  column:"+rows.columns[$$]+",  value:"+Tool.printObject(value)+",  Error:"+ex);}
			}
			sql.append(INSERT+tab.tableId+" ("+cols+")"+VALUES+"("+vals+")");
			sql.append(";");
		}
		FTOperation op = new FTOperation(TargetType.ROW, ActionType.INSERT, sql.toString(), tab, null, retryms);
		op.execute();
		//
		try {Thread.sleep(MOD_RATE_LIMIT);} catch (Throwable ex) {}
		//
		fromIx = toIx;
		toIx += step;
	}
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> String[] insertRows(FTTable tab, List<T> objects, int retryms) throws FTException
{
	if (Tool.isEmpty(objects)) return new String[0];
	else if (objects.size() == 1)
	{
		String rowid = insertRow(tab, objects.get(0));
		return new String[] {rowid};
	}
	else if (objects.size() > 100)
	{
		int fromIx = 0, toIx = 100;
		List<String> ids = new ArrayList<String>();
		while (fromIx < objects.size())
		{
			List<T> subset = objects.subList(fromIx, toIx);
			String[] subids = FTOperation.insertRows(tab, subset, retryms);
			ids.addAll(Arrays.asList(subids));
			fromIx = toIx;
			toIx = toIx+100 <= objects.size() ? toIx+100 : objects.size();
			try {Thread.sleep(MOD_RATE_LIMIT);} catch (Throwable ex) {}
		}
		return ids.toArray(new String[0]);
	}
	// else
	StringBuilder sql = new StringBuilder();
	StringBuilder cols = new StringBuilder();
	boolean first = true;
	for (T obj : objects)
	{
		StringBuilder vals = new StringBuilder();
		for (Column col: ObjectSh.getSchema(obj.getClass()).ftColumns)
		{
//			String col = col = pair.getKey();
			try
			{
				if (col.name.equals(Column.RowidColumn.NAME)) continue;
				if (first) cols.append((cols.length()>0?",":"")+Q+col.localName+Q);
				String value = FTUtils.convert2FT(col.get(obj));
				vals.append((vals.length()>0?",":"")+Q+(value==null?"":value)+Q);
			}
			catch (Throwable ex) {log.severe("Failed to parse value. Table:"+tab.name+",  column:"+col.localName+",  obj:"+Tool.printObject(obj)+",  Error:"+ex);}
		}
		sql.append(INSERT+tab.tableId+" ("+cols+")"+VALUES+"("+vals+")");
		sql.append(";");
		first = false;
	}
	if (cols.length() == 0) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "[updateRow] no sql.");
	//
	FTOperation op = new FTOperation(TargetType.ROW, ActionType.INSERT, sql.toString(), tab, null, retryms);
	op.execute();
	FTRows rows = op.getResult(FTRows.class);
	return rows == null ? null : rows.getInsertedRowIds();
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> void updateRowByCond(FTTable tab, T obj, String where, int retryms) throws FTException
{
	String rowid = selectRowid(tab, where, retryms);
	if (Tool.isEmpty(rowid)) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.NOT_FOUND, "[updateRow] row not found in FTDB.");
	updateRow(tab, obj, rowid);
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> void updateRow(FTTable tab, T obj, String rowid) throws FTException
{
StringBuilder sql = new StringBuilder();
for (Column col: ObjectSh.getSchema(obj.getClass()).ftColumns)
{
	try
	{
		if (col.name.equals(Column.RowidColumn.NAME)) continue;
		String value = FTUtils.convert2FT(col.get(obj));
		if (value == null) continue;
		sql.append((sql.length()>0?",":"")+Q+col.localName+Q+"="+Q+value+Q);
	}
	catch (Throwable ex) {log.severe("Failed to parse value. Table:"+tab.name+",  column:"+col.localName+",  obj:"+Tool.printObject(obj)+",  Error:"+ex);}
}
if (sql.length() == 0) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "[updateRow] no sql.");
//
sql.insert(0, UPDATE+tab.tableId+SET);
sql.append(WHERE+LwROWID+"="+Q+rowid+Q);
//
FTOperation op = new FTOperation(TargetType.ROW, ActionType.UPDATE , sql.toString(), tab, rowid, 0);
op.execute();
}
// -------------------------------------------------------------------
public static void deleteRow(FTTable tab, String rowid) throws FTException
{
	String sql = DELETE+tab.tableId+(!Tool.isEmpty(rowid) ? WHERE+LwROWID+"="+Q+rowid+Q : "");
	//
	FTOperation op = new FTOperation(TargetType.ROW, ActionType.DELETE, sql, tab, rowid, 0);
	op.execute();
}


// -------------------------------------------------------------------
static String toJson(Object obj)
{
return new Gson().toJson(obj);	
}
// -------------------------------------------------------------------
static String toJson(Object obj, Type typeOfT)
{
return new Gson().toJson(obj, typeOfT);	
}





/***********************************/
TargetType target;
ActionType action;
String content;
FTTable table;
String targetId;
String result;
int retryms = 20000;
Account account;

// -------------------------------------------------------------------
FTOperation(TargetType targ, ActionType act, String data, FTTable tab, String targId, int ms)
{
	target = targ;
	action = act;
	content = data;
	table = tab;
	targetId = targId;
	if (ms > 0) retryms = ms;
	if (table != null && table.account != null) account = table.account;
	log.info("    >>   [FT OPERATION]: "+(table != null?"table:"+table.name:"")+"  target:"+target+"  action:"+action+"  content:"+(content==null?"null":(content.length()>300?content.substring(0, 300)+" ...":content)));
}

//-------------------------------------------------------------------
public void execute() throws FTException
{
	if (account == null) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "No Account.");
	String url = getURL();
	//
	HTTPMethod meth = null;
	if (target == TargetType.ROW) meth = HTTPMethod.POST;
	else if (action == ActionType.LIST || action == ActionType.GET) meth = HTTPMethod.GET;
	else if (action == ActionType.INSERT) meth = HTTPMethod.POST;
	else if (action == ActionType.UPDATE) meth = HTTPMethod.PUT;
	else if (action == ActionType.DELETE) meth = HTTPMethod.DELETE;
	//
	ContentType contType = target == TargetType.ROW || target == TargetType.VIEW ? ContentType.DEFAULT : ContentType.JSON;
	// Set CONTENT
	NVPairs<NVPair> bodyargs = NVPairs.newList();
	if (contType == ContentType.DEFAULT) {
		bodyargs.add(SQL_PARAM, getContent());
		bodyargs.add(KEY_PARAM, App.getApiKey());// else it's included in url
	}
	else if (!Tool.isEmpty(content)) bodyargs.add(JSON_PARAM, content);
	// to select values as is (not converted)
	if (target == TargetType.ROW) bodyargs.add("typed", "false");
	//
	Credentials creds = new Credentials(account, Scope.FUSION_OLD);//target == TargetType.VIEW ? Scope.FUSION_OLD : Scope.FUSION);
	// EXEC
	HttpResponse response = HttpRequest.exec(url, meth, bodyargs, null, creds, retryms, contType);
	// RESULT
	RsStatus status = response.status;
	int code =response.getCode();
	result = response.asText();
	//
	if (status == RsStatus.OK);// log.info("    <<   [FT OPERATION OK]:  resultCode:"+resultCode+",   RESULT:"+(result==null?"null":(result.length()>300?result.substring(0, 300)+" ...":result)));
	else 
	{
		OpErrCause cause = OpErrCause.OTHER;
		switch (response.getCode())
		{
			case 400:// Bad Request
				// Ex: Invalid query: Column `Text2' does not exist
				cause = OpErrCause.BAD_REQUEST;
				break;
			case 404://Not Found
				cause = OpErrCause.NOT_FOUND;
				break;
		}
		log.warning(String.format("    <<   [FT OPERATION FAILURE]:  TARGET=%1$s; ACT=%2$s;  CODE=%3$s;  STATUS=%4$s;  CAUSE=%5$s;  RESPONSE=%6$s;  ACCOUNT=%7$s; URL=%8$s", target, action, code, status, cause, result, account.name, url));
		throw new FTException(response.getCode(), status, cause, result);
	}
}
/*
UPDATE row with fake rowid > results in OK, affected_rows = 1. I.e. no way to know if row is present
UPDATE fake column results in 400 Error. "Invalid query: Base column reference FakeColumn
SELECT row with fakse rowid results in empty list of rows
SELECT with fakse table_id resulta in 400 Error "Invalid query: Invalid table id 4030413"
INSERT with fake column results in 400 Error "Invalid query: Bad column reference Text1"
After manual deletion table is still accessible for some time
GET TABLE with fake id results in 404 Error "The table XXXXXX does not exist."
GET COLUMN with fakse name or id results in 404 Error "The column 7 in table XXXXXXX does not exist."
GET anything with fake table_id results in 404 Error "The column 7 in table XXXXXXX does not exist."
*/
// -------------------------------------------------------------------
private String getURL() throws FTException
{
	String key = "?"+KEY_PARAM+"="+App.getApiKey();
	String url = BASE_URL;
	switch (target)
	{
		case ROW:
		case VIEW:
			url += "query";
			// key is added in bodyargs
			break;
		case TABLE:
			url += "tables";
			if (action == ActionType.LIST) url += key+"&maxResults=250";
			else if (action != ActionType.INSERT) url += "/"+getTableId()+key;
			else url += key;
//			if (action == ActionType.UPDATE) url += "?replaceViewDefinition=true";// TODO if possible in Http PUT
			break;
		case COLUMN:
			url += "tables/"+getTableId()+"/columns";
			if (action == ActionType.LIST) url += key+"&maxResults=50";
			else if (action != ActionType.INSERT) url += "/"+getTargetId()+key;
			else url += key;
		break;
		case TEMPL:
			url += "tables/"+getTableId()+"/templates";
			if (action == ActionType.LIST) url += key;
			else if (action != ActionType.INSERT) url += "/"+getTargetId()+key;
			else url += key;
			break;
		case STYLE:
			url += "tables/"+getTableId()+"/styles";
			if (action == ActionType.LIST) url += key;
			else if (action != ActionType.INSERT) url += "/"+getTargetId()+key;
			else url += key;
			break;
		default:
			break;
	}
	return url;
}
// -------------------------------------------------------------------
private String getTableId() throws FTException
{
	if (table == null || Tool.isEmpty(table.tableId)) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "Table without id");
	else return table.tableId;
}
// -------------------------------------------------------------------
private String getTargetId() throws FTException
{
	if (Tool.isEmpty(targetId)) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "Target without id");
	else return targetId;
}
// -------------------------------------------------------------------
private String getContent() throws FTException
{
	if (Tool.isEmpty(content)) throw new FTException(RMIException.FAIL_CODE, RsStatus.FAIL, OpErrCause.FORMAT, "Content is empty");
	else return content;
}

// ===================================================
@SuppressWarnings("unchecked")
<T> T getResult(Class<?> classOfT)
{
//	log.info("[FT GET RES]: "+result);
return (T) new Gson().fromJson(result, classOfT);
}

@SuppressWarnings("unchecked")
<T> T getResult(Type typeOfT)
{
return (T) new Gson().fromJson(result, typeOfT);
}



// ===================================================
public static class FTException extends RMIException
{
private static final long serialVersionUID = -5084135368976659192L;
//
public RsStatus status;
public OpErrCause cause;

public FTException(int code, RsStatus _status, String message)
{
	super(code, message);
	status = _status;
}
public FTException(int code, RsStatus _status, OpErrCause _cause, String message)
{
	super(code, message);
	status = _status; cause = _cause;
}
public FTException(String msg)
{
	super(msg);
	status = RsStatus.FAIL; cause = OpErrCause.NOT_FOUND;
}
}

}
