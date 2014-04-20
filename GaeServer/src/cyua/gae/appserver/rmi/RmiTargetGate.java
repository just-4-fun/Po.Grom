package cyua.gae.appserver.rmi;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.logging.Logger;

import cyua.gae.appserver.MessageManager;
import cyua.gae.appserver.Tool;
import cyua.gae.appserver.fusion.FTDB;
import cyua.gae.appserver.fusion.FTOperation;
import cyua.gae.appserver.fusion.FTTable;
import cyua.gae.appserver.memo.Memo;
import cyua.java.shared.Phantom;
import cyua.java.shared.RMIException;
import cyua.java.shared.RmiTargetInterface;
import cyua.java.shared.objects.ConfigSh;

import static cyua.java.shared.Phantom.Gae;



// WARNING support RMIResponse till all devices will be upgraded


public class RmiTargetGate extends RmiGateBase {
static final Logger log = Logger.getLogger(RmiTargetGate.class.getName());
//

//
public String device_id;



@Override
protected void unpackExtra(JsonObject jobj) {
//	uid = Tool.objectFromJson(jobj, RmiTargetInterface.DEVICE_EXTRA_PARAM, DeviceShd.class);
}

/** Rmi IMPLEMENTATIONS */

class InitRmi extends RmiTargetInterface.InitRmi {
	@Override public void response() throws RMIException {
		log.info("    < < < <   [InitRmi invoke] response = " + Tool.printObject(request));
		response.p1 = Gae.AWS_ACCESS_KEY_ID;
		response.p2 = Gae.AWS_SECRET_KEY;
		response.p3 = Gae.AWS_BUCKET;
		response.p4 = Gae.AWS_REGION;
		response.p5 = Gae.AWS_HOST;
		response.p6 = String.format("%1$tY%1$tm%1$td", Tool.now());
		//
		ConfigSh cfg = FTDB.getConfig();
		if (cfg != null) {
			response.phones = cfg.operators;
			response.types = cfg.types;
			// TODO photo size
		}
	}
}




class MessageSendRmi extends RmiTargetInterface.MessageSendRmi {
	@Override public void response() throws RMIException {
		log.info("    < < < <   [EventsSaveRmi invoke] response = " + Tool.printObject(request));
		response.ok = MessageManager.save(device_id, request.message);
	}
}



class LogsSendRmi extends RmiTargetInterface.LogsSendRmi {
	@Override public void response() throws RMIException {
		log.info("    < < < <   [LogsSendRmi response]  ");
		FTTable tab = FTDB.getLogsTable();
		FTOperation.insertRows(tab, request.list, 30000);
		response.ok = true;
	}
}


}
