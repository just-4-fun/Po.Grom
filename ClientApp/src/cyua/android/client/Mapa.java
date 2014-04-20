package cyua.android.client;

import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import cyua.android.core.map.MapCore;
import cyua.android.core.misc.EasyList;
import cyua.android.core.ui.ToolbarCore;
import cyua.android.core.ui.UiCore;
import cyua.android.core.ui.UiService;



public class Mapa extends MapCore implements UiCore.iFragment<Ui, UiState> {
/* Similar to UiFragment class */
List<View> toolbarLefts, toolbarRights, toolbarCenters, titlebarLefts, titlebarRights, titlebarCenters;
Marker marker;
UiState uiState;
@Override public void init(Ui ui, UiState state, boolean firstInit) {
	uiState = state;
	if (uiState.mapLat != 0) {
		MarkerOptions mopts = new MarkerOptions();
		mopts.position(new LatLng(uiState.mapLat, uiState.mapLng));
		marker = add(mopts);
	}
}
@Override public List<View> getToolbarCenters() {
	Button sendBtn = Fmt.createToolButton(R.string.btn_ok);
	sendBtn.setOnClickListener(new View.OnClickListener() {
		@Override public void onClick(View v) {onSelectLocation(); }
	});
	return new EasyList<View>().plus(sendBtn);
}
private void onSelectLocation() {
	if (marker != null) {
		uiState.mapLat = marker.getPosition().latitude;
		uiState.mapLng = marker.getPosition().longitude;
		GeocodeResponse.assignGeocode();
	}
	new UiCore.UiAction(Ui.UiOp.LOCATION_SELECTED).execute();
}
@Override public List<View> getToolbarLefts() { return null; }
@Override public List<View> getToolbarRights() { return null; }
@Override public List<View> getTitlebarCenters() { return null; }
@Override public List<View> getTitlebarLefts() { return null; }
@Override public List<View> getTitlebarRights() { return null; }
@Override public void initContent(boolean firstInit) {
//		if (D) Wow.v(TAG, "initContent", this.getClass().getSimpleName());
	Ui ui = (Ui) UiService.getUi();
	toolbarLefts = getToolbarLefts();
	toolbarRights = getToolbarRights();
	toolbarCenters = getToolbarCenters();
	titlebarLefts = getTitlebarLefts();
	titlebarRights = getTitlebarRights();
	titlebarCenters = getTitlebarCenters();
	ToolbarCore titleBar = ui.titleBar;
	ToolbarCore toolBar = ui.toolBar;
	toolBar.leftAdd(toolbarLefts).rightAdd(toolbarRights).centerAdd(toolbarCenters);
	titleBar.leftAdd(titlebarLefts).rightAdd(titlebarRights).centerAdd(titlebarCenters);
	init(ui, (UiState) UiService.getUiState(), firstInit);
}
@Override public void removeContent() {
//		if (D) Wow.v(TAG, "removeContent", this.getClass().getSimpleName());
	Ui ui = (Ui) UiService.getUi();
	ToolbarCore toolBar = ui.toolBar;
	ToolbarCore titleBar = ui.titleBar;
	toolBar.leftRemove(toolbarLefts).rightRemove(toolbarRights).centerRemove(toolbarCenters);
	titleBar.leftRemove(titlebarLefts).rightRemove(titlebarRights).centerRemove(titlebarCenters);
}

/**/

@Override public void onMapClick(LatLng coord) {
	if (marker != null) marker.remove();
	MarkerOptions mopts = new MarkerOptions();
	mopts.position(coord);
	marker = add(mopts);
}
}
