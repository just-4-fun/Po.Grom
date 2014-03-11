package cyua.android.core.misc;

import java.util.ArrayList;
import java.util.ListIterator;


/** Created by far.be on 7/21/13. */
public class Listeners<T> {

private ListenersList<T> list;

public Listeners() {
	list = new ListenersList<T>();
}
public boolean add(T object) {
	return list.addListener(object);
}
public boolean remove(T object) {
	return list.removeListener(object);
}
public boolean hasNext() {
	return list.hasNext();
}
public T next() {
	return list.next();
}
public void clear() {
	list.clear();
}
public int size() {
	return list.size();
}
public boolean isEmpty() {
	return list.isEmpty();
}





/** WRAPPER */

private class ListenersList<T> extends ArrayList<T> {

	private ListIterator<T> iterator;
	private T current;

	private boolean hasNext() {
		synchronized (this) {
			if (iterator == null) iterator = listIterator();
			if (iterator.hasNext()) current = iterator.next();
			else { iterator = null; current = null; }
			return iterator != null;
		}
	}

	private T next() {
		return current;
	}

	private boolean addListener(T object) {
		if (object == null || contains(object)) return false;
		synchronized (this) {
			if (iterator != null) iterator.add(object);
			else add(object);
		}
		return true;
	}

	private boolean removeListener(T object) {
		if (!contains(object)) return false;
		synchronized (this) {
			if (iterator != null) {
				if (current == object) iterator.remove();
				else if (!removeForth(object)) removeBack(object);
			}
			else remove(object);
		}
		return true;
	}

	private boolean removeForth(Object object) {
		int ix = iterator.nextIndex();
		boolean done = false;
		while (iterator.hasNext()) {
			if (iterator.next() != object) continue;
			iterator.remove();
			done = true;
		}
		while (iterator.nextIndex() > ix) iterator.previous();
		return done;
	}
	private boolean removeBack(Object object) {
		int ix = iterator.nextIndex();
		boolean done = false;
		while (iterator.hasPrevious()) {
			if (iterator.previous() != object) continue;
			iterator.remove();
			done = true;
			ix--;
		}
		while (iterator.nextIndex() < ix) iterator.next();
		return done;
	}
}

}