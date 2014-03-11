package cyua.android.core.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;

import static cyua.android.core.AppCore.D;



public class HintNumField extends EditText {
public static final String TAG = "HintNumField";

private String value = "";
private String format;
private float mult = 1;
private OnFocusChangeListener focusListener2;

public HintNumField(Context context) {
	super(context);
	init();
}
public HintNumField(Context context, AttributeSet attrs) {
	super(context, attrs);
	init();
}
public HintNumField(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	init();
}

private void init() {
	setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
//	setImeOptions(EditorInfo.IME_ACTION_NEXT);
	setTag(getHint());
	//
	setOnFocusChangeListener(new OnFocusChangeListener() {
		@Override public void onFocusChange(View v, boolean hasFocus) {
			String val = getText().toString();
//			if (D) Wow.v(TAG, ">>>FIELD onFocusChange BEGIN::  hint = "+getTag()+", hasFocus = "+hasFocus+"  hasValue = "+hasValue()+"  text = "+val+"  value = "+value+"   EQ ? "+val.equals(value));
			if (hasFocus) {
				if (Tool.isEmpty(val) && !Tool.isEmpty(value)) {
					setText(value);
					setSelection(getText().length());
				}
			}
			else {
				float _mult = mult;
				setMultiplier(1);
				String origVal = value;
				setText("");// cought by onTextChanged > setAsHint("", format)
				setAsHint(!val.equals(origVal) ? val : origVal, format);
				setMultiplier(_mult);
			}
			//
			if (focusListener2 != null) focusListener2.onFocusChange(v, hasFocus);
		}
	});
	//
	addTextChangedListener(new TextWatcher() {
		@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
//		if (D) Wow.v(TAG, "[onTextChanged]: s:"+s);
			if (s.length() == 0) setAsHint("", format);
		}
		@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		@Override public void afterTextChanged(Editable s) {}
	});
}

public void setMultiplier(float _mult) {
	mult = _mult;
}

public void setAsHint(Object val, String fmt) {
	format = fmt;
	if (Tool.isEmpty(val)) value = "";
	else {
		double num = Tool.toDouble(val.toString()) * mult;
		value = Tool.toNumString(num, format);
//		if (D) Wow.v(TAG, "setAsHint", "tag:" + getTag() + "; origVal:" + val + ";  num:" + num + ";  value:" + value);
	}
	setHint(getTag() + (Tool.isEmpty(value) ? "" : ": " + value));
}
public void setAsHint(int val, String format) {setAsHint(Integer.valueOf(val), format);}
public void setAsHint(long val, String format) {setAsHint(Long.valueOf(val), format);}
public void setAsHint(float val, String format) {setAsHint(Float.valueOf(val), format);}
public void setAsHint(double val, String format) {setAsHint(Double.valueOf(val), format);}

public String getValue() {
	String val = "";
	if (!hasInput()) val = value;
	else val = getText().toString();
	//
	if (Tool.isEmpty(val)) return "";
	// else
	double num = Tool.toDouble(val) / mult;
	val = Tool.toNumString(num, format);
//	if (D) Wow.v(TAG, getTag()+" [getValue]: textVal:"+getText().toString()+"  num:"+num+"  val:"+val+"   floatVal:"+Tool.toFloat(val));
	return val;
}

public int getIntValue() {return Tool.toInt(getValue());}
public long getLongValue() {return Tool.toLong(getValue());}
public double getDoubleValue() {return Tool.toDouble(getValue());}

public boolean hasValue() {
	return hasInput() || Tool.notEmpty(value);
}

public boolean hasInput() {
	return Tool.notEmpty(getText()) && getText().toString().trim().length() > 0;
}

public void setOnFocusChangeListener2(OnFocusChangeListener listener) {
	focusListener2 = listener;
}

@Override
public void onDetachedFromWindow() {
	focusListener2 = null;
	super.onDetachedFromWindow();
}


@Override public Parcelable onSaveInstanceState() {
	Bundle bundle = new Bundle();
	bundle.putParcelable("superState", super.onSaveInstanceState());
	bundle.putString("val", value);
	return bundle;
}

@Override public void onRestoreInstanceState(Parcelable state) {
	String val = null;
	if (state instanceof Bundle) {
		Bundle bundle = (Bundle) state;
		val = bundle.getString("val");
		state = bundle.getParcelable("superState");
	}
	super.onRestoreInstanceState(state);
	setText("");
	setAsHint(val, format);
}

/*
@Override public Parcelable onSaveInstanceState() {
	setText(getValue());
//	if (D) Wow.v(TAG, ">>>FIELD onSaveInstanceState :: text = " + getText()+ "  value = "+value);
	return super.onSaveInstanceState();
}
@Override public void onRestoreInstanceState(Parcelable state) {
	super.onRestoreInstanceState(state);
	CharSequence val = getText();
	setText("");
	setAsHint(val, format);
//	if (D) Wow.v(TAG, ">>>FIELD onRestoreInstanceState :: text = " + getText() + ",  val = " + val + ",  value = " + value);
}
*/

}
