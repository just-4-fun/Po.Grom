package cyua.android.core.inet;


public abstract class InetAuthenticator
{
public final int retryCode = 401;
public String scope;


//public String getHeaderName()
//{
//	return "Authorization";
//}
//public String getHeaderValue()
//{
////	return "GoogleLogin auth=" + getToken();
//	return "OAuth " + getToken();
//}
public abstract String getToken();

public abstract String requestToken();

public Boolean checkRetry(int httpCode) {
	if (httpCode != retryCode) return null;
	else return requestToken() != null;
}

public abstract void onPrepareRequest(InetRequest.Options opts) ;
}
