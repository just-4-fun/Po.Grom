package cyua.java.shared.objects;

import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;


/**
 Created by Marvell on 3/6/14.
 */
public class ConfigSh extends ObjectSh {
private static final String TAG = "cyua.ConfigSh";
public static final String KEY = "base";
@Override public String getStorableID() {
	return KEY;
}
@Override public boolean isCacheable() {
	return true;
}

// ===================================================
public static final Column ROWID = new Column.RowidColumn();
public String rowid;
public static final Column OPERATORS = new Column(100).localName("Телефони SMSS (через кому)");
public String operators;


}
