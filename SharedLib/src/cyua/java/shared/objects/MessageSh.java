package cyua.java.shared.objects;

import cyua.java.shared.BaseTool;
import cyua.java.shared.BitState;
import cyua.java.shared.Phantom;
import cyua.java.shared.Column;
import cyua.java.shared.ObjectSh;
import cyua.java.shared.SmsUtils;

import static cyua.java.shared.BaseTool.log;
import static cyua.java.shared.BaseTool.toDouble;
import static cyua.java.shared.BaseTool.toLong;
import static cyua.java.shared.Column.FtType;
import static cyua.java.shared.Column.RowidColumn;


/**
 Created by Marvell on 1/30/14.
 */
public class MessageSh extends ObjectSh {
private static final String TAG = "cyua.MessageSh";
@Override public String getStorableID() {
	return uid +"-"+_id;// TODO
}
@Override public boolean isCacheable() {
	return false;
}

// ===================================================
// IDs
public static final Column ROWID = new RowidColumn();//.localName("№ запису");
public String rowid;
public static final Column NOTES = new Column(110).localName("Позначка");
public String notes;
public static final Column UID = new Column(120).localName("Ід.Користувача");
public String uid;
public static final Column USER = new Column(130).localName("Користувач");
public String user;
public static final Column PHONE = new Column(140).localName("Тел.номер");
public String phone;
public static final Column EXTRA = new Column(150).localName("Додаткова Інфо");
public String extra;
// TIME
public static final Column DATETIME = new Column(210).localName("Час").fttype(FtType.DATETIME);
public String datetime;
// PLACE
public static final Column LOCATION = new Column(310).fttype(FtType.LOCATION).localName("Місце");
public String location;
public static final Column REGION = new Column(320).localName("Область");
public String region;
public static final Column CITY = new Column(322).localName("Місто");
public String city;
public static final Column ADDRESS = new Column(324).localName("Адреса");
public String address;
public static final Column LAT = new Column(1310);
public Double lat;
public static final Column LNG = new Column(1321);
public Double lng;
// STATUS on MAP
public static final Column MARKER = new Column(1360);
public String marker;
public static final Column COLOR = new Column(1361);
public String color;
// STATUS
public static final Column RELIAB = new Column(510).localName("Надійнисть");
public String reliab;
// FILTERS
//public static final Column ACTUAL = new Column(610);
//public String actual;
//public static final Column CHAN1 = new Column(620);
//public Integer chan1;
// CONTENT
public static final Column FLAGS = new Column(410).localName("Тип");
public String flags;
public static final Column TEXT = new Column(412).localName("Текст");
public String text;
public static final Column PHOTO1 = new Column(420).localName("Фото1");
public String photo1;
public static final Column PHOTO2 = new Column(422).localName("Фото2");
public String photo2;
public static final Column PHOTO3 = new Column(424).localName("Фото3");
public String photo3;
public static final Column PHOTO4 = new Column(426).localName("Фото4");
public String photo4;
public static final Column PHOTOLINK1 = new Column(430).localName("Лінк1");
public String photolink1;
public static final Column PHOTOLINK2 = new Column(432).localName("Лінк2");
public String photolink2;
public static final Column PHOTOLINK3 = new Column(434).localName("Лінк3");
public String photolink3;
public static final Column PHOTOLINK4 = new Column(436).localName("Лінк4");
public String photolink4;
//public static final Column VIDEO = new Column(440);
//public String video;
public static final Column SMSS = new Column(2000);
public String smss;
public static final Column MNTH = new Column(2010).localName("sys1");// to quick remove view by month
public String mnth;

//=========================================

//public static String __ = "|", REPLFROM = "\\|", REPLTO = ";";
//public static String __ = "\r", REPLFROM = "\r", REPLTO = "\n";
public static String __ = "¡", REPLFROM = __, REPLTO = ";";
public static enum Flag {LOCAT_AVAIL, SOS}

public static String[] getTemplateColumnNames() {
	return new String[]{
			USER.localName,
			TEXT.localName
	};
}

/** WARN: do not change order ! */
public static Column[] getSmsCols() {
	return new Column[]{
			_ID, UID, USER, PHONE, LAT, LNG, LOCATION, TEXT // other columns may follow
	};
}

public static MessageSh create(long id, String uid, String user, String phone, String text, double lat, double lng, boolean actualLcn, boolean sos) {
	MessageSh m = new MessageSh();
	m._id = id; m.uid = uid; m.user = user; m.phone = phone;
	m.lat = lat; m.lng = lng;
	m.text = text;
	//
	BitState flags = new BitState();
	if (sos) flags.set(Flag.SOS);
	if (actualLcn) flags.set(Flag.LOCAT_AVAIL);
	m.flags = flags.getValue() + "";
	return m;
}

public static MessageSh fromSms(String sms) {
	MessageSh m = new MessageSh();
	try {
		int ix = sms.indexOf(REPLFROM);
		if (ix < 0) throw new Exception("Wrong format");
		// check signature
		String srcsign = sms.substring(0, ix);
		String sign = Phantom.signPayload(sms.substring(ix), 6);
		if (!BaseTool.safeEquals(srcsign, sign)) throw new Exception("Wrong sign");
		String[] p = sms.split(REPLFROM, -1);// WARN: it adds one empty element at end
//		int encType = toInt(p[0]);
		m._id = toLong(p[1]);
		m.uid = p[2];
		m.user = SmsUtils.decodeCyr(p[3]);
		m.phone = p[4];
		m.text = SmsUtils.decodeCyr(p[5]);
		m.flags = p[6];
		m.lat = toDouble(p[7]);
		m.lng = toDouble(p[8]);
		return m;
	} catch (Exception ex) {
		m.uid = null;
		log(TAG, ex.toString());
	}
	return m;
}

public static String toSms(long id, String uid, String user, String phone, String text, double lat, double lng, boolean actualLcn, boolean sos) {
	BitState flags = new BitState();
	if (sos) flags.set(Flag.SOS);
	if (actualLcn) flags.set(Flag.LOCAT_AVAIL);
	text = text.replaceAll(REPLFROM, REPLTO);
	text = SmsUtils.encodeCyr(text);
	user = SmsUtils.encodeCyr(user);
	String sms = __ + id + __ + uid + __ + user + __ + phone + __ + text + __ + flags.getValue() + __ + lat + __ + lng + __;
	// set signature
	String sign = Phantom.signPayload(sms, 6);
	sms = sign + sms;
//	log(TAG, " toSms > len = "+len+"; enclen = "+enclen);
	return sms;
}



/** INSTANCE API */

public MessageSh() {
	// for Serialization
}


}

