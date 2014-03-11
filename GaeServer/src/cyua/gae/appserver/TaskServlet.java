package cyua.gae.appserver;

import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

import cyua.gae.appserver.fusion.FTColumn;
import cyua.gae.appserver.fusion.FTDB;
import cyua.gae.appserver.fusion.FTOperation;
import cyua.gae.appserver.fusion.FTTable;
import cyua.gae.appserver.memo.MCache;
import cyua.gae.appserver.memo.MCache.CacheKeys;
import cyua.java.shared.RMIException;


public class TaskServlet extends HttpServlet {
private static final long serialVersionUID = -6722055193324021036L;
private static final Logger log = Logger.getLogger(TaskServlet.class.getName());
//
public static final String TASK_URL = "/task";
public static final String METHOD_HDR = "Task-method-name";
private static final String MAX_RETRIES_PARAM = "max_retries";
//
public static enum Queues {
	DEFAULT, CYCLIC
}
//
public static enum Tasks {
	INIT,
	FTDB_INIT, FTDB_CLEANUP, FTDB_OPTIMIZE,
}
//
private boolean ticBussy;


// ===================================================
public static boolean addTask(Queues que, Tasks method, Map<String, String> params, long optOverMs, long optAtMs, RetryOptionsExt rtOpts, boolean isBackend) throws RMIException {
	try {
		if (rtOpts == null) rtOpts = new RetryOptionsExt(0, 0, 0, 0, 0);
		String qName = que.name().toLowerCase();
		log.info("    [ADD TASK] que:" + qName + "  method:" + method + "  hasParams:" + (params != null) + "  overMs:" + optOverMs + "  atMs:" + optAtMs + "  retryOpts:" + Tool.printObject(rtOpts) + ",  isBackend:" + isBackend);
		//
		Queue queue = QueueFactory.getQueue(qName);
		//
		String url = TASK_URL + "/" + method;
		TaskOptions opts = TaskOptions.Builder.withUrl(url).method(Method.POST)
				.header(TaskServlet.METHOD_HDR, method.toString());
		if (optOverMs > 0) opts.countdownMillis(optOverMs);
		else if (optAtMs > 0) opts.etaMillis(optAtMs);
		opts.retryOptions(rtOpts.opts);
		//
		if (params == null) params = new HashMap<String, String>();
		params.put(MAX_RETRIES_PARAM, rtOpts.retries + "");
		for (Entry<String, String> pair : params.entrySet()) opts.param(pair.getKey(), pair.getValue());
		// if run within BACKEND
		if (isBackend) {
			if (Backend.isOverLimit()) return false;
			opts.header("Host", Backend.getAddress());
		}
		//
		queue.add(opts);
		return true;
	} catch (Throwable ex) {log.warning("Failed to add Task: " + ex);}
	return false;
}
// -------------------------------------------------------------------
public static boolean addTask(Queues que, DeferredTaskExt task, long optOverMs, long optAtMs, RetryOptionsExt rtOpts, boolean isBackend) throws RMIException {
	try {
		if (rtOpts == null) rtOpts = new RetryOptionsExt(0, 0, 0, 0, 0);
		task.maxRetries = rtOpts.retries;
		String qName = que.name().toLowerCase();
		log.info("    [ADD TASK] que:" + qName + "  deferedTask:" + task.getClass().getSimpleName() + "  overMs:" + optOverMs + "  atMs:" + optAtMs + "  retryOpts:" + Tool.printObject(rtOpts) + ",  isBackend:" + isBackend);
		//
		Queue queue = QueueFactory.getQueue(qName);
		//
		TaskOptions opts = TaskOptions.Builder.withPayload(task);
		if (optOverMs > 0) opts.countdownMillis(optOverMs);
		else if (optAtMs > 0) opts.etaMillis(optAtMs);
		opts.retryOptions(rtOpts.opts);
		// if run within BACKEND
		if (isBackend) {
			if (Backend.isOverLimit()) return false;
			opts.header("Host", Backend.getAddress());
		}
		//
		queue.add(opts);
		return true;
	} catch (Throwable ex) {log.warning("Failed to add Task: " + ex);}
	return false;
}

//-------------------------------------------------------------------
public static void purgeQueue(Queues que) {
	String qName = que.name().toLowerCase();
	try {
		log.info("    [PURGE QUEUE]: " + qName);
		Queue queue = QueueFactory.getQueue(qName);
		queue.purge();
	} catch (Throwable ex) {log.warning("Failed to purge " + qName + " Queue: " + ex);}
}



/*
X-AppEngine-QueueName, the name of the queue (possibly default)
X-AppEngine-TaskName, the name of the task, or a system-generated unique ID if no name was specified
X-AppEngine-TaskRetryCount, the number of times this task has been retried; for the first attempt, this value is 0
X-AppEngine-FailFast specifies that a task running on a backend fails immediately instead of waiting in a pending queue.
X-AppEngine-TaskETA, the target execution time of the task, specified in microseconds since January 1st 1970.
*/
//----------------------------------------------------------------------------------------
// TASK call
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	long ms = Tool.now();
	boolean isBackend = false;
	try {
		Tool.setTimeZone();
		int retries = Tool.toInt(req.getHeader("X-AppEngine-TaskRetryCount"));
		String queName = Tool.asString(req.getHeader("X-AppEngine-QueueName"));
		isBackend = Backend.isBackendHost(Tool.asString(req.getHeader("Host")));
		//	String taskName = Tool.asString(req.getHeader("X-AppEngine-TaskName"));
		//	long eta = Tool.toLong(Tool.asString(req.getHeader("X-AppEngine-TaskETA").replace(".", "")));
		//	String failfast = Tool.asString(req.getHeader("X-AppEngine-FailFast"));
		//	String content = readContent(req);// TODO do not read content and get param (only one is valid)
		//	String key = req.getParameter("key");
		//	log.info(String.format("[TASK HEADERS]... %1$s, %2$s, %3$s, %4$s\n[PARAMS]...%5$s", queName,taskName,retries,eta, new Gson().toJson(req.getParameterMap())));
//		Enumeration<String> hnames = req.getHeaderNames();
//		while (hnames.hasMoreElements())
//		{
//			String hname = hnames.nextElement();
//			log.info(hname+"="+req.getHeader(hname));
//		}
		String methodName = Tool.asString(req.getHeader(METHOD_HDR));
		Map params = req.getParameterMap();
		int maxRetries = Integer.MAX_VALUE;
		if (Queues.CYCLIC.name().toLowerCase().equals(queName)) maxRetries = 0;
		else if (params != null && params.containsKey(MAX_RETRIES_PARAM))
			maxRetries = Tool.toInt(params.get(MAX_RETRIES_PARAM));
		//
		if (retries > maxRetries) {
			log.warning("UNEXPECTED RETRY. Queue:" + queName + ", meth:" + methodName + ". maxReties:" + maxRetries + ",  retry:" + retries);
			resp.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		//
		Tasks method = Tasks.valueOf(methodName);// throws Exception if no match
		log.info("    >>    [TASK " + methodName.toUpperCase() + "]  hasParams:" + (params != null) + "  #try:" + retries);
		//
		execTask(method, params, isBackend);
		//
		log.info("    <<   [TASK " + methodName.toUpperCase() + "]  DONE in " + (Tool.now() - ms) + " ms");
		resp.setStatus(HttpServletResponse.SC_OK);
	} catch (Throwable ex) {
		log.severe("    [Task exec Error] :" + Tool.stackTrace(ex));
		resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	} finally {
		if (isBackend) Backend.appendTimeUsed(Tool.now() - ms);
	}
}
// -------------------------------------------------------------------
private void execTask(Tasks method, Map params, boolean isBackend) throws Throwable {
	switch (method) {
		case INIT:
			App.init();
			break;
		case FTDB_INIT:
			if (App.isReady()) FTDB.init();
			break;
		case FTDB_OPTIMIZE:
//			if (App.isReady()) MessageManager.optimize();
			break;
		case FTDB_CLEANUP:
//			if (App.isReady()) MessageManager.cleanup();
			break;
	}
}


//===================================================
/*X-AppEngine-Cron: true*/
//----------------------------------------------------------------------------------------
//CRON call
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	log.info("    [CRON TIC RUNING] ............................");
	try {
		tic();
	} catch (Throwable ex) {
		log.severe(Tool.stackTrace(ex));
		throw new ServletException(ex);
	}
}

//-------------------------------------------------------------------
private void tic() throws Throwable {
	if (!App.isReady()) { App.preInit(-1); return;}
	Tool.setTimeZone();
// else
	long now = Tool.now();
//Day tic. Once after 4:00
	tryDayTic(now);
// check if could reset day quotas
	tryResetQuotas(now);
// avoid long running ops run twice at same time
	if (ticBussy) return;
	else ticBussy = true;
	try {// do tasks
		refreshConfig(now);
	}
	catch (Throwable ex) {throw ex;} finally {ticBussy = false;}
}
// -------------------------------------------------------------------
private void tryDayTic(long now) {
	try {
		long $23h = 23 * 3600000L;// not less 23 hours since last check
		int $chkHour = 4;// after 4 AM
		long dayTicLastTime = Tool.toLong(MCache.getValue(CacheKeys.DAY_LAST_TIC));
		Calendar date = Tool.nowDate();
		int h = date.get(Calendar.HOUR_OF_DAY);
		if (h == $chkHour && now - dayTicLastTime > $23h) {
			boolean ok = MCache.saveValue(CacheKeys.DAY_LAST_TIC, now);
			if (ok) {
				if (now - Tool.toLong(MCache.getValue(CacheKeys.LAST_FTDB_INIT)) > $23h)
					addTask(Queues.DEFAULT, Tasks.FTDB_INIT, null, 0, 0, null, false);
				//
//			ok = addTask(Queues.DEFAULT, Tasks.REPORT_DAY, null, 300000, 0, new RetryOptionsExt(4, 60, 600, 0, 3600), false);
				// Backend task
				addTask(Queues.DEFAULT, Tasks.FTDB_OPTIMIZE, null, 600000, 0, null, true);
			}
			if (!ok) MCache.saveValue(CacheKeys.DAY_LAST_TIC, 0);
			log.info("[DAY TIC] ok? " + ok);
		}
	} catch (Throwable ex) {log.warning("DAY TIC Failed. > " + ex);}
}
// -------------------------------------------------------------------
private void tryResetQuotas(long now) {
	try {
		Calendar date = Tool.nowDate();
		int h = date.get(Calendar.HOUR_OF_DAY);
		int m = date.get(Calendar.MINUTE);
		if (h == 10 && m >= 0 && m <= 4) {
			boolean ok = Backend.resetTimeUsed();
			log.info("[Reset quotas] ok? " + ok);
		}
	} catch (Throwable ex) {log.warning("Reset quotas failed > " + ex);}
}

/** TASKS */

private void refreshConfig(long now) {
	try {
		long lastChk = Tool.toLong(MCache.getValue(CacheKeys.CFG_LAST_CHK));
		if (now - lastChk > 10 * 60 * 1000)//TODO 15 min
		{
			FTDB.loadConfig();
			MCache.saveValue(CacheKeys.CFG_LAST_CHK, now);
		}
	} catch (Throwable ex) {log.warning("[refreshConfig] failed > " + ex);}
}



// ===================================================
// -------------------------------------------------------------------
private String readContent(HttpServletRequest req) throws IOException {
	char[] buff = new char[req.getContentLength()];
	Reader reader = req.getReader();
	reader.read(buff);
	String content = String.valueOf(buff);
	content = URLDecoder.decode(content, Tool.UTF8);
	return content;
}


// ===================================================
public static class RetryOptionsExt {
	public RetryOptions opts;
	public int retries;
	public RetryOptionsExt(int _retries, double minBackoff, double maxBackoff, int doublings, long ageSecs) {
		retries = _retries;
		opts = RetryOptions.Builder.withDefaults();
		if (retries >= 0) opts.taskRetryLimit(retries);
		if (minBackoff > 0 && maxBackoff > 0) {
			opts.minBackoffSeconds(minBackoff).maxBackoffSeconds(maxBackoff);
			if (doublings >= 0) opts.maxDoublings(doublings);
		}
		if (ageSecs > 0) opts.taskAgeLimitSeconds(ageSecs);
	}
}


// ===================================================
public static abstract class DeferredTaskExt implements DeferredTask {
	public int maxRetries = 0;
	public int retries = 0;
	@Override public abstract void run();

	public boolean isEnough() {
		boolean enough = retries > maxRetries;
		retries++;
		return enough;
	}
}




// ===================================================
//public static class InitTask implements DeferredTask
//{
//private static final long serialVersionUID = 4220913123352379393L;
////
//@Override public void run()
//{
//	log.info("    [DEFERRED TASK RUNING] ............................");
//	try
//	{
//		App.init();
//	}
//	catch (Exception ex)
//	{
//		log.severe("[InitTask Error] :"+Tool.stackTrace(ex));
//	}
//}
//}


}
