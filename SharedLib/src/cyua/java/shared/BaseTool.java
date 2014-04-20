package cyua.java.shared;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class BaseTool
{
public static final String UTF8 = "UTF-8";
public static Pattern numPattern = Pattern.compile("[\\D&&[^\\.,\\-]]*(\\-*\\d*(\\.|,)?\\d+)");// "[\\D]*(\\d*(\\.|,)?\\d+)"
public static long timePoint;
//

/*** CONVERSION ******************************/

public static String toNumString(Object num) {
	return toNumString(num, null);
}
public static String toNumString(Object num, String fmt) {
	String text;
	try {
		if (isEmpty(num)) text = "0";
		else if (num instanceof Number) {
			DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
			df.applyPattern(isEmpty(fmt) ? "#.################" : fmt);
			text = df.format(num);
		}
		else {
			Matcher m = numPattern.matcher(num.toString());
			if (m.find()) {
				text = m.group(1).replace(',', '.');// .  if Locale.EN
				if (!isEmpty(fmt)) {
					DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
					df.applyPattern(fmt);
					text = df.format(Double.valueOf(text));
				}
			}
			else text = "0";
		}
	} catch (Exception ex) {text = "0";}
	return text;
}


public static Number toNumber(Object text, String fmt) {
	try {
		String numText = toNumString(text, fmt);
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		Number num = df.parse(numText);
		return num;
	} catch (Throwable e) {}
	return 0;
}

public static double toDouble(Object obj, String fmt) {
	return toNumber(obj, fmt).doubleValue();
}

public static double toDouble(Object obj) {
	if (obj == null) return 0;
	else if (obj instanceof Double) return (Double) obj;
	Number n = obj instanceof Number ? (Number) obj : toNumber(obj, null);
	return n.doubleValue();
}
public static long toLong(Object obj) {
	if (obj == null) return 0;
	else if (obj instanceof Long) return (Long) obj;
	Number n = obj instanceof Number ? (Number) obj : toNumber(obj, null);
	return Math.round(n.doubleValue());
}
public static int toInt(Object obj) {
	if (obj == null) return 0;
	else if (obj instanceof Integer) return (Integer) obj;
	Number n = obj instanceof Number ? (Number) obj : toNumber(obj, null);
	return Math.round(n.floatValue());
}

public static boolean toBoolean(Object num) {
	return !isNothing(num);
}


public static long toNum(Long num) {
	return num != null ? num.longValue() : 0;
}
public static int toNum(Integer num) {
	return num != null ? num.intValue() : 0;
}
public static double toNum(Double num) {
	return num != null ? num.doubleValue() : 0;
}
public static float toNum(Float num) {
	return num != null ? num.floatValue() : 0f;
}


@SuppressWarnings("unchecked")
public static <T> T cast(Object val, Class<T> type) {
	if (val == null) return null;
	else if (val.getClass() == type) return (T) val;
	else if (Number.class.isAssignableFrom(type)) {
		Number res, num;
		if (val instanceof Number) num = (Number) val;
		else if (val instanceof Boolean) num = Integer.valueOf((Boolean) val ? 1 : 0);
		else num = toNumber(val, null);
		//
		if (type == Double.class) res = Double.valueOf(num.doubleValue());
		else if (type == Long.class) res = Long.valueOf(num.longValue());
		else if (type == Integer.class) res = Integer.valueOf(num.intValue());
		else if (type == Float.class) res = Float.valueOf(num.floatValue());
		else if (type == Byte.class) res = Byte.valueOf(num.byteValue());
		else if (type == Short.class) res = Short.valueOf(num.shortValue());
		else res = num;
		return (T) res;
	}
	else if (type == String.class) return (T) val.toString();
	else if (type == Boolean.class) return (T) Boolean.valueOf(!isNothing(val));
	else return null;
}


/** STRING UTILS ************************** */
public static String asString(Object text) {
	return text != null ? text.toString() : "";
}

@SuppressWarnings("rawtypes")
public static boolean isEmpty(Object val) {
	return val == null
			|| (val instanceof CharSequence && ((CharSequence) val).length() == 0)
			|| (val instanceof Collection && ((Collection) val).isEmpty())
			|| (val instanceof Map && ((Map) val).isEmpty())
			|| (val.getClass().isArray() && Array.getLength(val) == 0)
			|| val.toString().length() == 0;
}
public static boolean notEmpty(Object val) {
	return !isEmpty(val);
}

public static boolean isNothing(Object val) {
	if (val == null) return true;
	else if (val instanceof Boolean) return (Boolean) val;
	else if (val instanceof Number) return ((Number) val).doubleValue() == 0;
	String txt = val.toString().trim().toLowerCase();
	return txt.length() == 0 || txt.equals("false") || txt.equals("0") || txt.equals("0.0") || txt.equals("null") || txt.equals("no") || txt.equals("off") || txt.equals("нет");
}
public static boolean notNothing(Object val) {
	return !isNothing(val);
}


public static boolean safeEquals(CharSequence s1, CharSequence s2) {
	if (s1 == null || s2 == null) return s1 == null && s2 == null;
	return s1.equals(s2);
}

public static String pad(int c) {
	return c < 10 ? "0" + String.valueOf(c) : String.valueOf(c);
}



public static String randomString(int size)
{
	StringBuilder expr = new StringBuilder(size);
	int offset, len;
	for  (int $ = 0; $ < size; $++)
	{
		boolean isDigit = (int)(Math.random()*2) == 0;
		if (isDigit) {offset = 49; len = 9;}
		else {offset = 65; len = 26;}
		char chr = (char) (offset + Math.random()*len);
		expr.append(chr);
	}
	return expr.toString();
}





/***TIME UTILS ***********************************/
public static long now() {
	return System.currentTimeMillis();
}

public static GregorianCalendar nowDate() {
	GregorianCalendar date = new GregorianCalendar();
	date.setTimeInMillis(now());
	return date;
}

public static GregorianCalendar toDate(long ms) {
	GregorianCalendar date = new GregorianCalendar();
	date.setTimeInMillis(ms);
	return date;
}

public static String nowTimeStr() {
	return timeStr(now());
}
public static String timeStr(long ms) {
	return String.format("%1$tH:%1$tM", ms);
}

public static String dateTimeStr(long ms)
{
	return String.format("%1$td.%1$tm %1$tH:%1$tM", ms);
}

public static String nowFullDateStr()
{
	return fullDateStr(now());
}
public static String fullDateStr(long ms)
{
	return String.format("%1$tY.%1$tm.%1$td %1$tH:%1$tM", ms);
}

public static String nowMonthDateStr()
{
	return monthDateStr(now());
}
public static String monthDateStr(long ms)
{
	return String.format("%1$tm.%1$td", ms);
}


public static String nowFullTimeStr() {
	return fullTimeStr(now());
}
public static String fullTimeStr(long ms) {
	return String.format("%1$tH:%1$tM:%1$tS", ms);
}

public static String nowDateTimeStr() {
	return dateMonthTimeStr(now());
}
public static String dateMonthTimeStr(long ms) {
	return String.format("%1$td.%1$tm %1$tH:%1$tM", ms);
}
public static String monthDateTimeStr(long ms) {
	return String.format("%1$tm.%1$td %1$tH:%1$tM", ms);
}


public static String[] YMDT(long ms) {
	if (ms <= 0) ms = now();
	String[] ymdt = new String[6];
	GregorianCalendar c = new GregorianCalendar();
	c.setTimeInMillis(ms);
	ymdt[0] = c.get(Calendar.YEAR) + "";
	if (ymdt[0].length() > 2) ymdt[0] = ymdt[0].substring(ymdt[0].length() - 2);
	ymdt[1] = pad(c.get(Calendar.MONTH)+1);
	ymdt[2] = pad(c.get(Calendar.DATE));
	ymdt[3] = pad(c.get(Calendar.HOUR_OF_DAY));
	ymdt[4] = pad(c.get(Calendar.MINUTE));
	ymdt[5] = pad(c.get(Calendar.SECOND));
	return ymdt;
}


public static String minutes2hm(int minutes) {
	int hours = (int) Math.floor(minutes / 60);
	minutes = minutes % 60;
	return hours + ":" + minutes;// 12:43
}

public static String ms2hms(long ms) {
	boolean positive = ms >= 0;
	double s = Math.abs(ms / 1000d);
	int h = (int) Math.floor(s / 3600);
	s -= h * 3600;
	int m = (int) Math.floor(s / 60);
	s -= m * 60;
//	return (positive ? "" : "-") + (h > 0 ? h + "h " : "") + (m > 0 || h > 0 ? m + "m " : "") + toDouble(s, "#.###") + "s";// HH:MM:SS.MS
	return (positive ? "" : "-") + (h > 0 ? h + "h " : "") + (m > 0 || h > 0 ? m + "m " : "") + (int)s+ "s";//HH:MM:SS
}


public static enum TU {SEC, MIN, HOUR, DAY}
public static long toMS(long time, TU units) {
	switch (units) {
		case DAY: time *= 24;
		case HOUR: time *= 60;
		case MIN: time *= 60;
		case SEC: time *= 1000;
	}
	return time;
}


public static void setTimeZone()
{
	String id = "Europe/Kiev";
//	boolean found = false;
//	for (String tz : TimeZone.getAvailableIDs()) if (tz.equals(id)) {found = true; break;}
//	if (!found) id = "GMT+3:00";
	TimeZone reqTz = TimeZone.getTimeZone(id);
	TimeZone.setDefault(reqTz);
}

// ===================================================
// TIMER
public static void setTimer()
{
	timePoint = System.currentTimeMillis();
}
public static long getTimer(boolean reset)
{
	long now = System.currentTimeMillis();
	long time = now - timePoint;
	if (reset) timePoint = now;
	return time;
}

// ===================================================
public static String stackTrace(Throwable ex) {
	if (ex == null) return "Exception is null";
	String thisPack = BaseTool.class.getPackage().getName();
	int ix = thisPack.indexOf('.');
	if (ix < 0) ix = thisPack.length() - 1;
	thisPack = thisPack.substring(0, ix);
	boolean prev = false;
	String msg = ex.toString();
	if (isEmpty(msg)) msg = ex.getClass().getName()+": "+ex.getMessage();
	StringBuilder text = new StringBuilder("[" + msg + "]\n");
	//add each element of the stack trace
	for (StackTraceElement elt : ex.getStackTrace()) {
		String pack = elt.getClassName();
		boolean curr = ix < pack.length() && thisPack.equals(pack.substring(0, ix));
		if (!curr && prev) break;// all trace up to current package
		prev = curr;
		text.append((curr ? "!" : "") + "    " + elt + "\n");
	}
	Throwable cause = ex.getCause();
	if (cause != null && cause != ex) text.append("\n... CAUSED By: \n" + stackTrace(cause));
	return text.toString();
}

// -------------------------------------------------------------------
public static String printObject(Object object) {
	try {
		return new Gson().toJson(object);
	} catch (Throwable ex) {return "[FAILED to PRINT Object] : " + ex.getMessage();}
}

// ===================================================
// COLLECTIONS UTILS
public static <T> T[] values2Array(Class<T> clas, T... t) {
	return t;
}

public static <T> List<T> toList(T[] ttt) {
	ArrayList<T> list = new ArrayList<T>();
	if (notEmpty(ttt)) for (T t : ttt) list.add(t);
	return list;
}

public static String join(List<?> list, String delim) {
	if (isEmpty(list)) return "";
	StringBuilder str = new StringBuilder();
	for (Object obj : list) {
		str.append(str.length() > 0 ? delim : "")
				.append(obj == null ? "" : obj.toString());
	}
	return str.toString();
}
public static String join(Object[] objects, String delim) {
	if (isEmpty(objects)) return "";
	StringBuilder str = new StringBuilder();
	for (Object obj : objects) {
		str.append(str.length() > 0 ? delim : "")
				.append(obj == null ? "" : obj.toString());
	}
	return str.toString();
}



// ===================================================
// -------------------------------------------------------------------
public static void log(String tag, String msg) {
	System.out.println(tag+" :: "+msg);
}



//== JSON Utils ====================================
//-------------------------------------------------------------------
//-------------------------------------------------------------------
//public static <T> T fromJson(JsonObject jobj, String objName, Class<T> objCls)
//{
//	return new Gson().fromJson(jobj.get(objName), objCls);
//}
public static <T> T objectFromJson(JsonObject jobj, String objName, Type objType) {
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


/** **   REFLECTION */

/*
WARNING  as targetSuperclass use superclass of class that concretizes  generic param that is searched
Example:
static enum EN1{}
static class A<T1 extends Number, T2 extends Enum<?>> {}
static class B<T2 extends Enum<?>> extends A<Float, T2> {}
static class C extends B<EN1> {}
...
	// NOTE use B.class as targetSuperclass to get class EN1
	C obj = new C();
	enumClas = getGenericParamClass2(obj.getClass(), B.class, Enum.class);
	numClas = getGenericParamClass2(obj.getClass(), A.class, Number.class);
	...
	// NOTE use A.class as targetSuperclass to get class Float
	B<EN1> obj = new B<EN1>();
	numClas = getGenericParamClass2(obj.getClass(), A.class, Number.class);
	...
	// NOTE that won't work
	enumClas = getGenericParamClass2(obj.getClass(), A.class, Enum.class);
	numClas = getGenericParamClass2(obj.getClass(), B.class, Number.class);
...
*/
@SuppressWarnings("unchecked")
public static <Cls, Param> Class<? extends Param> getGenericParamClass(Class<? extends Cls> hostClass, Class<Cls> superclassOfHostClass, Class<Param> gegnericParamSuperclass) throws Exception {
	ParameterizedType targetGenericSuperclass = null;
	while (true) {
		Class<? extends Cls> clas = (Class<? extends Cls>) hostClass.getSuperclass();
		if (clas != superclassOfHostClass) {hostClass = clas; continue;}
		// else
		Type type = hostClass.getGenericSuperclass();
		if (!(type instanceof ParameterizedType)) throw new Exception("Target superclass "+superclassOfHostClass.getSimpleName()+" of class "+hostClass.getSimpleName()+" is not parametrized  with "+gegnericParamSuperclass.getSimpleName());
		targetGenericSuperclass = (ParameterizedType) type;
		break;
	}
	//
	for (Type type : targetGenericSuperclass.getActualTypeArguments()) {
		if (type instanceof Class<?>) {
			Class<?> paramClass = (Class<?>) type;
			if (gegnericParamSuperclass.isAssignableFrom(paramClass)) return (Class<? extends Param>) paramClass;
		}
	}
	throw new Exception("No subclass of generic type found.");
}



}
