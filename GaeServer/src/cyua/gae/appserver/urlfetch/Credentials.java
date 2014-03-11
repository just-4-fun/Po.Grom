package cyua.gae.appserver.urlfetch;


/*******************************************/
public class Credentials
{
public Account account;
public Scope scope;
///
public Credentials(Account acc, Scope _scope)
{
	account = acc; scope = _scope;
}
public String getToken(boolean reset)
{
	if (reset) account.resetToken(scope);
	return account.getToken(scope);
}
}