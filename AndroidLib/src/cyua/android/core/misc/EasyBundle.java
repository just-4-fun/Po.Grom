package cyua.android.core.misc;

import android.os.Bundle;


/** Created by far.be on 6/11/13. */
public class EasyBundle {

private Bundle instance;

public EasyBundle() {
	instance = new Bundle();
}

public Bundle get() {
	return  instance;
}

public EasyBundle put(String key, String value) {
	instance.putString(key, value);
	return this;
}
public EasyBundle putInt(String key, int value) {
	instance.putInt(key, value);
	return this;
}
public EasyBundle putLong(String key, long value) {
	instance.putLong(key, value);
	return this;
}
public EasyBundle putFlt(String key, float value) {
	instance.putFloat(key, value);
	return this;
}
public EasyBundle putDbl(String key, double value) {
	instance.putDouble(key, value);
	return this;
}
public EasyBundle put(String key, boolean value) {
	instance.putBoolean(key, value);
	return this;
}

}
