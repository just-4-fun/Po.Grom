package cyua.android.core.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import static cyua.android.core.AppCore.D; import cyua.android.core.log.Wow;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import cyua.android.core.AppCore;
import cyua.android.core.R;
import cyua.android.core.misc.EasyBundle;
import cyua.android.core.misc.Tiker;
import cyua.android.core.misc.Tool;

import static android.app.DatePickerDialog.OnDateSetListener;
import static android.app.TimePickerDialog.OnTimeSetListener;


/** Created by far.be on 7/13/13. */
public class DatetimePicker extends Button {

private static final String TAG = "DatetimePicker";

//
public final String YEAR = "year", MONTH = "month", DATE = "date", HOURS = "hours", MINUTES = "minutes", SECONDS = "seconds", CHANGED = "changed", TITLE = "title";
//
private enum Op {
	SHOW_TIME_DIALOG,
}
private Tiker<Op> tiker;
private CharSequence title;
private int year;
private int month;// 0-based
private int date;
private int hours;
private int minutes;
private int seconds;
private int mseconds;
private SelectListener selectListener;
private boolean changed;

public DatetimePicker(Context context) {
	super(context);
	preinit();
}
public DatetimePicker(Context context, AttributeSet attrs) {
	super(context, attrs);
	preinit();
}
public DatetimePicker(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	preinit();
}

@Override protected void onDetachedFromWindow() {
	if (tiker != null) tiker.clear();
	super.onDetachedFromWindow();
}

private void preinit() {
	tiker = new Tiker<Op>(TAG) {
		@Override public void handleTik(Op operation, Object obj, Bundle data) {
			if (operation == Op.SHOW_TIME_DIALOG) new TimeDailog(getId(), data).show();
		}
	};
	//
	setOnClickListener(new OnClickListener() {
		@Override public void onClick(View v) {
			new DateDailog(getId()).show();
		}
	});
	//
	title = getText();
	setTime(Tool.now());
}
public void setTime(long ms) {
	Calendar time = new GregorianCalendar();
	time.setTimeInMillis(ms);
	year = time.get(Calendar.YEAR);
	month = time.get(Calendar.MONTH);
	date = time.get(Calendar.DAY_OF_MONTH);
	hours = time.get(Calendar.HOUR_OF_DAY);
	minutes = time.get(Calendar.MINUTE);
	seconds = time.get(Calendar.SECOND);
	mseconds = time.get(Calendar.MILLISECOND);
	setText();
}

private void setText() {
	setText((title == null ? "" : title+": ") + getTime());
}

private void onDateSelected(int y, int m, int d) {
	Bundle data = new EasyBundle().putInt(YEAR, y).putInt(MONTH, m).putInt(DATE, d).get();
	tiker.setTik(Op.SHOW_TIME_DIALOG, null, data, 50);
}

private void onTimeSelected(int y, int mo, int d, int h, int mi) {
	if (year != y) {year = y; changed = true;}
	if (month != mo) {month = mo; changed = true;}
	if (date != d) {date = d; changed = true;}
	if (hours != h) {hours = h; changed = true;}
	if (minutes != mi) {minutes = mi; changed = true;}
	if (changed) setText();
	if (selectListener != null) selectListener.onSelect();
}

public String getTime() {
	return year+ "." + Tool.pad(month + 1)+"."+Tool.pad(date)  + "  " + Tool.pad(hours) + ":" + Tool.pad(minutes);
}

public long getTimeMs() {
	Calendar now = Tool.nowDate();
	now.set(Calendar.YEAR, year);
	now.set(Calendar.MONTH, month);
	now.set(Calendar.DAY_OF_MONTH, date);
	now.set(Calendar.HOUR_OF_DAY, hours);
	now.set(Calendar.MINUTE, minutes);
	now.set(Calendar.SECOND, seconds);
	now.set(Calendar.MILLISECOND, mseconds);
	return now.getTimeInMillis();
}

public boolean isChanged() {
	return changed;
}

public void setSelectListener(SelectListener _selectListener) {
	selectListener = _selectListener;
}

public boolean isBefore(long time) {
	return getTimeMs() < time;
}
public boolean isAfter(long time) {
	return getTimeMs() > time;
}

public boolean outOfRanges(List<long[]> ranges) {
	long time = getTimeMs();
	long now = Tool.now();
	for (long[] range : ranges) {
		long from = range[0], to = range[1];
		if (to == 0) to = now;
//		if (D) Wow.v(TAG, String.format("[outOfRanges]: %1$tD %1$tT < %2$tD %2$tT > %3$tD %3$tT", from, time, to));
		if (time >= from && time <= to) return false;
	}
	return true;
}

public void setInsideRanges(List<long[]> ranges) {
	long time = getTimeMs();
	long now = Tool.now();
	long pvTo = 0;
	for (long[] range : ranges) {
		long from = range[0], to = range[1];
		if (to == 0) to = now;
//		if (D) Wow.v(TAG, String.format("[setInsideRanges]: %1$tD %1$tT.%1$tL < %2$tD %2$tT.%2$tL > %3$tD %3$tT.%3$tL", from, time, to));
//		if (D) Wow.v(TAG, from+" < "+ time+" > "+ to);
		if (time >= from && time <= to) return;// OK
		else if (time < from)// OUT
		{
			if (pvTo == 0) return; // 1st iteration.  time is too early
			long toDelta = time - pvTo;
			long fromDelta = from - time;
			time = fromDelta <= toDelta ? from + 1000 : pvTo - 1000;
			select(time);
//			if (D) Wow.v(TAG, "select1:"+getTime()+"  ms:"+getTimeMs());
			return;
		}
		pvTo = to;
	}
	if (pvTo == 0) select(now);
	else select(pvTo - 1000);
//	if (D) Wow.v(TAG, "select0:"+getTime()+"  ms:"+getTimeMs());
	return;
}
private void select(long milliseconds) {
	setTime(milliseconds);
	changed = true;
}

public String isBeforeRanges(List<long[]> ranges) {
	long time = getTimeMs();
	for (long[] range : ranges) {
		long from = range[0];
		return time < from ? Tool.dateMonthTimeStr(from) : null;
	}
	return null;
}







/** DateDialog */

public static class DateDailog extends FloatUiConnector {
	private final String VIEWID = "viewid";
	private int viewId;
	public DateDailog() { }
	public DateDailog(int id) {
		super();
		name("DateDialog");
		viewId = id;
	}
	@Override public void onSaveInstanceState(Bundle outState) {
		outState.putInt(VIEWID, viewId);
		super.onSaveInstanceState(outState);
	}
	@Override public Dialog onCreateDialog(Bundle savedState) {
		if (savedState != null) viewId = savedState.getInt(VIEWID);
		//
		final DatetimePicker picker = UiHelper.view(viewId);
		if (picker == null) return FloatAlert.create(R.string.timepicker_broken, 0);
		//
		OnDateSetListener listener = new OnDateSetListener() {
			@Override public void onDateSet(DatePicker view, int y, int m, int d) {
				picker.onDateSelected(y, m, d);
			}
		};
		//
		return new DatePickerDialog(AppCore.uiContext(), listener, picker.year, picker.month, picker.date);
	}
}



@Override public Parcelable onSaveInstanceState() {
	Bundle bundle = new Bundle();
	bundle.putParcelable("superState", super.onSaveInstanceState());
	bundle.putString(TITLE, title == null ? "" : title.toString());
	bundle.putInt(YEAR, year);
	bundle.putInt(MONTH, month);
	bundle.putInt(DATE, date);
	bundle.putInt(HOURS, hours);
	bundle.putInt(MINUTES, minutes);
	bundle.putInt(SECONDS, seconds);
	bundle.putBoolean(CHANGED, changed);
	if (D) Wow.i(TAG, "onSaveInstanceState", "" + bundle);
	return bundle;
}

@Override public void onRestoreInstanceState(Parcelable state) {
	if (state instanceof Bundle) {
		Bundle bundle = (Bundle) state;
		state = bundle.getParcelable("superState");
		title = bundle.getString(TITLE);
		year = bundle.getInt(YEAR, year);
		month = bundle.getInt(MONTH, month);
		date = bundle.getInt(DATE, date);
		hours = bundle.getInt(HOURS, hours);
		minutes = bundle.getInt(MINUTES, minutes);
		seconds = bundle.getInt(SECONDS, seconds);
		changed = bundle.getBoolean(CHANGED, changed);
		if (D) Wow.i(TAG, "onRestoreInstanceState", "" + bundle);
	}
	super.onRestoreInstanceState(state);
	setText();
}







/** TIMEDIALOG */

public static class TimeDailog extends FloatUiConnector {
	private final String VIEWID = "viewid", DATA = "data";
	private int viewId;
	private Bundle data;
	public TimeDailog() { }
	public TimeDailog(int id, Bundle _data) {
		super();
		name("TimeDialog");
		viewId = id;
		data = _data;
	}
	@Override public void onSaveInstanceState(Bundle outState) {
		outState.putInt(VIEWID, viewId);
		outState.putBundle(DATA, data);
		super.onSaveInstanceState(outState);
	}
	@Override public Dialog onCreateDialog(Bundle savedState) {
		if (savedState != null) {
			viewId = savedState.getInt(VIEWID);
			data = savedState.getBundle(DATA);
		}
		//
		final DatetimePicker picker = UiHelper.view(viewId);
		if (picker == null) return FloatAlert.create(R.string.timepicker_broken, 0);
		//
		final int year = data.getInt(picker.YEAR),
				month = data.getInt(picker.MONTH),
				date = data.getInt(picker.DATE);
		//
		OnTimeSetListener listener = new OnTimeSetListener() {
			@Override public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				picker.onTimeSelected(year, month, date, hourOfDay, minute);
			}
		};
		return new TimePickerDialog(AppCore.uiContext(), listener, picker.hours, picker.minutes, true);
	}


}






/** SELECT LISTENER */

public static interface SelectListener {
	public void onSelect();
}

}
