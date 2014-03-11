package cyua.gae.appserver.urlfetch;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;

/********************************************/
public class HttpResponse
{
private static final Logger log = Logger.getLogger(HttpResponse.class.getName());
//
public static enum RsStatus {OK, RETRY, AUTH, FAIL}
//
public RsStatus status;
protected HTTPResponse response;
protected int code;

public HttpResponse(HTTPResponse rsp)
{
	response = rsp;
	setStatus();
}

public HttpResponse(int _code)
{
	code = _code;
	setStatus();
}

public int getCode() 
{
	return response == null ? code : response.getResponseCode();
}

public List<HTTPHeader> getHeaders()
{
	return response == null ? new ArrayList<HTTPHeader>() : response.getHeaders();
}

public byte[] getContent()
{
	return response == null ? null : response.getContent();
}

public String asText()
{
	try
	{
		byte[] data = getContent();
		return data == null ? null : new String(data, HttpRequest.UTF8);
	}
	catch (UnsupportedEncodingException ex)
	{
		// TODO try guess encoding
		log.warning("[getText] UnsupportedEncodingException");
		return null;
	}
}

public void setStatus()
{
	int code = getCode();
	String content = null;
	switch (code)
	{
		case 200:
			status = RsStatus.OK;
			break;
		case 401: // SC_UNAUTHORIZED When invalid or no credentials are presented for a private table
			status = RsStatus.AUTH;
			break;
		case 403:// SC_FORBIDDEN When valid credentials are presented for a private table, but permission is denied
			content = asText();
			if (content == null || !content.contains("Rate Limit Exceeded")) status = RsStatus.FAIL;
			else status = RsStatus.RETRY;
			break;
		case 190:// general inner Exception
		case 191:// URL_FETCH service anavailable
		case 192:// Authorization failed
		case 404:// Not Found
		case 400:// SC_BAD_REQUEST
			status = RsStatus.FAIL;
			break;
		case 503:// serverErrorNotRetryable OR try again OR Backend Error
			content = asText();
			if (content == null || !content.contains("try again")) status = RsStatus.FAIL;
			else status = RsStatus.RETRY;
			break;
		case 408: // SC_REQUEST_TIMEOUT
		case 500: // SC_INTERNAL_SERVER_ERROR
			status = RsStatus.RETRY;
			break;
		default:
			if (code > 200 && code < 300) status = RsStatus.OK;// Success codes
			else status = RsStatus.FAIL;
			break;
	}
}
}