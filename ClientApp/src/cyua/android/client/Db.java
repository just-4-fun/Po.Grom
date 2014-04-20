package cyua.android.client;

import cyua.android.core.db.DbCore;
import cyua.android.core.location.Fix;
import cyua.android.core.log.Wow;
import cyua.android.core.map.MarkerObject;
import cyua.android.core.map.MarkerTable;

import static cyua.android.core.location.Fix.FixTable;


public class Db extends DbCore
{

public static MarkerTable markerDbTable() {
	return (MarkerTable) getTable(MarkerObject.class);
}
public static FixTable fixDbTable() {
	return (FixTable) getTable(Fix.class);
}
//public static Wow.LogsTable logsTable() {
//	return (Wow.LogsTable) getTable(Wow.LogRecord.class);
//}





@Override protected void onInited() {
	registerTable(MarkerTable.class);
	registerTable(FixTable.class);
}
}
