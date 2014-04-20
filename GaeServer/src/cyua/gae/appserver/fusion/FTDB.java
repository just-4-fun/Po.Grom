package cyua.gae.appserver.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import cyua.gae.appserver.MessageManager;
import cyua.gae.appserver.Tool;
import cyua.gae.appserver.fusion.FTOperation.FTException;
import cyua.gae.appserver.fusion.FTTable.FTTableDescription;
import cyua.gae.appserver.memo.MCache;
import cyua.gae.appserver.memo.MCache.CacheKeys;
import cyua.gae.appserver.memo.Memo;
import cyua.gae.appserver.urlfetch.Account;
import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;
import cyua.java.shared.RMIException;
import cyua.java.shared.objects.BlockedSh;
import cyua.java.shared.objects.ConfigSh;
import cyua.java.shared.objects.LogRecordSh;
import cyua.java.shared.objects.MessageSh;


public class FTDB {
static final Logger log = Logger.getLogger(FTDB.class.getName());

static final String msgTabName = "Повідомлення";
static final String logsTabName = "Logs";
static final String cfgTabName = "Конфігурація";
static final String blockedTabName = "Заблоковані";
//
private static FTTable msgTable;
private static FTTable logsTable;
private static FTTable cfgTable;
private static FTTable blockedTable;
//
public static final int MAX_INSERT_ROWS = 500;



// -------------------------------------------------------------------
public static void init() throws RMIException {
	initRootAccount();
	MCache.saveValue(CacheKeys.LAST_FTDB_INIT, Tool.now());
}

// -------------------------------------------------------------------
private static void initRootAccount() throws RMIException {
	Tool.setTimer();
	Account acc = Account.ROOT;
	FTTable[] tabs = FTOperation.listTables(acc);
	//
	long now = Tool.now();
	// MESSAGES tab
	FTTable tab = getTable(tabs, msgTabName);
	if (tab == null)
		tab = FTOperation.insertTable(tableFromType(MessageSh.class, msgTabName, acc, 0));
	else syncSchema(tab, MessageSh.class);
	Memo.save(tab);
	tab.updateDefaultTemplate(MessageSh.getTemplateColumnNames());
	getMsgTable();
	// LOGS tab
	tab = getTable(tabs, logsTabName);
	if (tab == null)
		tab = FTOperation.insertTable(tableFromType(LogRecordSh.class, logsTabName, acc, 0));
	else syncSchema(tab, LogRecordSh.class);
	Memo.save(tab);
	getLogsTable();
	// BLOCKED tab
	tab = getTable(tabs, blockedTabName);
	if (tab == null)
		tab = FTOperation.insertTable(tableFromType(BlockedSh.class, blockedTabName, acc, 0));
	else syncSchema(tab, BlockedSh.class);
	Memo.save(tab);
	getBlockedTable();
	// CONFIG tab
	tab = getTable(tabs, cfgTabName);
	if (tab == null)
		tab = FTOperation.insertTable(tableFromType(ConfigSh.class, cfgTabName, acc, 0));
	else syncSchema(tab, ConfigSh.class);
	Memo.save(tab);
	getCfgTable();
	loadConfig();
	//
	log.info("    [initRootAccount]  time_ms:" + Tool.getTimer(true) + "  tables num:" + (tabs == null ? "null" : tabs.length));
}

// ===================================================
//-------------------------------------------------------------------
private static FTTable getTable(FTTable[] tabs, String tName) {
	for (FTTable tab : tabs) if (tName.equals(tab.name)) return tab;
	return null;
}

// -------------------------------------------------------------------
public static <T extends ObjectSh> void syncSchema(FTTable tab, Class<T> typeCls) {
	try {
		// remove _x_ columns 
		for (FTColumn tCol : tab.columns) {
			if (tCol.name.startsWith("_x_"))
				try { FTOperation.deleteColumn(tab, tCol.name);} catch (Throwable ex) {
					log.warning("Can't remove old column: " + tCol.name + "  Error: " + ex);
				}
		}
		// insert new columns
		List<Column> columns = ObjectSh.getSchema(typeCls).ftColumns;
		for (Column srcCol : columns) {
			boolean found = false;
			for (FTColumn tCol : tab.columns) if (tCol.name.equals(srcCol.localName)) {found = true; break;}
			if (!found) FTOperation.insertColumn(tab, new FTColumn(srcCol.localName, srcCol.fttype.name()));
		}
	} catch (Throwable ex) {log.severe(Tool.stackTrace(ex));}
}
// -------------------------------------------------------------------
public static FTTable createView(Account acc, FTTable baseTab, String vName, List<FTColumn> columns, String filter, long expires, boolean deleteData) throws FTException {
	if (columns == null) columns = baseTab.getColumns(false);
	//
	FTTable view = new FTTable(vName, baseTab, columns.toArray(new FTColumn[0]), filter);
	view.account = acc;
	FTTableDescription descr = baseTab.getDescription();
	view.setDescription(descr.objectType, expires, deleteData);
	// created without Description
	FTOperation.createView(view);
	// create Description
	FTOperation.updateTable(view);
	return view;
}

//-------------------------------------------------------------------
public static <T extends ObjectSh> FTTable assignTable(Account acc, Class<T> clas, String tName) throws RMIException {
	FTTable tab = null;
	FTTable[] tables = FTOperation.listTables(acc);
	for (FTTable t : tables) {
		if (t.name.equals(tName)) {tab = t; break;}
	}
	//
	if (tab == null) {
		tab = tableFromType(clas, tName, acc, 0);
		tab = FTOperation.insertTable(tab);
		log.info("    [CREATED TAB] :" + tName + "   in account:" + acc.name() + "  isOk:" + (tab != null));
	}
	//
	if (tab != null && Tool.notEmpty(tab.tableId)) Memo.save(tab);
	else log.warning("    [ASSIGN TAB Failed] tab:" + tName);
	return tab;
}
//-------------------------------------------------------------------
static FTTable getTableFromMemo(String name, Account acc) throws RMIException {
	FTTable tab = null;
	try { tab = Memo.get(FTTable.class, FTTable.generateStorableId(name, acc));} catch (Throwable ex) {}
	if (tab != null) tab.account = acc;
	return tab;
}
//-------------------------------------------------------------------
public static <T extends ObjectSh> FTTable tableFromType(Class<T> typeCls, String tabName, Account acc, long expires) {
	FTTable tab = new FTTable();
	tab.name = tabName;
	tab.account = acc;
//
	List<Column> columns = ObjectSh.getSchema(typeCls).ftColumns;
	List<FTColumn> cList = new ArrayList<FTColumn>();
	for (Column column : columns) {
		if (column.name.equals(Column.RowidColumn.NAME)) continue;
		FTColumn col = new FTColumn();
		col.name = column.localName;
		col.type = column.fttype.name();
		cList.add(col);
	}
	tab.columns = cList.toArray(new FTColumn[0]);
//
	tab.setDescription(typeCls.getSimpleName(), expires, false);
//
	return tab;
}






/** SPECIFIC TABLES */

//-------------------------------------------------------------------
public static FTTable getMsgTable() throws RMIException {
	if (msgTable != null) return msgTable;
	Account acc = Account.ROOT;
	FTTable tab = getTableFromMemo(msgTabName, acc);
	if (tab == null || Tool.isEmpty(tab.tableId)) tab = assignTable(acc, MessageSh.class, msgTabName);
	msgTable = tab;
	return tab;
}

public static FTTable getLogsTable() throws RMIException {
	if (logsTable != null) return logsTable;
	Account acc = Account.ROOT;
	FTTable tab = getTableFromMemo(logsTabName, acc);
	if (tab == null || Tool.isEmpty(tab.tableId)) tab = assignTable(acc, LogRecordSh.class, logsTabName);
	logsTable = tab;
	return tab;
}

public static FTTable getCfgTable() throws RMIException {
	if (cfgTable != null) return cfgTable;
	Account acc = Account.ROOT;
	FTTable tab = getTableFromMemo(cfgTabName, acc);
	if (tab == null || Tool.isEmpty(tab.tableId)) tab = assignTable(acc, ConfigSh.class, cfgTabName);
	cfgTable = tab;
	return tab;
}
public static ConfigSh getConfig() {
	try {
		ConfigSh cfg = Memo.get(ConfigSh.class, ConfigSh.KEY);
		return cfg != null ? cfg : loadConfig();
	} catch (Exception ex) {log.severe("[getConfig]: " + Tool.stackTrace(ex)); return null;}
}
public static ConfigSh loadConfig() {
	try {
		FTTable tab = getCfgTable();
		List<ConfigSh> list = FTOperation.selectRows(ConfigSh.class, tab, null, null, null, false, 0, 10000);
		if (Tool.isEmpty(list)) throw new Exception("Empty config table");
		ConfigSh cfg = list.get(0);
		if (cfg != null && cfg.operators != null) cfg.operators = cfg.operators.replaceAll(" ", "");
		Memo.save(cfg);
		MessageManager.onConfigUpdate(cfg);
		return cfg;
	} catch (Exception ex) {log.severe("[loadConfig]: " + Tool.stackTrace(ex)); return null;}
}


public static FTTable getBlockedTable() throws RMIException {
	if (blockedTable != null) return blockedTable;
	Account acc = Account.ROOT;
	FTTable tab = getTableFromMemo(blockedTabName, acc);
	if (tab == null || Tool.isEmpty(tab.tableId)) tab = assignTable(acc, BlockedSh.class, blockedTabName);
	blockedTable = tab;
	return tab;
}


}
