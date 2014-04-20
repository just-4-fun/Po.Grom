package cyua.java.shared;

import java.util.List;

import cyua.java.shared.objects.LogRecordSh;
import cyua.java.shared.objects.MessageSh;



public class RmiTargetInterface {
public static final String GATE_DIR = "/rmigate";

//----------------------------------------------------

public static class InitRmi extends Rmi<InitRmi.Request, InitRmi.Response> {
	public static class Request extends Rmi.RmiRequest {
		public String uid;
	}
	public static class Response extends Rmi.RmiResponse {
		public String phones;
		public String types;
		public String p1, p2, p3, p4, p5, p6;
	}
}


public static class MessageSendRmi extends Rmi<MessageSendRmi.Request, MessageSendRmi.Response> {
	public static class Request extends Rmi.RmiRequest {
		public MessageSh message;
	}
	public static class Response extends Rmi.RmiResponse {
		public boolean ok;
	}
}


public static class LogsSendRmi extends Rmi<LogsSendRmi.Request, LogsSendRmi.Response> {
	public static class Request extends Rmi.RmiRequest {
		public List<LogRecordSh> list;
	}
	public static class Response extends Rmi.RmiResponse {
		public boolean ok;
	}
}

/*
public static class RequestUrlsRmi extends Rmi<RequestUrlsRmi.Request, RequestUrlsRmi.Response> {
	public static class Request extends Rmi.RmiRequest {
		public String uid;
		public int pairNum;// num of pairs (thumb, photo)
	}
	public static class Response extends Rmi.RmiResponse {
		public List<String> urls;
	}
}
*/

}
