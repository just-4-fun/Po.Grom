package cyua.java.shared;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;



public abstract class Rmi<TRequest extends Rmi.RmiRequest, TResponse extends Rmi.RmiResponse> {
private static final String TAG = "Rmi";

public static final String HEAD_KEY = "App-header-1";
public static final String HEAD_UID = "App-header-2";
//
public static final String DATA_PARAM = "_data";
public static final String DEF_PARAM = "_def";
//
//public static final String REQUEST_FIELD = "request";
//public static final String RESPONSE_FIELD = "response";
//





/** **   INSTANCE */

public TRequest request;
public TResponse response;
public boolean primary;
private boolean canceled;
public long timeToRun;
public int resultCode;




@SuppressWarnings("unchecked")
public Rmi() {
	try {
		Class<TRequest> rqClas = (Class<TRequest>) BaseTool.getGenericParamClass(getClass(), Rmi.class, RmiRequest.class);
		request = rqClas.newInstance();
		Class<TResponse> rsClas = (Class<TResponse>) BaseTool.getGenericParamClass(getClass(), Rmi.class, RmiResponse.class);
		response = rsClas.newInstance();
	} catch (Exception e) { BaseTool.log(TAG, "Init error:" + e); }
}

public String getName() {
	return getClass().getSimpleName();
}

public boolean request() {return true;}

public void response() throws RMIException {}

public void done() throws Exception{
	if (isSuccess()) onSuccess();
	else onError();
}

public void onSuccess() {}

public void onError() {}

public boolean isSuccess() {
	return resultCode == 200;
}






/** **   STATIC */

// NOTE Client side resultCode
public static JsonObject request2data(Rmi<?, ?> rmi) {
	JsonObject data = new JsonObject();
	addToJson(data, DEF_PARAM, rmi.getName());
	addToJson(data, DATA_PARAM, rmi.request);
	return data;
}

public static void data2response(Rmi<?, ?> rmi, JsonObject data) {
	rmi.response = objectFromJson(data, DATA_PARAM, rmi.response.getClass());
}

//NOTE Server side resultCode
public static Rmi<?, ?> data2request(JsonObject data, Object implContainerInstance) throws Exception {
	String rmiName = stringFromJson(data, Rmi.DEF_PARAM);
	String packagePfx = implContainerInstance.getClass().getName() + "$";
	Class<?> cls = Class.forName(packagePfx + rmiName);
	Rmi<?, ?> rmi = (Rmi<?, ?>) cls.getDeclaredConstructor(implContainerInstance.getClass()).newInstance(implContainerInstance);
	// REQUEST
	rmi.request = objectFromJson(data, DATA_PARAM, rmi.request.getClass());
	return rmi;
}

public static JsonObject response2data(Rmi<?, ?> rmi) {
	JsonObject data = new JsonObject();
	addToJson(data, DATA_PARAM, rmi.response);
	return data;
}




//== JSON Utils ====================================
//-------------------------------------------------------------------
//-------------------------------------------------------------------
//public static <T> T fromJson(JsonObject jobj, String objName, Class<T> objCls)
//{
//	return new Gson().fromJson(jobj.get(objName), objCls);
//}
public static <T> T objectFromJson(JsonObject jobj, String objName, Type objType) {
	if (jobj == null) return (T)null;
	return new Gson().fromJson(jobj.get(objName), objType);
}
//-------------------------------------------------------------------
public static String stringFromJson(JsonObject jobj, String objName) {
	JsonPrimitive jp = jobj.getAsJsonPrimitive(objName);
	return jp == null ? null : jp.getAsString();
}
//-------------------------------------------------------------------
public static long longFromJson(JsonObject jobj, String objName) {
	JsonPrimitive jp = jobj.getAsJsonPrimitive(objName);
	return jp == null ? 0L : jp.getAsLong();
}
//-------------------------------------------------------------------
public static double doubleFromJson(JsonObject jobj, String objName) {
	JsonPrimitive jp = jobj.getAsJsonPrimitive(objName);
	return jp == null ? 0 : jp.getAsDouble();
}

//-------------------------------------------------------------------
public static void addToJson(JsonObject jobj, String objName, Object objValue) {
	jobj.add(objName, new Gson().toJsonTree(objValue));
}
//-------------------------------------------------------------------
public static void addToJson(JsonObject jobj, String objName, Object objValue, Type paramType) {
	jobj.add(objName, new Gson().toJsonTree(objValue, paramType));
}

public JsonObject getJsonRequest() {
	JsonObject jobj = new JsonObject();
	Rmi.addToJson(jobj, Rmi.DEF_PARAM, getName());
	Rmi.addToJson(jobj, Rmi.DATA_PARAM, request);
	return jobj;
}
public void setJsonResponse(JsonObject result, Class<?> respClas, int httpCode) {
	resultCode = httpCode;
	response = Rmi.objectFromJson(result, Rmi.DATA_PARAM, respClas);
}






/** **   REQUEST & RESPONSE SUPERCLASS (for generic type detection) */

public static abstract class RmiRequest {}

public static abstract class RmiResponse {}

}
