package cyua.gae.appserver.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import cyua.gae.appserver.Tool;
import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;

public class FTRows
{
static final Logger log = Logger.getLogger(FTRows.class.getName());
static final String ROWID = "rowid"; 

public String[] columns;
public Object[][] rows;


//-------------------------------------------------------------------
public boolean isEmpty()
{
	return rows == null || rows.length == 0;
}
//-------------------------------------------------------------------
public int length()
{
	return rows == null ? 0 : rows.length;
}
//-------------------------------------------------------------------
String getFirstValue()
{
	return Tool.isEmpty(rows) || rows[0].length == 0 ? null : rows[0][0].toString();
}
//-------------------------------------------------------------------
String[] getInsertedRowIds()
{
	if (Tool.isEmpty(rows) || rows[0].length == 0) return null;
	//
	String[] rowids = new String[rows.length];
	for (int $ = 0; $ < rows.length; $++)
	{
		rowids[$] = rows[$][0].toString();
	}
	return rowids;
}
// -------------------------------------------------------------------
<T extends ObjectSh> List<T> toList(Class<T> cls)
{
	List<T> list = new ArrayList<T>();
	if (rows == null) return list;
	Column[] cols = ObjectSh.getFTColumns(cls, columns);
	// create list of objects
	for (Object[] row : rows)
	{
		T tobj = null;
		try {tobj = cls.newInstance();} catch (Throwable ex) {break;}
		list.add(tobj);
		//
		for (int $ = 0; $ < columns.length; $++)
		{
			try
			{
				Object value = row[$];
				if (value == null) continue;
				Column col = cols[$];
				value = FTUtils.convertFromFT(value, col.objField.getType());
				if (value == null) continue;
				col.set(tobj, value);
			} catch (Throwable ex) {}
		}
	}
	return list;
}


// -------------------------------------------------------------------
public int indexOf(String column)
{
	for (int $ = 0; $ < columns.length; $++)
	{
		if (column.equals(columns[$])) return $;
	}
	return -1;
}

public Object[][] subset(int fromIx, int toIx)
{
	if (fromIx == 0 && toIx >= rows.length) return rows;
	//
	if (toIx > rows.length) toIx = rows.length;
	Object[][] subset = new Object[toIx - fromIx][];
	System.arraycopy(rows, fromIx, subset, 0, subset.length);
	return subset;
}
}
