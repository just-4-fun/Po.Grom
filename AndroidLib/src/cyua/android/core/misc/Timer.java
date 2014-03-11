package cyua.android.core.misc;

/** Created by far.be on 7/19/13. */
public class Timer {
long cms, expireMs;
public Timer() {
	cms = Tool.deviceNow();
}
public Timer setExpire(long addMs) {
	expireMs = Tool.deviceNow() + addMs;
	return this;
}
public boolean isExpired() {
	return Tool.deviceNow() >= expireMs;
}
public long getSpan(boolean reset) {
	long now = Tool.deviceNow();
	long span = now - cms;
	if (reset) cms = now;
	return span;
}
@Override public String toString() {
	return " [Time = " + getSpan(true)+"] ";
}
}
