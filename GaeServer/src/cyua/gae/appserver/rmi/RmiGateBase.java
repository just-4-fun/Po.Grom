package cyua.gae.appserver.rmi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import cyua.gae.appserver.Tool;
import cyua.java.shared.Rmi;
import cyua.java.shared.RmiTargetInterface;



public class RmiGateBase {
static final Logger log = Logger.getLogger(RmiGateBase.class.getName());
//
public String invoke(String content, boolean appReady) throws Throwable {
	JsonReader reader = new JsonReader(new StringReader(content));
	reader.setLenient(true);
	JsonElement jelt = new JsonParser().parse(reader);
	JsonObject jobj = jelt.getAsJsonObject();// throws exception if not a JsonObject
	// WARN RMI extension classes should be placed inside RmiTargetGate (subclass of RmiGateBase)
	String rmiName = Tool.stringFromJson(jobj, Rmi.DEF_PARAM);
	String rmiClasName = getClass().getName() + "$" + rmiName;
	Class<? extends Rmi> cls = (Class<? extends Rmi>) Class.forName(rmiClasName);
	Rmi rmi = cls.getDeclaredConstructor(getClass()).newInstance(this);
	// REQUEST
	rmi.request = Rmi.objectFromJson(jobj, Rmi.DATA_PARAM, rmi.request.getClass());
	// RESPONSE
	unpackExtra(jobj);
	//
	rmi.response();
	//
	jobj = new JsonObject();
	//
	Rmi.addToJson(jobj, Rmi.DATA_PARAM, rmi.response);
	//
	packExtra(jobj);
	//
	String result = new Gson().toJson(jobj);
//	log("    [INSTRUCTS]:"+BaseTool.printObject(instructs));
	log.info("    < < < <   [RESULT]:" + (result == null ? "null" : result.length() <= 500 ? result : result.substring(0, 500)));
	return result;
}

protected void packExtra(JsonObject jobj) {
	// TODO Override if needed
}

protected void unpackExtra(JsonObject jobj) {
	// TODO Override if needed
}

}
