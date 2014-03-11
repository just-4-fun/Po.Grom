package cyua.android.core.inet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cyua.android.core.log.Wow;
import cyua.java.shared.Phantom;
import cyua.java.shared.Rmi;

import static cyua.android.core.AppCore.D;


public class RmiUtils {
private static final String TAG = "RmiUtils";


public static void rmiRequest(Rmi rmi, String url, Class<? extends Rmi.RmiResponse> responseClas) {
	String rmiName = rmi.getName();
	url += "/" + rmiName;
	JsonObject jobj = rmi.getJsonRequest();
	// encrypt header
	String payload = new Gson().toJson(jobj);
	String sign = Phantom.signPayload(payload, 4);
//	if (D) Wow.v(TAG, "sendViaInet", "sign = " + sign, "payload = " + payload);
	//
	InetRequest.Options opts = new InetRequest.Options(url)
			.maxAttempts(4)
			.maxDuration(10000)
			.method(InetRequest.Method.POST)
			.contentType(InetRequest.ContentType.JSON)
			.addHeader(Rmi.HEAD_KEY, sign)
			.payload(payload);
	InetRequest<JsonObject> request = new InetRequest<JsonObject>(JsonObject.class, opts);
	if (D) Wow.v(TAG, rmiName, "exec started ... url = " + url + "; request = " + jobj);
	JsonObject result = request.execute();
	//
	rmi.setJsonResponse(result, responseClas, request.httpCode);
	//
	if (rmi.isSuccess()) {if (D) Wow.v(TAG, rmiName, "executed result = " + result);}
	else {if (D) Wow.w(TAG, rmiName, "executed FAILURE: " + request);}
}


}
