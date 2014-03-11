package cyua.gae.appserver;

import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cyua.gae.appserver.rmi.RmiTargetGate;
import cyua.gae.appserver.urlfetch.HttpRequest;
import cyua.java.shared.BaseTool;
import cyua.java.shared.Phantom;
import cyua.java.shared.RMIException;
import cyua.java.shared.Rmi;
import cyua.java.shared.RmiTargetInterface;

public class MainServlet extends HttpServlet
{
private static final long serialVersionUID = 1818981329023050120L;
private static final Logger log = Logger.getLogger(MainServlet.class.getName());



//----------------------------------------------------------------------------------------
@Override
public void init() throws ServletException
{
	super.init();
	App.preInit(hashCode());
}

//----------------------------------------------------------------------------------------
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
{
	Tool.setTimeZone();
	processRequest(req, resp);
}

// -------------------------------------------------------------------
private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
{
	// WARNING ! keep in mind SYNCHRONIZATION
	long ms = Tool.now();
	try
	{
		String url = req.getRequestURI();
		if (!App.isProduction) log.info("<< .................... URL = "+url);// to distinct in console
		// decypt header
		String payload = readContent(req);
		String srcSign = req.getHeader(Rmi.HEAD_KEY);
		String sign = Phantom.signPayload(payload, 4);
		log.info("<<  srcSign = "+srcSign+"; sign = "+sign+";  payload = "+payload);
		if (!BaseTool.safeEquals(srcSign, sign))  throw new ServletException("Unknown error. #1="+srcSign+"; #2="+sign);
		// Process
		String result = null;
		if (url.contains(RmiTargetInterface.GATE_DIR))
			result = new RmiTargetGate().invoke(payload, App.isReady());
		else throw new RMIException("Unknown directory access. URL:"+url);
		// Response
		sendResponse(resp, result, 200);
	}
	catch (RMIException ex)
	{
		log.severe(Tool.stackTrace(ex));
		sendResponse(resp, ex.getMessage(), ex.httpCode);
	}
//	catch (DeadlineExceededException ex)// thrown after ~60 secs
//	catch (IOException ex)
	catch (Throwable ex)
	{
		log.severe(Tool.stackTrace(ex));
		sendResponse(resp, ex.getMessage(), RMIException.FAIL_CODE);
//		throw new ServletException(ex); // client will get resultCode 500 error
	}
	log.info("........................................... >> "+(Tool.now()-ms)+" ms");
}


private String readContent(HttpServletRequest req) throws IOException
{
	String content = null;
	String cntType = req.getContentType();
	if (Tool.notEmpty(cntType) && cntType.startsWith(HttpRequest.ContentType.DEFAULT.text))
	{
		char[] buff = new char[req.getContentLength()];
		Reader reader = req.getReader();
		reader.read(buff);
		content = URLDecoder.decode(new String(buff), Tool.UTF8);
	}
	else
	{
		byte[] data = new byte[req.getContentLength()];
		ServletInputStream is = req.getInputStream();
		is.read(data);
		content = new String(data, Tool.UTF8);
	}
	return content;
}

private void sendResponse(HttpServletResponse resp, String result, int code) throws IOException
{
	resp.setStatus(code);
	if (result == null) result = "";
	resp.setContentType(HttpRequest.ContentType.JSON.text);
	resp.setCharacterEncoding(Tool.UTF8);
	resp.getWriter().write(result);
}

}
