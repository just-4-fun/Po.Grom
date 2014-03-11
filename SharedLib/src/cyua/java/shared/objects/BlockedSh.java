package cyua.java.shared.objects;

import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;


/**
 Created by Marvell on 3/6/14.
 */
public class BlockedSh extends ObjectSh {
private static final String TAG = "cyua.BlockedSh";
@Override public String getStorableID() {
	return uid;
}
@Override public boolean isCacheable() {
	return true;
}

// ===================================================
public static final Column ROWID = new Column.RowidColumn();
public String rowid;
public static final Column UID = new Column(100).localName("Ідент.користувача");
public String uid;
public static final Column COMMENT = new Column(100).localName("Коментар");
public String comment;
public static final Column DATETIME = new Column(100).localName("Дата");
public String datetime;


}
