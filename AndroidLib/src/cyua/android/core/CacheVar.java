package cyua.android.core;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;


/**
 Test Example
 class TestVars extends ObjectVar
 {
 StringVar v1 = new StringVar("Vxx1");
 LongVar v2 = new LongVar();
 InnerVars v3 = new InnerVars();
 ArrayVar v4 = new ArrayVar();
 }
 class InnerVars extends ObjectVar
 {
 StringVar v1 = new StringVar("VINNER1");
 DoubleVar v2 = new DoubleVar();
 }
 <p/>
 void etc()
 {
 SharedPreferences cache = getSharedPreferences("test", 0);
 cache.edit().clear().commit();
 TestVars v0 = new TestVars();
 v0.init(cache, "v0", "");
 String js = "{Vxx1:'variable 1',v2:123,v3:{VINNER1:'inner var 1',v2:67.890},v4:[qwerty, 123.09]}";
 v0.set(js);
 if (D) Wow.i(TAG, ">>> ETC :  v1="+v0.v1+",  v2="+v0.v2+",  v3.v1="+v0.v3.v1+",  v3.v2="+v0.v3.v2+",  v4="+v0.v4.get().size());
 if (D) Wow.i(TAG, ">>> ETC :  v0="+v0);
 String v1text = "???(0);???(1);";
 ArrayVar v1 = new ArrayVar("DRIVERS");
 v1.init(cache, null, null);
 v1.set(v1text);
 //	List<String> list = v1.get();
 //	list.add("Stradevary(00)");
 //	v1.set(list);
 if (D) Wow.i(TAG, ">>> ETC :  DRIVERS v1="+v1);
 if (D) Wow.i(TAG, ">>> ETC :  cache.size:"+cache.getAll().size()+",   cache:"+BaseTool.printObject(cache.getAll()));
 }
 <p/>
 */

public abstract class CacheVar<T> {
private static final String TAG = CacheVar.class.getSimpleName();
public static final String NONAME = "";
//
public static final String
		DEFAULT_PREFS = "cache";
//		BACKUP_PREFS = "backupprefs";
public static SharedPreferences defaultCache;//, backupCache;
private static ArrayList<Class> backupListeners = new ArrayList<Class>();


/** *   STATIC */
public static void init() {
	defaultCache = AppCore.context().getSharedPreferences(DEFAULT_PREFS, 0);
//	backupCache = AppCore.context().getSharedPreferences(BACKUP_PREFS, 0);
}

public static void reinit() {
	for (Class clas : backupListeners) {
		initVars(clas, null, null);
	}
}


//public static void copyFromObject(Object clasORinst, Object data)
//{
//	Map<String, CacheVar<?>> vars = findVars(clasORinst);
//	Field[] fields = data.getClass().getDeclaredFields();
//	for (Field f : fields) {
//		try {
//			CacheVar<?> var = vars.get(f.path);
//			if (var == null || !f.isAccessible()) continue;
//			var.set(f.get(data));
//		} catch (Throwable ex) {Wow.e(TAG, BaseTool.stackTrace(ex));}
//	}
//}
//public static void copyToObject(Object clasORinst, Object data)
//{
//	Map<String, CacheVar<?>> vars = findVars(clasORinst);
//	Field[] fields = data.getClass().getDeclaredFields();
//	for (Field f : fields) {
//		try {
//			CacheVar<?> var = vars.get(f.path);
//			if (var == null || var.get() == null || !f.isAccessible()) continue;
//			Object val = null;
//			if (var instanceof ObjectVar) val = var.get().toString();
//			else if (var instanceof ArrayVar) val = ((ArrayVar)var).getAsJsonArray().toString();
//			else val = BaseTool.cast(var.get(), f.getType());
//			f.set(data, val);
//		} catch (Throwable ex) {Wow.e(TAG, BaseTool.stackTrace(ex));}
//	}
//}

public static void initVars(Object clasORinst, SharedPreferences _cache, String _path) {
	if (clasORinst instanceof Class && (_cache == null || _cache == defaultCache) && _path == null
			&& !backupListeners.contains(clasORinst)) backupListeners.add((Class) clasORinst);
	//
	Set<Entry<String, CacheVar<?>>> set = findVars(clasORinst).entrySet();
	for (Entry<String, CacheVar<?>> entry : set) {
		try {
			CacheVar<?> var = entry.getValue();
			String ptyName = entry.getKey();
			var.init(_cache, ptyName, _path);
		} catch (Throwable ex) {Wow.e(ex);}
	}
}

private static Map<String, CacheVar<?>> findVars(Object clasORinst) {
	Class<?> clas = null; Object inst = null;
	if (clasORinst instanceof Class) clas = (Class<?>) clasORinst;
	else {inst = clasORinst; clas = inst.getClass();}
	//
	Map<String, CacheVar<?>> vars = new HashMap<String, CacheVar<?>>();
	// look all superclasses up to Object
	boolean exit = false;
	while (!exit) {
		Field[] fields = clas.getDeclaredFields();
		for (Field f : fields) {
			try {
//				if (D) Wow.i(TAG, "CACHE >>>  clas="+clas.getSimpleName()+",  test field="+f.getName());
				if (CacheVar.class.isAssignableFrom(f.getType())) {
					boolean isStatic = Modifier.isStatic(f.getModifiers());
					if (!isStatic && inst == null) continue;
					f.setAccessible(true);
					CacheVar<?> var = (CacheVar<?>) f.get(isStatic ? null : inst);
					if (var == null) {
						var = (CacheVar<?>) f.getType().newInstance();
						f.set(inst, var);
					}
					String varName = var.simpleName();
					if (varName == null) varName = f.getName();
					vars.put(varName, var);
//					if (D) Wow.i(TAG, "CACHE >>> class="+clas.getSimpleName()+",  varName="+varName);
				}
			} catch (Throwable ex) {Wow.e(ex);}
		}
		clas = clas.getSuperclass();
		String pack = clas.getPackage().getName();
		exit = clas.equals(Object.class) || pack.startsWith("java") || pack.startsWith("android.");
	}
	return vars;
}







/** *   INSTANCE */

protected String name;
protected T value, defolt;
protected SharedPreferences cache = defaultCache;


// CONSTRUCTORS CHAIN

@SuppressWarnings("unchecked")
public <C extends CacheVar<T>> C name(String _name) {
	name = _name;
	return (C) this;
}
@SuppressWarnings("unchecked")
public <C extends CacheVar<T>> C defolt(T val) {
	defolt = val;
	return (C) this;
}
//@SuppressWarnings("unchecked")
//public <C extends CacheVar<T>> C backup() {
//	cache = backupCache;
//	return (C) this;
//}


protected void init(SharedPreferences _cache, String _name, String _path) {
	cache = _cache == null ? defaultCache : _cache;
	if (name == null) name = (Tool.isEmpty(_path) ? "" : _path + ".") + (_name == null ? "" : _name);
	value = null;
}

public T get() {
	if (value == null) getFromCache();// first time only
	return value;
}

public void set(Object val) {
	setToCache(val, cache.edit()).commit();
}

public void remove() {
	cache.edit().remove(name).commit();
	value = null;
}

protected abstract void getFromCache();
protected abstract Editor setToCache(Object val, Editor editor);

private String simpleName() {
	if (name == null) return null;
	int ix = name.lastIndexOf('.');
	if (ix < 0) return name;
	else return name.substring(ix + 1);
}

@Override public String toString() {
	Object res = get();
	return name + " = " + (res == null ? "null" : res.toString());
}

/** *   UTILS */
protected void checkByName() {
	Object val = null;
	try {val = cache.getString(name, "");} catch (ClassCastException ex) {}
	if (val == null) try {val = cache.getLong(name, 0L);} catch (ClassCastException ex) {}
	if (val == null) try {val = cache.getInt(name, 0);} catch (ClassCastException ex) {}
	if (val == null) try {val = cache.getFloat(name, 0f);} catch (ClassCastException ex) {}
	if (val == null) try {val = cache.getBoolean(name, false);} catch (ClassCastException ex) {}
//	if (val == null) val = getStringSet();
//	remove();
	set(val);
}








/** TYPED VARS */

public static class StringVar extends CacheVar<String> {

	public boolean isEmpty() {
		return Tool.isEmpty(get());
	}
	@Override protected Editor setToCache(Object val, Editor editor) {
		value = Tool.cast(val, String.class);
		if (value == null) value = "";
		return editor.putString(name, value);
	}
	@Override protected void getFromCache() {
		try {value = cache.getString(name, "");} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && value.equals("")) set(defolt);
	}
}



public static class LongVar extends CacheVar<Long> {

	@Override protected Editor setToCache(Object val, Editor editor) {
		value = Tool.cast(val, Long.class);
		if (value == null) value = 0L;
		return editor.putLong(name, value);
	}
	@Override protected void getFromCache() {
		try {value = cache.getLong(name, 0L);} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && value == 0) set(defolt);
	}
}



public static class DoubleVar extends CacheVar<Double> {

	@Override protected Editor setToCache(Object val, Editor editor) {
		value = Tool.cast(val, Double.class);
		if (value == null) value = 0d;
		return editor.putLong(name, Double.doubleToLongBits(value));
	}
	@Override protected void getFromCache() {
		try {value = Double.longBitsToDouble(cache.getLong(name, 0L));} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && value == 0) set(defolt);
	}
}



public static class IntVar extends CacheVar<Integer> {

	@Override protected Editor setToCache(Object val, Editor editor) {
		value = Tool.cast(val, Integer.class);
		if (value == null) value = 0;
		return editor.putInt(name, value);
	}
	@Override protected void getFromCache() {
		try {value = cache.getInt(name, 0);} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && value == 0) set(defolt);
	}
}



public static class FloatVar extends CacheVar<Float> {

	@Override protected Editor setToCache(Object val, Editor editor) {
		value = Tool.cast(val, Float.class);
		if (value == null) value = 0f;
		return editor.putFloat(name, value);
	}
	@Override protected void getFromCache() {
		try {value = cache.getFloat(name, 0f);} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && value == 0) set(defolt);
	}
}



public static class BooleanVar extends CacheVar<Boolean> {

	@Override protected Editor setToCache(Object val, Editor editor) {
		value = Tool.cast(val, Boolean.class);
		if (value == null) value = false;
		return editor.putBoolean(name, value);
	}
	@Override protected void getFromCache() {
		boolean noVal = !cache.contains(name);
		try {value = cache.getBoolean(name, false);} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && !value && noVal) set(defolt);
	}
}



public static class ArrayVar extends CacheVar<List<String>> {

	// valid values: Set<String>,  "," or ";" delimited string that can be placed in []
	@Override protected Editor setToCache(Object val, Editor editor) {
		String jstr = null;
		if (val instanceof List) jstr = new Gson().toJson(val);
		else {
			jstr = val == null ? "[]" : val.toString();
			if (jstr.endsWith(";")) jstr = jstr.substring(0, jstr.length() - 1);
			if (!jstr.startsWith("[")) jstr = "[" + jstr + "]";
		}
		value = parse(jstr);
		//
		return editor.putString(name, jstr);
	}
	@Override protected void getFromCache() {
		try {value = parse(cache.getString(name, "[]"));} catch (ClassCastException ex) {checkByName();}
		if (defolt != null && value.isEmpty()) set(defolt);
	}
	private List<String> parse(String jstr) {
		ArrayList<String> list = new ArrayList<String>();
		JsonReader jreader = new JsonReader(new StringReader(jstr));
		jreader.setLenient(true);
		JsonElement jelt = new JsonParser().parse(jreader);
		if (jelt.isJsonArray()) {
			JsonArray jarray = jelt.getAsJsonArray();
			for (JsonElement e : jarray) {
				if (e.isJsonPrimitive()) list.add(e.getAsString());
				else list.add(e.toString());
			}
		}
		return list;
	}
	private JsonArray getAsJsonArray() {
		JsonArray jarray = new JsonArray();
		if (Tool.isEmpty(value)) return jarray;
		Iterator<String> iterator = value.iterator();
		while (iterator.hasNext()) jarray.add(new JsonPrimitive(iterator.next()));
		return jarray;
	}
}





public static abstract class ObjectVar extends CacheVar<JsonObject> {

	@SuppressWarnings("unchecked")
	public <O extends ObjectVar> O init(String _name, SharedPreferences _cache) {
		init(_cache, _name, null);
		return (O) this;
	}

	@Override protected void init(SharedPreferences _cache, String _name, String _path) {
		super.init(_cache, _name, _path);
		initVars(this, cache, name);
	}
	@Override public void set(Object jsonString) {
		if (Tool.isEmpty(jsonString)) jsonString = "{}";
		JsonReader jreader = new JsonReader(new StringReader(jsonString.toString()));
		jreader.setLenient(true);
		JsonElement jelt = new JsonParser().parse(jreader);
		if (!jelt.isJsonObject()) return;
		setToCache(jelt.getAsJsonObject(), cache.edit()).commit();
	}
	@Override protected Editor setToCache(Object jsonObject, Editor editor) {
		Map<String, CacheVar<?>> vars = findVars(this);
		if (!(jsonObject instanceof JsonObject) || Tool.isEmpty(vars)) return editor;
		JsonObject jobject = (JsonObject) jsonObject;
		//
		for (Entry<String, JsonElement> entry : jobject.entrySet()) {
			try {
				String ptyName = entry.getKey();
				CacheVar<?> var = vars.get(ptyName);
				if (var == null) continue;
				JsonElement e = entry.getValue();
				if (var instanceof ObjectVar) var.setToCache(e.getAsJsonObject(), editor);
				else if (var instanceof ArrayVar || !e.isJsonPrimitive()) var.setToCache(e.toString(), editor);
				else if (var instanceof StringVar) var.setToCache(e.getAsString(), editor);
				else if (var instanceof LongVar) var.setToCache(e.getAsLong(), editor);
				else if (var instanceof DoubleVar) var.setToCache(e.getAsDouble(), editor);
				else if (var instanceof IntVar) var.setToCache(e.getAsInt(), editor);
				else if (var instanceof FloatVar) var.setToCache(e.getAsFloat(), editor);
				else if (var instanceof BooleanVar) var.setToCache(e.getAsBoolean(), editor);
			} catch (Throwable ex) {Wow.e(ex);}
		}
		return editor;
	}
	@Override public JsonObject get() {
		getFromCache();
		return value;
	}
	@Override protected void getFromCache() {
		value = new JsonObject();
		//
		for (Entry<String, CacheVar<?>> entry : findVars(this).entrySet()) {
			try {
				String ptyName = entry.getKey();
				CacheVar<?> var = entry.getValue();
				if (var instanceof ObjectVar) value.add(ptyName, (JsonObject) var.get());
				else if (var instanceof ArrayVar) value.add(ptyName, ((ArrayVar) var).getAsJsonArray());
				else if (var instanceof StringVar) value.addProperty(ptyName, ((StringVar) var).get());
				else if (var instanceof LongVar) value.addProperty(ptyName, ((LongVar) var).get());
				else if (var instanceof DoubleVar) value.addProperty(ptyName, ((DoubleVar) var).get());
				else if (var instanceof IntVar) value.addProperty(ptyName, ((IntVar) var).get());
				else if (var instanceof FloatVar) value.addProperty(ptyName, ((FloatVar) var).get());
				else if (var instanceof BooleanVar) value.addProperty(ptyName, ((BooleanVar) var).get());
			} catch (Throwable ex) {Wow.e(ex);}
		}
	}
	@Override public void remove() {
		for (Entry<String, CacheVar<?>> entry : findVars(this).entrySet()) {
			CacheVar<?> var = entry.getValue();
			var.remove();
		}
		value = null;
	}
}


}
