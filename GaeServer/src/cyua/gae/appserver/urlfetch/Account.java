package cyua.gae.appserver.urlfetch;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cyua.gae.appserver.App;
import cyua.gae.appserver.memo.MCache;
import cyua.java.shared.Phantom;

import static cyua.java.shared.Phantom.Gae;


/*******************************************/
public enum Account
{
ROOT;
//
private static final Logger log = Logger.getLogger(Account.class.getName());
static final String AUTHURL = "https://www.google.com/accounts/ClientLogin";



public static void init() {
	// BASE Account 2-step verification is setup
	String login = App.isRealVersion ? Gae.ACCOUNT_BASENAME + Gae.ACCOUNT_ROOT_PFX  : Gae.ACCOUNT_BASENAME + Gae.ACCOUNT_TEST_PFX;
	String pass = App.isRealVersion ? Gae.ACCOUNT_ROOT_PSWD : Gae.ACCOUNT_TEST_PSWD;
	Account.ROOT.init(login, pass);
}



// ===================================================
// INSTANCE
public String name;
public String pass;
protected String lastError;
//
// -------------------------------------------------------------------
public void init(String _name, String _pass)
{
	name = _name; pass = _pass;
}
// -------------------------------------------------------------------
public String getToken(Scope scope)
{
	String token = null;
	Map<String, String> tokens = null;
	try{ tokens = MCache.getValue(MCache.CacheKeys.ACC_TOKENS);} catch(Throwable ex) {}
	if (tokens == null) tokens = new HashMap<String, String>();
	else token = tokens.get(name+"_"+scope.name());
	if (token == null)
	{
		token = authorize(scope);
		tokens.put(name+"_"+scope.name(), token);
		MCache.saveValue(MCache.CacheKeys.ACC_TOKENS, tokens);
	}
	return token;
}
public void resetToken(Scope scope)
{
	Map<String, String> tokens = null;
	try{ tokens = MCache.getValue(MCache.CacheKeys.ACC_TOKENS);} catch(Throwable ex) {}
	if (tokens == null) return;
	tokens.remove(name+"_"+scope.name());
	MCache.saveValue(MCache.CacheKeys.ACC_TOKENS, tokens);
}

// -------------------------------------------------------------------
private String authorize(Scope scope)
{
	NVPairs<NVPair> bodyargs = NVPairs.newList()
	.add("Email", name)
	.add("Passwd", pass)
	.add("service", scope.id)
	.add("source", App.appName)
	.add("accountType", "HOSTED_OR_GOOGLE");
	//
	HttpResponse resp = HttpRequest.post(AUTHURL, bodyargs, null, 5000);
	String res = resp.asText();
	String token = null;
	if (res != null)
	{
		Pattern authPtt = Pattern.compile(".*Auth=([^\\&]+)");
		Matcher m = authPtt.matcher(res);
		if (m.find())
		{
			token = m.group(1).trim();
			lastError = null;
		}
		else
		{
			Pattern errPtt = Pattern.compile(".*Error=([^\\&]+)");
			m = errPtt.matcher(res);
			if (m.find()) lastError = "Code:"+resp.getCode()+"  Message:"+ m.group(1).trim();
		}
	}
	else lastError = "Code:"+resp.getCode()+"  Message: Failed";
	//
	if (lastError != null) log.severe("[Account authorize]: resultCode:"+resp.getCode()+"  message:"+res);
	//
//	log.info("[ACCOUNT] "+name()+",  ligin:"+name+", pass:"+pass+",  token:"+token);
	return token;
}

public String getLastError()
{
	return lastError;
}

}