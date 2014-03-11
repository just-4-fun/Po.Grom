package cyua.android.core.misc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

import cyua.android.core.AppCore;
import cyua.android.core.log.Wow;

import static cyua.android.core.AppCore.D;


public class CachedFileProvider extends ContentProvider {

private static final String TAG = "CachedFileProvider";
public static final String AUTHORITY = "just4fun.mailattach.provider";
private UriMatcher uriMatcher;

@Override
public boolean onCreate() {
	uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	uriMatcher.addURI(AUTHORITY, "*", 1);
	return true;
}

@Override
public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
	if (D) Wow.i(TAG, "Called with uri: '" + uri + "'." + uri.getLastPathSegment());
	switch (uriMatcher.match(uri)) {
		case 1:// If it returns 1 - then it matches the Uri defined in createRoot
			String fileLocation = AppCore.context().getCacheDir() + File.separator + uri.getLastPathSegment();
			ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(fileLocation), ParcelFileDescriptor.MODE_READ_ONLY);
			return pfd;
		default:// Otherwise unrecognised Uri
			Wow.i(TAG, "Unsupported uri: '" + uri + "'.");
			throw new FileNotFoundException("Unsupported uri: " + uri.toString());
	}
}

@Override public int update(Uri uri, ContentValues contentvalues, String s, String[] as) { return 0; }
@Override public int delete(Uri uri, String s, String[] as) { return 0; }
@Override public Uri insert(Uri uri, ContentValues contentvalues) { return null; }
@Override public String getType(Uri uri) { return null; }
@Override public Cursor query(Uri uri, String[] projection, String s, String[] as1, String s1) { return null; }
}