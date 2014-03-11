package cyua.gae.appserver.urlfetch;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import cyua.gae.appserver.App;
import cyua.gae.appserver.Tool;
import cyua.gae.appserver.urlfetch.HttpResponse.RsStatus;
import cyua.java.shared.RMIException;

public class HttpRequest
{
static final Logger log = Logger.getLogger(HttpRequest.class.getName());
//
static final String UTF8 = "UTF-8";


/**********************************/
public static String get(String url)
{
	// XXX: can return null
	return exec(url, HTTPMethod.GET, null, null, null, 0, ContentType.DEFAULT).asText();
}

public static String get(String url, NVPairs<NVPair> bodyargs, Credentials creds, int retryMs)
{
	// XXX: can return null
	return exec(url, HTTPMethod.GET, bodyargs, null, creds, retryMs, ContentType.DEFAULT).asText();
}

public static HttpResponse post(String url, NVPairs<NVPair> bodyargs, Credentials creds, int retryMs)
{
	// XXX: can return null
	return exec(url, HTTPMethod.POST, bodyargs, null, creds, retryMs, ContentType.DEFAULT);
}


// ===================================================
public static HttpResponse exec(String url, HTTPMethod meth, NVPairs<NVPair> bodyargs, NVPairs<NVPair> headers, Credentials creds, int retryMs, ContentType contType)
{
	long lastTime = Tool.now();
	HttpResponse result = null;
	if (retryMs <= 0) retryMs = 20000;
	while(true)
	{
		result = fetch(url, meth, bodyargs, headers, creds, retryMs, contType);
		// recalc time
		long now = Tool.now();
		retryMs -= now - lastTime;
		lastTime = now;
		// analize
		if (result.status == RsStatus.OK || result.status == RsStatus.FAIL || retryMs <= 0) break;
		else if (result.status == RsStatus.AUTH)
		{
			if (creds != null) creds.getToken(true);
			else break;
		}
		// else if status = RETRY
		// and again...
		try{Thread.sleep(2500);}catch (InterruptedException ex) {}
		log.info("    [URL FETCH] ... Restart after "+result.status+"  resultCode:" + result.getCode());
	}
	return result;
}

// -------------------------------------------------------------------
protected static HttpResponse fetch(String url, HTTPMethod meth, NVPairs<NVPair> bodyargs, NVPairs<NVPair> headers, Credentials creds, int retryMs, ContentType contType)
{
	int errCode = 0;
	Throwable x = null;
	if (contType == null) contType = ContentType.DEFAULT;
	try
	{
		if (!App.isServiceAvailable(Capability.URL_FETCH)) throw new UrlFetchServiceUnavailableException();
		// REQUEST ******************
		String body = buildBody(bodyargs, contType);
		if (meth == HTTPMethod.GET && !Tool.isEmpty(body)) url += "?"+body;
		//
		double deadlineSec = retryMs/1000d;
		FetchOptions opts = FetchOptions.Builder.withDefaults().setDeadline(deadlineSec);
		HTTPRequest rq = new HTTPRequest(new URL(url), meth, opts);
		// AUTH
		if (creds != null)
		{
			String token = creds.getToken(false);
			if (token == null) throw new AuthFailedException(creds.account.getLastError());
			rq.setHeader(new HTTPHeader("Authorization", "GoogleLogin auth="+token));
		}
		//
		rq.setHeader(new HTTPHeader("Content-Type", contType.text));
		setHeaders(rq, headers);
		// 
		if (meth != HTTPMethod.GET && !Tool.isEmpty(body)) rq.setPayload((body.getBytes(UTF8)));
		//
		// RESPONSE ******************
		URLFetchService connection = URLFetchServiceFactory.getURLFetchService();
		HTTPResponse rsp = connection.fetch(rq);
	log.info("    [URL FETCH] "+meth+"  URL="+url+" ...");
		return new HttpResponse(rsp);
	}
//	TODO catch (ResponseTooLargeException ex)
//	catch (SocketTimeoutException ex) {x = ex; errCode = 408;}//408 Request Timeout
	catch (UrlFetchServiceUnavailableException ex) {x = ex; errCode = 191;}
	catch (AuthFailedException ex) {x = ex; errCode = 192;}
	catch (Throwable ex) {x = ex; errCode = 190;}
	//
	log.warning("    [URL FETCH FAILED] "+meth+"  URL="+url+", Error= "+Tool.stackTrace(x));
	return new HttpResponse(errCode);
}

private static void setHeaders(HTTPRequest rq, NVPairs<NVPair> headers)
{
	if (headers == null) return;
	for (NVPair header : headers) rq.setHeader(new HTTPHeader(header.name, header.value));
}
private static String buildBody(NVPairs<NVPair> params, ContentType contType) throws UnsupportedEncodingException
{
	if (params == null) return "";
	else if (contType == ContentType.JSON)
	{
		if (params.isEmpty()) return "";
		else return params.get(0).value;
	}
	//	else
	StringBuilder body = new StringBuilder();
	for (NVPair param : params)
	{
		body.append(body.length() == 0 ? "" : "&")
		.append(param.name+"=")
		.append(URLEncoder.encode(param.value, UTF8));
	}
	return body.toString();
}


// ===================================================
public static enum ContentType
{
DEFAULT("application/x-www-form-urlencoded"),
JSON("application/json");

public String text;
ContentType(String _text)
{
	text = _text;
}
}

// ===================================================
static class UrlFetchServiceUnavailableException extends Exception
{
}
static class AuthFailedException extends RMIException
{
AuthFailedException(String msg)
{
	super(msg);
}
}


}
