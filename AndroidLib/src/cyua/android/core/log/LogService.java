package cyua.android.core.log;

import cyua.android.core.AppCore;
import cyua.android.core.AppCore.AppService;
import cyua.android.core.inet.InetService;


public class LogService extends AppService implements InetService.NetStateListener {
private static final String TAG = "LogService";
//
private static LogService I;




/*****   STATIC INIT   */

public static LogService instantiate()
{
	if (I != null) return I;
	I = new LogService();
	return I;
}



/** *   PUBLIC STATIC Members */

public static LogService setPersistAgentClass(Class<? extends IPersistAgent> agentClas) {
	I.agentClas = agentClas;
	return I;
}

public static boolean isBussy() {
	return I != null && I.agent != null && I.agent.isBussy();
}




/** INSTANCE */

private Class<? extends IPersistAgent> agentClas;
private IPersistAgent agent;



/** SERVICE METHODS */

@Override public void onInitStart(AppCore app) throws Throwable {
	try {if (agentClas != null) agent = agentClas.newInstance();} catch(Exception ex) { Wow.e(ex);}
}
@Override public String onInitFinish(AppCore app) throws Throwable {
	InetService.addListener(this, true);
	return super.onInitFinish(app);
}
@Override public void onExitStart(AppCore app) throws Throwable {
	InetService.removeListener(this);
	if (agent != null && InetService.isOnline()) agent.persist(this);
}
@Override public boolean isExited(AppCore app) throws Throwable {
	return agent == null || !agent.isBussy();
}
@Override public void onExitFinish(AppCore app) throws Throwable {
	agent = null;
}
/** INET LISTENER */

@Override public void onlineStatusChanged(boolean isOnline, boolean byUser) {
	if (!isOnline) return;
	InetService.removeListener(this);
	if (agent != null) agent.persist(this);
}










/** PERSIST AGENT */

public static interface IPersistAgent {
	public void persist(LogService logService);
	public boolean isBussy();
}


















/*
public static String getLogcat()
{
	// TODO filter by process id and leave TAG alone
	if (D) Wow.i(TAG, "getLogcat", "pid:"+Process.myPid());
	try
	{
		java.lang.Process process = Runtime.getRuntime().exec("logcat -v time -t 50 "+AppCore.name+":E *:S");
	   BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	   StringBuilder log=new StringBuilder();
	   String line;
	   while ((line = bufferedReader.readLine()) != null) {
	     log.append(line+"\n");
	   }
	   return log.toString();
	}
	catch (Throwable ex) { Wow.w(TAG, "", Tool.printObject(ex));}
	return "";
}
private void clearLogcat()
{
	try {Runtime.getRuntime().exec("logcat -c");} catch (Throwable ex) { Wow.w(TAG, "clearLogcat", Tool.printObject(ex));}
}
*/


}
