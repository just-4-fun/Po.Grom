package cyua.android.client;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import cyua.android.core.ActivityCore;
import cyua.android.core.log.Wow;
import cyua.android.core.misc.Sequence;
import cyua.android.core.misc.Tool;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiHelper;
import cyua.android.core.ui.UiService;

import static android.app.Activity.RESULT_OK;
import static cyua.android.core.AppCore.D;


/** PHOTO CLICK LISTENER */

class PhotoPicker implements View.OnClickListener, ActivityCore.ActivityResultListener {
private static final String TAG = "PhotoPicker";

public static void setPhoto(File file, int ix) {
	UiState state = (UiState) (UiService.getUiState());
	state.photos[ix] = file == null ? null : Uri.fromFile(file);
	state.photoFiles[ix] = file;
}
public static Uri getPhotoUri(int ix) { return ((UiState) (UiService.getUiState())).photos[ix]; }



/** INSTANCE API */

final int REQUEST_IMAGE_CAPTURE = 0xF01;
ImageButton button;
int thumbIx, photoIx;
Uri photoUri;
UiState state;

PhotoPicker(ImageButton btn, int ix) {
	state = (UiState) UiService.getUiState();
	button = btn;
	thumbIx = ix * 2;
	photoIx = thumbIx + 1;
	button.setOnClickListener(this);
	updateState();
}
public boolean hasContent() { return state.photos[thumbIx] != null; }

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public void updateState() {
	int dRid = hasContent() ? R.drawable.btn_photo_remove : R.drawable.btn_photo_add;
	button.setImageDrawable(UiHelper.drawable(dRid));
	InputStream is = null;
	BitmapDrawable bmpd = null;
	if (hasContent()) {
		try {
			is = App.context().getContentResolver().openInputStream(getPhotoUri(thumbIx));
			bmpd = new BitmapDrawable(App.context().getResources(), is);
		} catch (Exception ex) {Wow.e(ex);}
	}
	if (App.apiVersion >= 16) button.setBackground(bmpd);
	else button.setBackgroundDrawable(bmpd);
}
@Override public void onClick(View v) {
	if (!hasContent()) {
		String err = requestPhoto();
		if (err != null) ;// TODO show error message
	}
	else {
		setPhoto(null, thumbIx);
		setPhoto(null, photoIx);
		updateState();
	}
}
String requestPhoto() {
	try {
		String phName = String.format("%1$tH%1$tM%1$tS", Tool.now());
		File file = createImageFile(phName, false);
		photoUri = Uri.fromFile(file);
		if (D) Wow.v(TAG, "requestNewPhoto", "photoUri = " + photoUri);
		PackageManager pm = App.context().getPackageManager();
		// Camera
		final List<Intent> cameraIntents = new ArrayList<Intent>();
		final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		final List<ResolveInfo> list = pm.queryIntentActivities(captureIntent, 0);
		for (ResolveInfo res : list) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(captureIntent);
			intent.setComponent(new ComponentName(packageName, res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
//			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
			cameraIntents.add(intent);
		}
		// Gallery
		final Intent galleryIntent = new Intent();
		galleryIntent.setType("image/*");
		galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
		// Chooser of filesystem options.
		final Intent chooserIntent = Intent.createChooser(galleryIntent, UiHelper.string(R.string.title_photo_source));
		// Add the camera options.
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
		//
		App.uiContext().startActivityForResult(this, chooserIntent, REQUEST_IMAGE_CAPTURE);
		return null;
	} catch (Exception ex) {Wow.e(ex); return ex.getLocalizedMessage();}
}
public static File createImageFile(String filename, boolean temp) throws IOException {
	File dir = temp ? App.context().getCacheDir() :
			new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), UiHelper.string(R.string.app_name));
	if (!dir.exists()) dir.mkdirs();
	return new File(dir.getPath() + File.separator + filename + ".jpg");
}
@Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
//if (D) Wow.v(TAG, "onActivityResult", "requestCode = "+requestCode, "resultCode = "+resultCode, "data = "+data);
	if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
		boolean isCamera = data == null || MediaStore.ACTION_IMAGE_CAPTURE.equals(data.getAction());
		if (!isCamera) {
			photoUri = data.getData();
			if (photoUri == null || !isValidMimeType(photoUri)) return;
		}
		else publishPhotoToGallery();
		if (D) Wow.v(TAG, "onActivityResult", "photoUri = " + photoUri);
		new PhotoProcessor(this);
	}
}
private boolean isValidMimeType(Uri uri) {
	String mimetype = App.context().getContentResolver().getType(uri);
//	if (D) Wow.v(TAG, "isValidMimeType", "uri="+uri ,"mimetype="+mimetype);
	return mimetype == null || mimetype.contains("image");
}
private void publishPhotoToGallery() {
	Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	intent.setData(photoUri);
	App.context().sendBroadcast(intent);
}





/** PHOTO PROCESSOR */

static class PhotoProcessor extends Sequence {
	final int thMaxSize = 100;
	final int phMaxSize = 1000;// TODO 2000
	final int defaultDensity = 160;
	int thumbIx, photoIx;
	Uri photoUri;
	UiState state;

	PhotoProcessor(PhotoPicker picker) {
		thumbIx = picker.thumbIx;
		photoIx = picker.photoIx;
		photoUri = picker.photoUri;
	}

	@Override protected void doInBackground() throws Exception {
		state = (UiState) UiService.getUiState();
		state.processing = true;
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		// Get the dimensions of the bitmap
		bmOptions.inJustDecodeBounds = true;
		InputStream is = App.context().getContentResolver().openInputStream(photoUri);
		BitmapFactory.decodeStream(is, null, bmOptions);
		double origW = bmOptions.outWidth;
		double origH = bmOptions.outHeight;
		// Resize photo to avoid OutOfMemory error
		int halfSampleRatio = (int)(Math.max(origH / phMaxSize, origW / phMaxSize) / 2);
		bmOptions.inSampleSize = 2 * halfSampleRatio;
		//
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inPurgeable = true;
		is = App.context().getContentResolver().openInputStream(photoUri);
		Bitmap origBmp = BitmapFactory.decodeStream(is, null, bmOptions);
		origW = bmOptions.outWidth;
		origH = bmOptions.outHeight;
		if (D) Wow.v(TAG, "processPhoto", "origW = " + origW, "origH = " + origH, "density = " + origBmp.getDensity());
//	String mimeType = bmOptions.outMimeType;//image/jpeg; image/png;
		float densityRatio = 1;//(float)defaultDensity / origBmp.getDensity();
		int thMaxSizeCurr = (int) (thMaxSize * densityRatio);
		int phMaxSizeCurr = (int) (phMaxSize * densityRatio);
		boolean land = origW > origH;
		// photo name part
		String phName = photoUri.getLastPathSegment();
		int extIx = phName.lastIndexOf('.');
		if (extIx > 0) phName = phName.substring(0, extIx);
		phName += "_" + Settings.uid.get();
		// Photo
		Bitmap resized = origBmp;
		if (origH > phMaxSizeCurr || origW > phMaxSizeCurr) {
			int newW = land ? phMaxSizeCurr : (int) (origW * (phMaxSizeCurr / origH));
			int newH = land ? (int) (origH * (phMaxSizeCurr / origW)) : phMaxSizeCurr;
			resized = Bitmap.createScaledBitmap(origBmp, newW, newH, true);
		}
		File file = createImageFile(phName, true);
		OutputStream outs = new FileOutputStream(file);
		resized.compress(Bitmap.CompressFormat.JPEG, 100, outs);
		setPhoto(file, photoIx);
		if (D)
			Wow.v(TAG, "processPhoto", "newW = " + resized.getWidth(), "newH = " + resized.getHeight(), "newDensity = " + resized.getDensity(), "bytes = " + file.length(), "uri = " + getPhotoUri(photoIx));
		// Thumb
		int newW = land ? thMaxSizeCurr : (int) (origW * (thMaxSizeCurr / origH));
		int newH = land ? (int) (origH * (thMaxSizeCurr / origW)) : thMaxSizeCurr;
		resized = Bitmap.createScaledBitmap(origBmp, newW, newH, true);
		file = createImageFile(phName + "_small", true);
		outs = new FileOutputStream(file);
		resized.compress(Bitmap.CompressFormat.JPEG, 100, outs);
		setPhoto(file, thumbIx);
		if (D)
			Wow.v(TAG, "processPhoto", "newW = " + resized.getWidth(), "newH = " + resized.getHeight(), "newDensity = " + resized.getDensity(), "bytes = " + file.length(), "uri = " + getPhotoUri(thumbIx));
	}
	@Override protected void onFinish(boolean isOk) {
		new UiCore.UiAction(Ui.UiOp.UPDATE_PAGE).execute();
		state.processing = false;
//		if (pickerWr.get() != null) pickerWr.get().updateState();
	}

}

}
