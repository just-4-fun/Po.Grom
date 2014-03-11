package cyua.java.shared.objects;

import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;


/** RECORD */

public class LogRecordSh extends ObjectSh {
	public static final Column ROWID = new Column.RowidColumn();
	public String rowid;
	public static final Column DATETIME = new Column(10).notSql();
	public long datetime;
	public static final Column MONTH = new Column(14).notSql();
	public String month;
	public static final Column DATE = new Column(20).notSql();
	public String date;
	public static final Column TIME = new Column(30).notSql();
	public String time;
	public static final Column PLACE = new Column(100).notSql();
	public String place;
	public static final Column EXCEPTION = new Column(120).notSql();
	public String exception;
	public static final Column MESSAGE = new Column(130).notSql();
	public String message;
	public static final Column DEVICE = new Column(200).notSql();
	public String device;
	public static final Column DEVICEINFO = new Column(210).notSql();
	public String deviceInfo;
	public static final Column APPVERSION = new Column(220).notSql();
	public String appVersion;
	public static final Column APIVERSION = new Column(230).notSql();
	public String apiVersion;
	public static final Column APPSTATE = new Column(240).notSql();
	public String appState;


	@Override public String getStorableID() {
		return null;
	}
	@Override public boolean isCacheable() {
		return false;
	}
}
