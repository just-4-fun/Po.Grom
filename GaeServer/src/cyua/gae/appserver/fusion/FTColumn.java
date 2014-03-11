package cyua.gae.appserver.fusion;

import java.io.Serializable;

import cyua.java.shared.ObjectSh;
import cyua.java.shared.Column;


public class FTColumn extends ObjectSh
{
private static final long serialVersionUID = -1191976280999445044L;

public static final Column COLUMNID = new Column(10);
public String columnId;// from FT,
public static final Column NAME = new Column(10);
public String name;// from FT
public static final Column TYPE = new Column(10);
public String type;// from FT
public static final Column TABLEINDEX = new Column(10);
public String tableIndex;// from FT (baseColumn.tableIndex)
public static final Column BASECOLUMN = new Column(10);
public FTColumn baseColumn;// from FT (Optional identifier of the base column. If present, this column is derived from the specified base column.)

// ===================================================
// -------------------------------------------------------------------
public FTColumn()
{
// No args constructor is required by Gson
}
//-------------------------------------------------------------------
public FTColumn(String _nm, String _type)
{
name = _nm; type = _type.toUpperCase();
}
//-------------------------------------------------------------------
public FTColumn(String _nm)
{
name = _nm; type = Column.FtType.STRING.name();
}

// -------------------------------------------------------------------
@Override public String toString()
{
	return name;
}

@Override public String getStorableID() {
	return null;
}
@Override public boolean isCacheable() {
	return false;
}
}
