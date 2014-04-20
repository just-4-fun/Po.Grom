package cyua.android.client;

import android.annotation.TargetApi;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

import cyua.android.core.inet.InetRequest;
import cyua.android.core.inet.InetService;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiService;
import cyua.java.shared.objects.MessageSh;

import static cyua.android.core.AppCore.D;


/**
 Created by Marvell on 3/6/14.
 */
class GeocodeResponse {
private static final String TAG = "GeocodeResponse";
static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json?";//parameters



// Using native Android API
public static void assignGeocode() {
	new InetService.InetSequence() {
		@Override protected void doInBackground() throws Exception {
			UiState stt = (UiState) UiService.getUiState();
			String[] geodata = geocode(stt.mapLat, stt.mapLng);
			stt.region = geodata[0];
			stt.city = geodata[1];
			stt.address = geodata[2];
		}
		@Override protected void onSuccess() throws Exception {
			new UiCore.UiAction(Ui.UiOp.UPDATE_PAGE).execute();
		}
	};
}

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public static String[] geocode(double lat, double lng) {
	String[] geodata = new String[3];
	if (lat == 0) return geodata;
	try {
		boolean isPresent = App.apiVersion < 9 || Geocoder.isPresent();
		Geocoder gc = new Geocoder(App.context(), new Locale("uk"));
		List<Address> addresses = gc.getFromLocation(lat, lng, 2);
		if (addresses.isEmpty())
			throw new Exception("Can not geocode message lat= " + lat + "; lng= " + lng + ";  geocode is ? " + isPresent);
		String country = null, region = null, city = null, address = null, tmp = null;
		for (int $ = addresses.size() - 1; $ >= 0; $--) {
			Address adr = addresses.get($);
			tmp = adr.getCountryName();
			if (tmp != null) country = adr.getCountryName();
			tmp = adr.getAdminArea();
			if (tmp != null) region = tmp;
			tmp = adr.getLocality();
			if (tmp != null) city = tmp;
			if (adr.getMaxAddressLineIndex() > 0) address = adr.getAddressLine(0);
		}
		if (country != null && !country.equals("Україна")) geodata[0] = country;
		else {geodata[0] = region; geodata[1] = city; geodata[2] = address;}
/*
		Address adr = addresses.get(0);
		StringBuilder str = new StringBuilder();
		for (int $ = 0; $ < adr.getMaxAddressLineIndex(); $++) {
			str.append("AddressLine"+$+"=").append(adr.getAddressLine($)).append("\n");
		}
		str.append("CountryCode").append(adr.getCountryCode()).append("\n")
				.append("CountryName").append(adr.getCountryName()).append("\n")
				.append("AdminArea=").append(adr.getAdminArea()).append("\n")
				.append("SubAdminArea").append(adr.getSubAdminArea()).append("\n")
				.append("Locality").append(adr.getLocality()).append("\n")
				.append("SubLocality").append(adr.getSubLocality()).append("\n")
				.append("Thoroughfare").append(adr.getThoroughfare()).append("\n")
				.append("SubThoroughfare").append(adr.getSubThoroughfare()).append("\n")
				.append("FeatureName").append(adr.getFeatureName()).append("\n")
				.append("Phone").append(adr.getPhone()).append("\n")
				.append("PostalCode").append(adr.getPostalCode()).append("\n")
				.append("Premises").append(adr.getPremises()).append("\n")
				.append("Url").append(adr.getUrl()).append("\n")
				.append("Extras").append(adr.getExtras()).append("\n");
		if (D) Wow.v(TAG, "geocodeMessaga", str.toString());
*/
	} catch (Exception ex) {Wow.e(ex);}
	return geodata;
}



// CODE MIRRORS SAME SERVER CODE
public static String[] geocode_2(double lat, double lng) {
	String[] geodata = new String[3];
	if (lat == 0) return geodata;
	String url = GeocodeResponse.getUrl(lat, lng);
	InetRequest.Options opts = new InetRequest.Options(url)
			.maxAttempts(4)
			.maxDuration(10000)
			.method(InetRequest.Method.GET)
			.contentType(InetRequest.ContentType.DEFAULT);
	InetRequest<String> request = new InetRequest<String>(String.class, opts);
	if (D) Wow.v(TAG, "assignGeocode", "exec started ... url = " + url);
	String result = request.execute();
	if ((Tool.notEmpty(result))) {
		GeocodeResponse geoRes = new Gson().fromJson(result, GeocodeResponse.class);
		if ("OK".equals(geoRes.status)) {
			geodata = geoRes.getRegionCityAddress();
			if (D) Wow.v(TAG, "geocode", "Geo = " + Tool.join(geodata, ", "), "data=" + geoRes);
		}
		else if (D) Wow.v(TAG, "geocode", "failed with status " + geoRes.status);
	}
	return geodata;
}

static String getUrl(double lat, double lng) {
	StringBuilder url = new StringBuilder(GEOCODE_URL)
			.append("key=").append(App.SERVER_KEY).append('&')
			.append("latlng=").append(lat).append(",").append(lng).append('&')
			.append("sensor=true").append('&')
			.append("language=uk").append('&')
			.append("result_type=street_address|route|sublocality|locality|administrative_area_level_1|administrative_area_level_2");//.append('&')
//			.append("location_type=ROOFTOP|RANGE_INTERPOLATED");//GEOMETRIC_CENTER|APPROXIMATE
	return url.toString();
}



/** INSTANCE API */

// OK, ZERO_RESULTS, OVER_QUERY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, UNKNOWN_ERROR
String status;
GeoAddress[] results;

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

}
