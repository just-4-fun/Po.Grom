package cyua.android.core.inet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cyua.android.core.log.Wow;
import cyua.android.core.misc.Tool;
import cyua.java.shared.RMIException;

import org.apache.http.NoHttpResponseException;
import static cyua.android.core.AppCore.D;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import javax.net.ssl.SSLException;


// WARNING T types: String, byte[] and JsonObject only supported
public class InetRequest<T>
{
private static final String TAG = InetRequest.class.getSimpleName();
public static final String UTF8 = "UTF-8";
//
public enum Method {GET, POST, PUT, DELETE}
//
public enum ContentType
{
DEFAULT("application/x-www-form-urlencoded"),
JSON("application/json; charset=utf-8");
public String value;
ContentType(String val) {value = val;}
}
//



/*****   OPTIONS   */

public static class Options
{
// request params
public String url, fullUrl;
public Method method = Method.GET;
public ContentType contentType = ContentType.DEFAULT;
public Map<String, Object> headers;
public Map<String, Object> urlArgs;
public Map<String, Object> payloadArgs;
public byte[] payload;
// optional params
public int connectTimeoutMs = 20000;// WARNING be careful if all requests run in sequence in one thread first can block others
public int readTimeoutMs = 65000;
public int maxAttempts = 1;
public long maxDuration;
public boolean followRedirects = true;
public InetAuthenticator authenticator;
public ErrorHandler errorHandler;

public Options(String url)
{
	this.url = url;
}
public Options method(Method method) {this.method = method; return this;}
public Options contentType(ContentType contentType) {this.contentType = contentType; return this;}
public Options headers(Map<String, Object> headers) {this.headers = headers; return this;}
public Options bodyArgs(Map<String, Object> args) {this.urlArgs = args; return this;}
public Options payload(byte[] payload) {this.payload = payload; return this;}
public Options payload(String payload) {this.payload = payload.getBytes(); return this;}
public Options payload(JsonObject jobj) {this.payload = new Gson().toJson(jobj).getBytes(); return this;}
public Options authenticator(InetAuthenticator authenticator) {this.authenticator = authenticator; return this;}
public Options connectTimeoutMs(int connectTimeoutMs) {this.connectTimeoutMs = connectTimeoutMs; return this;}
public Options readTimeoutMs(int readTimeoutMs) {this.readTimeoutMs = readTimeoutMs; return this;}
public Options maxAttempts(int maxAttempts) {this.maxAttempts = maxAttempts; return this;}
public Options maxDuration(long maxDuration) {this.maxDuration = maxDuration; return this;}
public Options followRedirects(boolean followRedirects) {this.followRedirects = followRedirects; return this;}
public Options errorHandler(ErrorHandler errorHandler) {this.errorHandler = errorHandler; return this;}
public Options addHeader(String name, Object value)
{
	if (headers == null) headers = new HashMap<String, Object>();
	headers.put(name, value);
	return this;
}
public Options addUrlArg(String name, Object nonEncodedValue)
{
	if (urlArgs == null) urlArgs = new HashMap<String, Object>();
	urlArgs.put(name, nonEncodedValue);
	return this;
}
public Options addPayloadArg(String name, Object nonEncodedValue)
{
	if (payloadArgs == null) payloadArgs = new HashMap<String, Object>();
	payloadArgs.put(name, nonEncodedValue);
	return this;
}
public Options clone()
{
	Options clone = new Options(url);
	try {
		for (Field var : getClass().getDeclaredFields()) var.set(clone, var.get(this));
	} catch (Throwable ex) {Wow.e(ex);}
	return clone;
}
}


/*****   INSTANCE   */
// request params
public Options opts;
public Class<T> returnClas;
// response params
public int httpCode;
public String httpMessage;
public Throwable exception;
public StringBuilder debugStr;
// processing params
public int attempts;
public long startTime;
public boolean canceled;

public InetRequest(Class<T> _returnClas, Options _opts)
{
	returnClas = _returnClas;
	opts = _opts.clone();
}

/**
 * @return T resultClas Class of return type. Valid classes String, byte[], JsonObject
 */
@SuppressWarnings("unchecked")
public T execute()
{
	HttpURLConnection conn = null;
	try {
		if (canceled) throw new RMIException(RMIException.CANCELED_CODE, "RmiRequest is canceled.");
		httpCode = 0;
		httpMessage = null;
		exception = null;
		//
		if (attempts++ == 0) startTime = Tool.now();
		debugStr = new StringBuilder("attempt="+attempts);
		//
		if (opts.authenticator != null) opts.authenticator.onPrepareRequest(opts);
		//
		opts.fullUrl = opts.url;
		if (Tool.notEmpty(opts.urlArgs)) opts.fullUrl += "?"+ argsToString(opts.urlArgs);
		if (Tool.notEmpty(opts.payloadArgs)) opts.payload(argsToString(opts.payloadArgs));
		debugStr.append(", method = " + opts.method).append(",  url=" + opts.fullUrl);
		//
		URL urlObject = new URL(opts.fullUrl);
		conn = (HttpURLConnection) urlObject.openConnection();
		conn.setRequestMethod(opts.method.toString());
		conn.setConnectTimeout(opts.connectTimeoutMs);
		conn.setReadTimeout(opts.readTimeoutMs);
		conn.setInstanceFollowRedirects(opts.followRedirects);
		//conn.setRequestProperty("User-Agent", "?");
		conn.setRequestProperty("Accept-Charset", UTF8);
		conn.setRequestProperty("Content-Type", opts.contentType.value);
//		if (opts.authenticator != null) conn.setRequestProperty(opts.authenticator.getHeaderName(), opts.authenticator.getHeaderValue());
		setHeaders(conn);
        //
        if (opts.method != Method.GET && opts.payload != null) {
        	conn.setDoOutput(true);
        	conn.setFixedLengthStreamingMode(opts.payload.length);
        	conn.getOutputStream().write(opts.payload);
	        debugStr.append(",  payload len="+opts.payload.length);
        }
		if (D) Wow.i(TAG, "execute", "started. " + debugStr);
        //
		if (canceled) throw new RMIException(RMIException.CANCELED_CODE, "RmiRequest is canceled.");
       conn.connect();
		if (canceled) throw new RMIException(RMIException.CANCELED_CODE, "RmiRequest is canceled.");
        //
        httpCode = conn.getResponseCode();
        debugStr.append(", httpCode="+httpCode);
        //
        if (httpCode >= 200 && httpCode < 300) {
//        	conn.getHeaderFields()
        	InputStream in = conn.getInputStream();
        	T result = null;
        	debugStr.append(", Available bytes="+in.available());
        	if (returnClas == String.class) result = (T) stringResponse(in);
        	else if (returnClas == byte[].class)  result = (T) byteResponse(in);
        	else if (returnClas ==  JsonObject.class)  result = (T) jsonResponse(in);
        	else throw new Exception("Result type of "+returnClas.getSimpleName()+" is not supported.");
        	debugStr.append(",  result="+result);
        	return result;
        }
        else throw new IOException("RmiRequest failed with resultCode:"+httpCode+".");
    }
    catch (IOException ex) {
    	exception = ex;
    	try {
    		httpMessage = conn.getResponseMessage();
    		String errInfo = stringResponse(conn.getErrorStream());
    		httpMessage = (httpMessage == null ? "" : httpMessage+"  ")+(errInfo == null ? "" : errInfo);
    	} catch (Exception ex2) {}
    }
    catch (RMIException ex) {exception = ex; httpCode = ex.httpCode;}
    catch (Throwable ex) {exception = ex;}
    finally {
    	if (conn != null) {
    		try {conn.getOutputStream().close();} catch(Throwable ex) {};
    		try {conn.getInputStream().close();} catch(Throwable ex) {};
    		try {conn.disconnect();} catch (Throwable ex) {}
    	}
    	debugStr.insert(0, (exception==null?"OK":"FAILED")+".  Duration="+(Tool.now()-startTime)+", ");
    	if (exception != null) debugStr.append(",  err="+exception.getMessage());
        if (D) Wow.i(TAG, "execute", "finished " + debugStr);
    	if (exception == null) recycle();
    }
    //
	if (D) Wow.w(TAG, "execute", Tool.stackTrace(exception));
    //
    if (nextAttempt()) {
    	try {Thread.sleep(500);} catch(Exception ex) {}
    	return execute();
    }
    else {
    	recycle();
    	return null;
    }
}

public boolean nextAttempt()
{
	 //
	Boolean retry = null;
	//
	if (opts.authenticator != null) retry = opts.authenticator.checkRetry(httpCode);
	if (retry == null && opts.errorHandler != null)
		retry = opts.errorHandler.handleErrorForRetry(httpCode, httpMessage, exception, opts);
	if (attempts > opts.maxAttempts || (opts.maxDuration > 0 && Tool.now()-startTime > opts.maxDuration)) retry = false;
	if (!InetService.isOnline()) {exception = new OfflineException(exception); retry = false;}
	if (retry == null) {
		retry = false;
		if (exception instanceof NoHttpResponseException) retry = true;// Retry if the server dropped connection on us
		else if (exception instanceof NoRouteToHostException) retry = true;// Offline possibly
		else if (exception instanceof UnknownHostException) retry = true;// Offline possibly
		else if (exception instanceof SocketTimeoutException) retry = true;// if connectTimeout or readTimeout elapsed
		else if (exception instanceof SSLException) retry = true;// Connection timed out
		else if (exception instanceof EOFException) retry = true;// ?
//		else if (exception instanceof ConnectException) retry = true;// Typically, the connection was refused remotely (e.g., no process is listening on the remote address/port).
		//	else if (exception instanceof SSLHandshakeException);// Do not retry on SSL handshake exception
	}
	//
	return retry;
}

public void recycle()
{
	attempts = 0;
//	httpCode = 0;
//	httpMessage = null;
//	exception = null;
//	debugStr = null;
	canceled= false;
}

public void cancel()
{
	canceled = true;
}


//private String buildBody() throws UnsupportedEncodingException {
//	if (Tool.isEmpty(opts.urlArgs)) return "";
//	else if (opts.contentType == ContentType.JSON)
//		return opts.urlArgs.get("").toString();
//	else return argsToString();
//}

private String argsToString(Map<String, Object> args) throws UnsupportedEncodingException
{
	StringBuilder body = new StringBuilder();
	for (Entry<String, Object> entry : args.entrySet()) {
		body.append(body.length() > 0 ? "&" : "")
		.append(entry.getKey()+"=")
		.append(URLEncoder.encode(String.valueOf(entry.getValue()), UTF8));
	}
	return body.toString();
}

private void setHeaders(URLConnection conn)
{
	if (opts.headers == null || opts.headers.isEmpty()) return;
	for (Entry<String, Object> header : opts.headers.entrySet()) {
		conn.addRequestProperty(header.getKey(), header.getValue().toString());
	}
}



private String stringResponse(InputStream srcIn) throws IOException
{
	if (srcIn == null) return null;
	BufferedReader bufRd = null;
	try {
		bufRd = new BufferedReader(new InputStreamReader(srcIn, UTF8));
		StringBuilder res = new StringBuilder();
		String line;
		while ((line = bufRd.readLine()) != null) res.append(line); 
		return res.toString();
	}
	finally {if (bufRd != null) try {bufRd.close();} catch(Exception ex) {};}
}

private byte[] byteResponse(InputStream srcIn) throws IOException
{
	if (srcIn == null) return null;
	BufferedInputStream bufIn = null;
	ByteArrayOutputStream out = null;
	try {
		bufIn = new BufferedInputStream(srcIn);
		byte[] buf = new byte[1024];
		int count = 0;
		out = new ByteArrayOutputStream(1024);
		while ((count = bufIn.read(buf)) != -1) out.write(buf, 0, count);
		return out.toByteArray();
	}
	finally {
		if (out != null) try {out.close();} catch(Exception ex) {};
		if (bufIn != null) try {bufIn.close();} catch(Exception ex) {};
	}
}

private JsonObject jsonResponse(InputStream in) throws Exception
{
	JsonReader reader = null;
	try {
		reader = new JsonReader(new InputStreamReader(in, UTF8));
		reader.setLenient(true);
		JsonElement jelt = new JsonParser().parse(reader);
		return jelt.getAsJsonObject();// throws exception if not a JsonObject
	}
	finally {if (reader != null) try {reader.close();} catch(Exception ex) {};}
}


// NOTE Reading the response body cleans up the connection even if you are not interested in the response content itself.
private void wasteStream(InputStream in) throws IOException
{
	try {
		byte[] buf = new byte[1024];
		while (in.read(buf) != -1) ;
	} catch (Exception ex) {}
	finally {if (in != null) try {in.close();} catch(Exception ex) {};}
}

@Override public String toString() {
	return "URL = "+opts.url+", attempts = "+attempts+", httpCode = "+httpCode+",  errMsg = "+errMessage()+", D = "+debugStr;
}

public String errMessage() {
	return "Ex = " + exception + "; httpMsg = " + httpMessage;
}


/*****   ERROR HANDLER   */

public static interface ErrorHandler
{
public Boolean handleErrorForRetry(int httpCode, String httpMessage, Throwable exception, Options opts);
}



/** OFFLINEEXCEPTION */

public static class OfflineException extends IOException {
	public OfflineException(Throwable cause) {
		super(cause);
	}
}


}
