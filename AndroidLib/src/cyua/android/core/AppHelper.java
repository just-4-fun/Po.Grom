package cyua.android.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import cyua.android.core.log.Wow;


public class AppHelper
{
private static final String TAG = AppHelper.class.getSimpleName();
//
private static List<Class<?>> singletons = new ArrayList<Class<?>>();
//


protected static void clearSingletons()
{
	for (Class<?> clas : singletons) {
		try {
			Field f = clas.getDeclaredField("I");
			f.setAccessible(true);
			f.set(null, null);
		} catch(Exception ex) {Wow.e(ex, "class=" + (clas == null ? "null" : clas.getSimpleName()), "Can't clear 'I' .");}
	}
}
/** For accesing Class's singleton instance via 'I' field 
 * Super instance should be wide enough to be assgned with instClas instance
 * WARNING Only Class and Superclass are checked*/
@SuppressWarnings("unchecked")
public static <T> T getSelfCleaningSingleton(Class<T> instClas)
{
	T inst = null;
	Field instField = getSingletonField(instClas);
	// CLASS Test
	if (instField != null) try {
		inst = (T) instField.get(null);
		if (inst == null) {
			inst = instClas.newInstance();
			instField.set(null, inst);
		}
		if (!singletons.contains(instClas)) singletons.add(instClas);
	} catch (Throwable ex) {
		Wow.e(ex, "instance class=" + (instClas == null ? "null" : instClas.getSimpleName()), " 'I' field problem");}
	// SUPER CLASS Test
	Class<?> superClas = instClas.getSuperclass();
	Field superField = getSingletonField(superClas);
	if (superField != null) try {
		T superInst = (T) superField.get(null);
		if (inst == null) {
			if (superInst != null) inst = superInst;
			else inst = instClas.newInstance();
		}
		if (superInst == null) superField.set(null, inst);
		if (!singletons.contains(superClas)) singletons.add(superClas);
	} catch (Throwable ex) {
		Wow.e(ex, "ERROR [getSelfCleaningSingleton] super class=" + (superClas == null ? "null" : superClas.getSimpleName()) + " 'I' field problem: " + ex.getMessage());}
	return inst;
}
private static Field getSingletonField(Class<?> hostClas)
{
	Field f = null;
	try {
		f = hostClas.getDeclaredField("I");
		if (!Modifier.isStatic(f.getModifiers())) throw new Exception("'I' field should be static."+f);
		f.setAccessible(true);
	} catch (Throwable ex) {}
	return f;
}

}
