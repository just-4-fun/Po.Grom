package cyua.gae.appserver.fusion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import cyua.gae.appserver.Tool;
import cyua.gae.appserver.fusion.FTStyle.IconStyler;
import cyua.gae.appserver.fusion.FTStyle.MarkerOptions;
import cyua.gae.appserver.fusion.FTStyle.PolylineOptions;
import cyua.gae.appserver.fusion.FTStyle.StrokeColorStyler;
import cyua.gae.appserver.urlfetch.Account;
import cyua.java.shared.RMIException;
import cyua.java.shared.ObjectSh;
import cyua.java.shared.Column;



public class FTTable extends ObjectSh
{

static final Logger log = Logger.getLogger(FTTable.class.getName());
//TODO RELEASE: 7-10 DAYS	/ 13 MONTHS (year+month)
static final int expireDays = 7;// ! XXX!XXX: positive
static final int expireMonths = 14;// ! XXX!XXX: positive
//XXX!XXX sync with session view name format
static Pattern sessPtt = Pattern.compile("(\\d+).(\\d+) \\((\\d+):(\\d+)\\) (.*)\\z");
//
public static final Column TABLEID = new Column(10);
public String tableId;// from FT
public static final Column NAME = new Column(10);
public String name;// from FT
public static final Column COLUMNS = new Column(10);
public FTColumn[] columns;// from FT
public static final Column DESCRIPTION = new Column(10);
public String description;// from FT
public static final Column BASETABLEIDS = new Column(10);
public String[] baseTableIds;// from FT (Optional base table identifiers if this table is a view or merged table.)
public static final Column ISEXPORTABLE = new Column(10);
public Boolean isExportable;// from FT
// attribution": string
// attributionLink": string
transient FTTable baseTable;
transient String filter;
public transient Account account;
//
//-------------------------------------------------------------------
public FTTable()
{
	isExportable = true;
// No-args public constructor is required by Gson and Serialization
}
//-------------------------------------------------------------------
FTTable(String _name, String _id)
{
isExportable = true;
tableId = _id; name = _name;
}
// -------------------------------------------------------------------
FTTable(String _name, FTColumn[] _columns)
{
isExportable = true;
name = _name; columns = _columns;
}
// -------------------------------------------------------------------
FTTable(String _name, FTTable _baseTab, FTColumn[] _columns, String _filter)
{
this(_name, _columns);
baseTable = _baseTab; filter = _filter;
}


//-------------------------------------------------------------------
public boolean isView()
{
	return baseTable != null || !Tool.isEmpty(baseTableIds);
}
// -------------------------------------------------------------------
public <T extends ObjectSh> void setDescription(String objType, long expires, boolean deleteData)
{
	FTTableDescription descr = new FTTableDescription();
	descr.objectType = objType;
	descr.created = Tool.now();
	if (expires > 0)
	{
		descr.expires = expires;
		if (deleteData) descr.dd = deleteData;
	}
	description = new Gson().toJson(descr);
}
// -------------------------------------------------------------------
public FTTableDescription getDescription()
{
	FTTableDescription descr = null;
	if (description != null) try{descr = new Gson().fromJson(description, FTTableDescription.class);} catch(Throwable ex) {}
	if (descr == null) descr = new FTTableDescription();
	return descr;
}
//-------------------------------------------------------------------
public boolean isExpired()
{
FTTableDescription descr = getDescription();
Calendar testDate = Tool.nowDate();
testDate.add(Calendar.MONTH, -24);
return descr.expires > testDate.getTimeInMillis()
		&& descr.expires < Tool.now();
}
//-------------------------------------------------------------------
public boolean isDeleteData()
{
FTTableDescription descr = getDescription();
return isView() && descr.dd;
}
//
//// -------------------------------------------------------------------
//public boolean isDataExpired()
//{
//	if (expireMonths <= 0) return false;
//	GregorianCalendar date = null;
//	FTTableDescription descr = getDescription();
//	if (descr != null && descr.created > 0)
//	{
//		date = Tool.toDate(descr.created);
//	}
//	else// old format	// Ex: ������� 2012.02
//	{
//		// XXX!XXX sync with month view name format
//		Pattern mnthPtt = Pattern.compile(FTDB.msgTabName+" *(\\d+)\\.(\\d+)");
//		Matcher m = mnthPtt.matcher(name);
//		if (!m.find()) return false;
//		date = Tool.nowDate();
//		date.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
//		date.set(Calendar.MONTH, Integer.parseInt(m.group(2))-1);
//	}
//	date.add(Calendar.MONTH, expireMonths);
//	GregorianCalendar now = Tool.nowDate();
////Log.v(TAG, name+" [isDataExpired]: "+date.before(now));//String.format("%1$tY::%1$tm", date)+" delete:"+date.before(now));
//	return date.before(now);
//}


// ===================================================
// STATIC API
// -------------------------------------------------------------------
static String generateStorableId(String name, Account acc)
{
	return name+"#"+acc;
}

// ===================================================
// INSTANCE
//-------------------------------------------------------------------
public List<FTColumn> getColumns(boolean withRowid)
{
	List<FTColumn> list = new ArrayList<FTColumn>();
	boolean found = false;
	for (FTColumn col : columns)
	{
		if (col.name.equals(ObjectSh.ROWID))
		{
			found = true;
			if (!withRowid) continue;
		}
		list.add(col);
	}
	if (withRowid && !found)
	{
		FTColumn rowidCol = new FTColumn();
		rowidCol.name = ObjectSh.ROWID;
		list.add(0, rowidCol); 
	}
	return list;
}
// -------------------------------------------------------------------
public List<FTColumn> getColumn(Column... cols)
{
	// WARNING check that tab columns to match with iCols
	List<FTColumn> list = new ArrayList<FTColumn>();
	for (Column icol : cols)
	{
		FTColumn col = new FTColumn();
		col.name = icol.name;
		list.add(col);
	}
	return list;
}
//-------------------------------------------------------------------
public boolean hasColumn(String cName)
{
if (Tool.isEmpty(columns)) return false;
for (FTColumn col : columns) if (cName.equals(col.name)) return true;
return false;
}
//-------------------------------------------------------------------
public void updateDefaultStyle() throws RMIException
{
boolean hasMarker = hasColumn("marker"),
hasColor = hasColumn("color");
if (!hasMarker && !hasColor) return;
// else
FTStyle style = null;
boolean insert = false;
try { style = FTOperation.getStyle(this, "1"); } catch (Throwable ex) {insert = true;}
//log.info("[STYLE] "+Tool.printObject(style));
style = new FTStyle(this, "1", true);
if (hasMarker) style.markerOptions = new MarkerOptions(new FTColumn("marker", "STRING"));
if (hasColor) style.polylineOptions = new PolylineOptions(new FTColumn("color", "STRING"));
if (insert) FTOperation.insertStyle(this, style);
else FTOperation.updateStyle(this, style);
}
// -------------------------------------------------------------------
public void updateDefaultTemplate(String[] cols)
{
	try
	{
		FTTemplate tmp = null;
		try {tmp = FTOperation.getTemplate(this, "1");} catch (Throwable ex) {}
		//
		FTTemplate newTmp = new FTTemplate(this, "1", true, cols);
		if (tmp == null) FTOperation.insertTemplate(this, newTmp);
		else if (!tmp.equals(newTmp)) FTOperation.updateTemplate(this, newTmp);
	}
	catch (Throwable ex)
	{
		log.severe("Failed to update Template. "+ex);
	}
}


// ===================================================
//
static class FTTableDescription implements Serializable
{
private static final long serialVersionUID = -7465952105860784024L;
//
long expires;
boolean dd;// delete data
long created;
String objectType;// class simpleName
}




//===================================================
//STORABLE settings
private static final long serialVersionUID = -1829911299956532938L;
//-------------------------------------------------------------------
@Override
public String getStorableID() {
return generateStorableId(name, account);
}

@Override public boolean isCacheable() { return true; }

}
