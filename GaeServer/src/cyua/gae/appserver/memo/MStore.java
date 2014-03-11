package cyua.gae.appserver.memo;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import cyua.gae.appserver.App;
import cyua.gae.appserver.Tool;
import cyua.java.shared.RMIException;

public class MStore
{
private static final Logger log = Logger.getLogger(MStore.class.getName());
//
static final long INSIST_SPAN = 10000;
//
static enum DSOp {SAVE, SAVE_ALL, DELETE, DELETE_ALL}


// ===================================================
//-------------------------------------------------------------------
protected static DatastoreService getDatastore(boolean write) throws RMIException
{
if (!App.isServiceAvailable(write ? Capability.DATASTORE_WRITE : Capability.DATASTORE)) throw new RMIException("Datastore is not available.");
DatastoreService store = DatastoreServiceFactory.getDatastoreService();
if (store == null) throw new RMIException("Datastore is null.");
return store;
}

// ===================================================
// DATASTORE OPERATIONS that try to cope with ConcurrentModificationException
//-------------------------------------------------------------------
static boolean saveEntity(Entity entity)
{
return datastoreOperation(DSOp.SAVE, entity);
}

//-------------------------------------------------------------------
static boolean saveEntities(Iterable<Entity> entities)
{
return datastoreOperation(DSOp.SAVE_ALL, entities);
}

//-------------------------------------------------------------------
static boolean deleteEntity(Key key)
{
	return datastoreOperation(DSOp.DELETE, key);
}

//-------------------------------------------------------------------
static boolean deleteEntities(Iterable<Key> kies)
{
	return datastoreOperation(DSOp.DELETE_ALL, kies);
}

//-------------------------------------------------------------------
static boolean datastoreOperation(DSOp op, Object object)
{
	if (object == null) return true;
	DatastoreService store = null;
	long expireMs = Tool.now()+INSIST_SPAN;
	if (op == DSOp.SAVE_ALL || op == DSOp.DELETE_ALL) expireMs += 2000;
	while(Tool.now() < expireMs)
	{
		Transaction ta = null;
		try
		{
			store = getDatastore(true);
			try {store.getCurrentTransaction();}
			catch (NoSuchElementException ex) {ta = store.beginTransaction();}
			catch (IllegalStateException ex) {ta = store.beginTransaction();}
			switch (op)
			{
				case SAVE: store.put((Entity)object); break;
				case DELETE: store.delete((Key)object); break;
				case SAVE_ALL: store.put((Iterable<Entity>)object); break;
				case DELETE_ALL: store.delete((Iterable<Key>)object); break;
			}
			if (ta != null && ta.isActive()) ta.commit();// << TA
			return true;
		}
		catch (ConcurrentModificationException ex)
		{
			try{Thread.sleep(100);}catch (InterruptedException exx) {}
			continue;
		}//DatastoreTimeoutException, ApiProxy$OverQuotaException
		catch (Throwable ex)
		{
			log.severe(op+" failed. "+ex.toString());
			break;
		}
		finally
		{
			if (ta != null && ta.isActive()) try {ta.rollback();} catch (Throwable ex){}
		}
	}
	return false;
}


}
