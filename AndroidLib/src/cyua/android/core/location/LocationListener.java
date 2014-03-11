package cyua.android.core.location;


public interface LocationListener
{
public void onFirstFix(Fix fix);
public void onNextFix(Fix fix);
public void onNextStableFix(Fix fix);
}
