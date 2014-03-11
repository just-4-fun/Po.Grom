package cyua.android.core.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cyua.android.core.db.DbCore;
import cyua.android.core.db.DbTable;

public class MarkerTable extends DbTable<MarkerObject>
{
private static final String TAG = MarkerTable.class.getSimpleName();
//
private static MarkerTable I;
//


public static MarkerTable registerType(MarkerType typeInst)
{
	if (I == null) DbCore.registerTable(MarkerTable.class);
	I.types.put(typeInst.name, typeInst);
	return I;
}


static MarkerTable onMapCreate(MarkerLayer layer)
{
	if (I == null) DbCore.registerTable(MarkerTable.class);
	I.layer = layer;
	for (MarkerType type : I.types.values()) type.onMapCreate(layer);
	return I;
}
static void onMapDestroy()
{
	if (I == null) return;// allready onExit called
	for (MarkerType type : I.types.values()) type.onMapDestroy();
	I.layer = null;
}





/** INSTANCE */

private MarkerLayer layer;
protected Map<String, MarkerType> types = new HashMap<String, MarkerType>();


public MarkerTable() throws Exception
{
	super();
	I = this;
}
@Override protected void onExit()
{
	I = null;
}

@Override public String tableName() {return "markers";}

MarkerType getType(String typeName)
{
	return types.get(typeName);
}

protected MarkerObject findByRef(Long ref, String type)
{
	String where = MarkerObject.REF+"="+ref+" AND "+MarkerObject.TYPE+"='"+type+"'";
	List<MarkerObject> list = select(where, null, 1);
	if (!list.isEmpty()) return list.get(0);
	return null;
}

public boolean insertMarker(Long ref, String type, Double lat, Double lng, String title, String snippet, Double state)
{
	MarkerObject mo = new MarkerObject(ref, type, lat, lng, title, snippet, state);
	if (insert(mo)) {
		if (layer != null) layer.addToList(mo);
		return true;
	}
	return false;
}
public boolean updateMarker(Long ref, String type, Double lat, Double lng, String title, String snippet, Double state)
{
	MarkerObject mo = layer == null ? findByRef(ref, type) : layer.findByRef(ref, type);
	if (mo == null) return insertMarker(ref, type, lat, lng, title, snippet, state);
	mo.update(lat, lng, title, snippet, state);
	if (state != null && layer != null) {
		layer.removeFromList(mo);
		layer.addToList(mo);
	}
	return update(mo);
}
public boolean deleteMarker(Long ref, String type)
{
	MarkerObject mo = layer == null ? findByRef(ref, type) : layer.findByRef(ref, type);
	if (mo != null && delete(mo)) {
		if (layer != null) layer.removeFromList(mo);
		return true;
	}
	return false;
}
}