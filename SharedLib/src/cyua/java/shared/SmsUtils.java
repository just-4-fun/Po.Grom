package cyua.java.shared;

import java.util.Map;
import java.util.TreeMap;


public class SmsUtils {

static TreeMap<Character, Character> c2l = new TreeMap<Character, Character>();
static TreeMap<Character, Character> l2c = new TreeMap<Character, Character>();

static{
	//
	c2l.put('а', 'A'); c2l.put('б', 'B'); c2l.put('в', 'C'); c2l.put('г', 'D'); c2l.put('ґ', 'E'); c2l.put('д', 'F');
	c2l.put('е', 'G'); c2l.put('ё', 'H'); c2l.put('є', 'I'); c2l.put('ж', 'J'); c2l.put('з', 'K'); c2l.put('и', 'L');
	c2l.put('й', 'M');c2l.put('і', 'N'); c2l.put('ї', 'O'); c2l.put('к', 'P'); c2l.put('л', 'Q'); c2l.put('м', 'R');
	c2l.put('н', 'S'); c2l.put('о', 'T'); c2l.put('п', 'U'); c2l.put('р', 'V'); c2l.put('с', 'W'); c2l.put('т', 'X');
	c2l.put('у', 'Y'); c2l.put('ф', 'Z');
	c2l.put('х', 'Å'); c2l.put('ц', 'å'); c2l.put('ч', 'É'); c2l.put('ш', 'é'); c2l.put('щ', 'Ä');
	c2l.put('ъ', 'ä'); c2l.put('ы', 'Ö'); c2l.put('ь', 'ö'); c2l.put('э', 'Ñ'); c2l.put('ю', 'ñ');
	c2l.put('я', 'Ü');
	//
	for (Map.Entry<Character, Character> kv : c2l.entrySet()) l2c.put(kv.getValue(), kv.getKey());
}

public static String encodeCyr(String text) {
	text = text.toLowerCase();
	int len = text.length();
	char[] cyrs = new char[len];
	char[] lats = new char[len];
	text.getChars(0, len, cyrs, 0);
	for (int $ = 0; $ < len; $++) {
		char sc = cyrs[$];
		Character cc = c2l.get(sc);
		lats[$] = cc == null ? sc : cc;
	}
	return new String(lats);
}

public static String decodeCyr(String text) {
	int len = text.length();
	char[] cyrs = new char[len];
	char[] lats = new char[len];
	text.getChars(0, len, lats, 0);
	for (int $ = 0; $ < len; $++) {
		char sc = lats[$];
		Character cc = l2c.get(sc);
		cyrs[$] = cc == null ? sc : cc;
	}
	return new String(cyrs);
}

}
