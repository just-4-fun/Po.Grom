package cyua.gae.appserver;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.gson.Gson;

import java.util.List;
import java.util.logging.Logger;

import cyua.gae.appserver.fusion.FTDB;
import cyua.gae.appserver.fusion.FTOperation;
import cyua.gae.appserver.fusion.FTTable;
import cyua.gae.appserver.memo.MCache;
import cyua.gae.appserver.memo.Memo;
import cyua.gae.appserver.urlfetch.HttpRequest;
import cyua.gae.appserver.urlfetch.HttpResponse;
import cyua.java.shared.BitState;
import cyua.java.shared.RMIException;
import cyua.java.shared.objects.ConfigSh;
import cyua.java.shared.objects.MessageSh;
import sun.misc.resources.Messages;

import static cyua.java.shared.objects.MessageSh.Flag;


public class MessageManager {
static final Logger log = Logger.getLogger(MessageManager.class.getName());

static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json?";//parameters

private static final String pointOpenTag = "<Point><coordinates>";
private static final String pointCloseTag = "</coordinates></Point>";
private static final String lineOpenTag = "<LineString><coordinates>";
private static final String lineCloseTag = "</coordinates></LineString>";
private static ConfigSh.Type[] types;

public static boolean save(String device_id, MessageSh msg) throws RMIException {
	BitState flags = new BitState(Tool.toInt(msg.flags));
	boolean realLcn = flags.has(Flag.LOCAT_AVAIL);
	String updateRowid = null;
	byte reliab = 0;
	// LOCATION and GEOCODE
	if (msg.lat != 0) {
		msg.location = pointOpenTag + msg.lng + "," + msg.lat + pointCloseTag;
		if (Tool.isEmpty(msg.region)) geocode(msg);
		if (!realLcn) msg.address = "востаннє зафіксовано";
		else reliab++;
	}
	else reliab--;
//	msg.marker = realLcn ? "donut" : "forbidden";
	// PHONE check
	if (msg.phone != null && msg.phone.startsWith("+038")) reliab++;
	msg.flags = reliab + "";
	// DATETIME
	long time = Tool.notEmpty(msg.datetime) ? Tool.toLong(msg.datetime) : 0;
	if (time == 0) time = Tool.now();
	msg.datetime = String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", time);
	msg.mnth = String.format("%1$tY%1$tm", time);
	// SOS
	if (Tool.safeEquals(msg.type, MessageSh.SOS_INDEX + "")) {
		msg.type = "http://s3-eu-west-1.amazonaws.com/pg-pub/ic_sos.png";
		msg.marker = MessageSh.SOS_MARKER;
		MessageSh oldMsg = MCache.getObject(MessageSh.class, msg.uid);
		if (oldMsg != null && oldMsg.rowid != null) {
			updateRowid = oldMsg.rowid;
			String oldText = Tool.safeEquals(msg.text, oldMsg.text) ? "" : oldMsg.text + "; ";
			String oldLinks = (Tool.isEmpty(oldMsg.photolink1) ? "" : oldMsg.photolink1) +
					(Tool.isEmpty(oldMsg.photolink2) ? "" : " " + oldMsg.photolink2) +
					(Tool.isEmpty(oldMsg.photolink3) ? "" : " " + oldMsg.photolink3) +
					(Tool.isEmpty(oldMsg.photolink4) ? "" : " " + oldMsg.photolink4);
			msg.extra = oldMsg.extra + "\n[" + oldMsg.datetime + "; " + oldText + oldLinks + "]";
			msg.location = joinLocations(oldMsg.location, msg.lat, msg.lng, msg.location);
		}
	}
	else {
		ConfigSh.Type tp = getTypeByIndex(msg.type);
		if (tp != null) {
			msg.type = tp.name;
			msg.marker = tp.marker;
		}
	}
	//
	FTTable tab = FTDB.getMsgTable();
	if (updateRowid != null) {
		try {
			FTOperation.updateRow(tab, msg, updateRowid);
		} catch (Exception ex) {updateRowid = null;}// row not found
	}
	if (updateRowid == null) updateRowid = FTOperation.insertRow(tab, msg);
	if (updateRowid != null) {
		msg.rowid = updateRowid;
		boolean saved = MCache.saveObject(msg.uid, msg, 60 * 60);
	}
	return true;
}
private static String joinLocations(String oldLcn, Double lat, Double lng, String newLcn) {
	try {
		if (Tool.isEmpty(oldLcn)) throw new Exception();
		String tag0 = "<coordinates>", tag1 = "</coordinates>";
		int ix0 = oldLcn.indexOf(tag0);
		if (ix0 < 0) throw new Exception();
		int ix1 = oldLcn.indexOf(tag1);
		if (ix0 < 0) throw new Exception();
		String coords = oldLcn.substring(ix0 + tag0.length(), ix1);
		newLcn = lineOpenTag + coords + " " + lng + "," + lat + lineCloseTag;
	} catch (Exception ex) {}
	return newLcn;
}
/*
https://developers.google.com/maps/documentation/geocoding/#ReverseGeocoding
https://developers.google.com/maps/documentation/geocoding/#Types
 */
// CODE MIRRORS SAME CLENT CODE
static private void geocode(MessageSh msg) {
	if (msg.lat == 0) return;
	try {
		String url = GeocodeResponse.getUrl(msg.lat, msg.lng);
		HTTPMethod meth = HTTPMethod.GET;
		HttpRequest.ContentType contType = HttpRequest.ContentType.DEFAULT;
		HttpResponse response = HttpRequest.exec(url.toString(), meth, null, null, null, 5000, contType);
		HttpResponse.RsStatus status = response.status;
		int code = response.getCode();
		String result = response.asText();
		if (status == HttpResponse.RsStatus.OK) {
			GeocodeResponse geoRes = new Gson().fromJson(result, GeocodeResponse.class);
			if ("OK".equals(geoRes.status)) {
				String[] geodata = geoRes.getRegionCityAddress();
				msg.region = geodata[0];
				msg.city = geodata[1];
				msg.address = geodata[2];
			}
			else log.warning("geocode responsefailed with status " + geoRes.status);
		}
		else log.warning("geocode request failed with code " + code);
	} catch (Exception ex) {log.severe("[geocode]: " + Tool.stackTrace(ex));}
}



static class GeocodeResponse {
	// OK, ZERO_RESULTS, OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, UNKNOWN_ERROR
	String status;
	GeoAddress[] results;
	static String getUrl(double lat, double lng) {
		StringBuilder url = new StringBuilder(GEOCODE_URL)
				.append("key=").append(App.serverApiKey).append('&')
				.append("latlng=").append(lat).append(",").append(lng).append('&')
				.append("sensor=true").append('&')
				.append("language=uk").append('&')//uk
				.append("result_type=street_address|route|sublocality|locality|administrative_area_level_1|administrative_area_level_2");//.append('&')
//			.append("location_type=ROOFTOP|RANGE_INTERPOLATED");//GEOMETRIC_CENTER|APPROXIMATE
		return url.toString();
	}
	public String[] getRegionCityAddress() {
		String country = null, region = null, city = null, street = null, number = null;
		if ((Tool.notEmpty(results))) {
			for (int $ = results.length - 1; $ >= 0; $--) {
				GeoAddress g = results[$];
				for (AddressComponent adr : g.address_components) {
					for (String type : adr.types) {
						if ("country".equals(type)) country = adr.long_name;
						else if ("administrative_area_level_1".equals(type)) region = adr.long_name;
						else if ("locality".equals(type)) city = adr.long_name;
						else if ("route".equals(type)) street = adr.long_name;
						else if ("street_number".equals(type)) number = adr.long_name;
					}
				}
			}
		}
		if (country != null && !country.equals("Україна")) return new String[]{country, null, null};
		return new String[]{region, city, street + (number == null ? "" : " " + number)};
	}
	@Override public String toString() {
		StringBuilder str = new StringBuilder("Status=" + status + "\n");
		if ((Tool.notEmpty(results))) {
			for (GeoAddress g : results) {
				str.append("\n\n");
				str.append("____ADDRESS=").append(g.formatted_address).append("\n")
						.append("____result_types=").append(Tool.join(g.types, ", ")).append("\n");
				for (AddressComponent adr : g.address_components) {
					str.append("__________COMPO_types=").append(Tool.join(adr.types, ", ")).append("\n")
							.append("____________________longName=").append(adr.long_name).append("; ")
							.append("shortName=").append(adr.short_name).append("\n");
				}
			}
		}
		return str.toString();
	}
}

static class GeoAddress {
	String[] types;// locality, political
	String formatted_address;//human-readable address
	AddressComponent[] address_components;
}

private static class AddressComponent {
	String[] types;// locality, political
	String long_name;
	String short_name;
	// etc ex geometry
}



public static void onConfigUpdate(ConfigSh cfg) {
	if (cfg.types == null) cfg.types = "[]";
	if (!cfg.types.startsWith("[")) cfg.types = "[" + cfg.types + "]";
	types = new Gson().fromJson(cfg.types, ConfigSh.Type[].class);
	MCache.saveValue(MCache.CacheKeys.CFG_TYPES, types);
}
static ConfigSh.Type getTypeByIndex(String ix) {
	try {
		if (types == null) types = MCache.getValue(MCache.CacheKeys.CFG_TYPES);
		if (types == null) FTDB.loadConfig();
		if (types == null) return null;
		for (ConfigSh.Type tp : types) {
			if (String.valueOf(tp.index).equals(ix)) return tp;
		}
	} catch (Exception ex) {log.severe("[getTypeByIndex]: " + Tool.stackTrace(ex));}
	return null;
}


}
