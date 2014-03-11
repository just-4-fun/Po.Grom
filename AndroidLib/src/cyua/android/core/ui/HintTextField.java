package cyua.android.core.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;

import static cyua.android.core.AppCore.D;


/** Created by far.be on 7/12/13. */
public class HintTextField extends EditText {
public static final String TAG = "HintTextField";

private String value = "";
private View.OnFocusChangeListener focusListener2;

public HintTextField(Context context) {
	super(context);
	init();
}
public HintTextField(Context context, AttributeSet attrs) {
	super(context, attrs);
	init();
}
public HintTextField(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	init();
}

private void init() {
	setTag(getHint());
	//
	setOnFocusChangeListener(new View.OnFocusChangeListener() {
		@Override public void onFocusChange(View v, boolean hasFocus) {
			String val = getText() == null ? "" : getText().toString();
//			if (D) Wow.v(TAG, ">>>FIELD onFocusChange BEGIN::  hint = "+getTag()+", hasFocus = "+hasFocus+"  hasValue = "+hasValue()+"  text = "+val+"  value = "+value+"   EQ ? "+val.equals(value));
			if (hasFocus) {
				if (Tool.isEmpty(val) && Tool.notEmpty(value)) {
					setText(value);
					setSelection(getText().length());
				}
			}
			else {
				setText("");// cought by onTextChanged > setAsHint("", format)
				setAsHint(val);
			}
//			if (D) Wow.v(TAG, ">>>FIELD onFocusChange FINISH::   value = "+value);
			//
			if (focusListener2 != null) focusListener2.onFocusChange(v, hasFocus);
		}
	});
	//
	addTextChangedListener(new TextWatcher() {
		@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
//			if (D) Wow.v(TAG, ">>>FIELD onTextChanged ::   value = "+value);
			if (s.length() == 0) setAsHint("");
		}
		@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		@Override public void afterTextChanged(Editable s) {}
	});
}

public void setAsHint(CharSequence val) {
	value = val == null ? "" : val.toString();
	setHint(getTag() + (Tool.isEmpty(value) ? "" : ": " + value));
//	if (D) Wow.v(TAG, "setAsHint", "value = "+value, "hint = "+getHint());
}

public String getValue() {
	String val = hasInput() ? getText().toString() : value;
//	if (D) Wow.v(TAG, ">>>FIELD getValue :: Hint = "+getTag() + ",  text = " + getText().toString() + "  value = "+value+",  return val = "+val);
	return val;
}


public boolean hasValue() {
	return hasInput() || Tool.notEmpty(value);
}

public boolean hasInput() {
	return Tool.notEmpty(getText()) && getText().toString().trim().length() > 0;
}

public void setOnFocusChangeListener2(View.OnFocusChangeListener listener) {
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
	setAsHint(val);
}


/*
@Override public Parcelable onSaveInstanceState() {
	setText(getValue());
//	if (D) Wow.v(TAG, "onSaveInstanceState", "text = " + getText() + "  value = " + value);
	return super.onSaveInstanceState();
}
@Override public void onRestoreInstanceState(Parcelable state) {
	super.onRestoreInstanceState(state);
	CharSequence val = getText();
	setText("");
	setAsHint(val);
//	if (D) Wow.v(TAG, "onRestoreInstanceState", "text = " + getText() + ",  val = " + val + ",  value = " + value);
}
*/

}
