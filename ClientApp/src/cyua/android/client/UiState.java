package cyua.android.client;

import android.net.Uri;

import java.io.File;

import cyua.android.core.log.Wow;
import cyua.android.core.ui.UiService;


public class UiState extends UiService.UiStateCore {

public boolean processing;
public Uri[] photos = new Uri[8];
public File[] photoFiles = new File[8];

public UiState() {
}


public boolean hasPhotos() {
	for (Object uri : photos) { if (uri != null) return true;}
	return false;
}
public void clearPhotos() {
	for (File file : photoFiles) {
		if (file == null) continue;
		try { file.delete(); } catch (Exception ex) {Wow.e(ex);}
	}
	//
	photos = new Uri[8];
	photoFiles = new File[8];
	processing = false;
}
}
