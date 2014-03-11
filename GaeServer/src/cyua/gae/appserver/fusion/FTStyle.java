package cyua.gae.appserver.fusion;

import java.io.Serializable;

public class FTStyle implements Serializable
{
private static final long serialVersionUID = 8022126825402720697L;
//
String tableId,
styleId,
name;
Boolean isDefaultForTable;
MarkerOptions markerOptions;
PolylineOptions polylineOptions;
PolygonOptions polygonOptions;


FTStyle()
{
// No args constructor is required by Gson
}

FTStyle(FTTable tab, String id, boolean isdefault)
{
tableId = tab.tableId; styleId = id;
isDefaultForTable = isdefault;
markerOptions = new MarkerOptions();
polylineOptions = new PolylineOptions();
polygonOptions = new PolygonOptions();
}

// ===================================================
static class MarkerOptions implements Serializable
{
private static final long serialVersionUID = -3982755281992136611L;
String iconName;
IconStyler iconStyler;
public MarkerOptions() {this("small_red");}
public MarkerOptions(String name){iconName = name;}
public MarkerOptions(FTColumn col)
{
	iconStyler = new IconStyler();
	iconStyler.columnName = col.name;
}
}
// -------------------------------------------------------------------
static class IconStyler implements Serializable
{
private static final long serialVersionUID = -8404467017518765995L;
//String kind;// fusiontables#fromColumn or fusiontables#buckets
String columnName; // Name of the column whose value is used in the style
IconStylerBucket[] buckets;
}
// -------------------------------------------------------------------
static class IconStylerBucket implements Serializable // Bucket function that assigns the marker icon based on the range a column value falls into
{
private static final long serialVersionUID = -1218690353238911162L;
double min, max; // value in the selected column for a row to be styled according to the icon
String icon;// Icon name used for a point.
}

// ===================================================
static class PolylineOptions implements Serializable
{
private static final long serialVersionUID = -5090105856390115525L;
String strokeColor;// Color of the line in hexadecimal notation (#RRGGBB).
Double strokeOpacity;// Opacity of the line : 0.0 (transparent) to 1.0 (opaque).
Integer strokeWeight;//Width of the line in pixels.
StrokeColorStyler strokeColorStyler;
StrokeWeightStyler strokeWeightStyler;
public PolylineOptions() {this("#ff0000", 0.5, 2);}
public PolylineOptions(String c, double a, int w) {strokeColor = c; strokeOpacity = a; strokeWeight = w;}
public PolylineOptions(FTColumn col)
{
	strokeColorStyler = new StrokeColorStyler();
	strokeColorStyler.columnName = col.name;
}
}

// -------------------------------------------------------------------
static class StrokeColorStyler implements Serializable
{
private static final long serialVersionUID = 2223929555887472604L;
//String kind; // fusiontables#fromColumn or fusiontables#gradient of fusiontables#gradient (for PolygonOptions)
String columnName;
Gradient gradient;
ColorStylerBucket[] buckets;
}
// -------------------------------------------------------------------
static class Gradient implements Serializable
{
private static final long serialVersionUID = 2743970627317172800L;
double min,// Lower-end of the interpolation range: rows with this value will be assigned to colors[0].
max;//Higher-end of the interpolation range: rows with this value will be assigned to colors[n-1].
Color[] colors;
}
// -------------------------------------------------------------------
static class Color implements Serializable
{
private static final long serialVersionUID = 2720476118701162163L;
String color;//Color in hexadecimal notation (#RRGGBB).
double opacity;//Opacity of the color: 0.0 (transparent) to 1.0 (opaque).
}
// -------------------------------------------------------------------
static class ColorStylerBucket implements Serializable
{
private static final long serialVersionUID = 6342330377859427475L;
double min,// Minimum value in the selected column for a row to be styled according to the bucket color and/or opacity.
max,//Maximum value in the selected column for a row to be styled according to the bucket color and/or opacity
opacity;//Opacity of the color: 0.0 (transparent) to 1.0 (opaque).
String color;//Color in hexadecimal notation (#RRGGBB).
}

// -------------------------------------------------------------------
static class StrokeWeightStyler implements Serializable
{
private static final long serialVersionUID = -1281035247096184278L;
//String kind;// fusiontables#fromColumn or fusiontables#buckets
String columnName;
StrokeWeightStylerBucket[] buckets;
}
// -------------------------------------------------------------------
static class StrokeWeightStylerBucket implements Serializable
{
private static final long serialVersionUID = 5028668500657025711L;
double min, max;//value in the selected column for a row to be styled according to the weight.
int weight;// Width of a line (in pixels)
}

// ===================================================
static class PolygonOptions implements Serializable
{
private static final long serialVersionUID = 4851500333323911130L;
String strokeColor;//Color of the polygon border in hexadecimal notation (#RRGGBB).
double strokeOpacity;//Opacity of the polygon border: 0.0 (transparent) to 1.0 (opaque).
int strokeWeight;//Width of the polyon border in pixels
String fillColor;//Color of the interior of the polygon in hexadecimal notation (#RRGGBB).
double fillOpacity;//Opacity of the interior of the polygon: 0.0 (transparent) to 1.0 (opaque).
StrokeColorStyler strokeColorStyler;
StrokeWeightStyler strokeWeightStyler;
FillColorStyler fillColorStyler;
public PolygonOptions()
{
	strokeColor = "#666666";
	strokeOpacity = 1.0;
	strokeWeight = 1;
	fillColor = "#ff0000";
	fillOpacity = 0.5;
}
}
// -------------------------------------------------------------------
static class FillColorStyler implements Serializable
{
private static final long serialVersionUID = -7344266316921201776L;
//String kind;// fusiontables#fromColumn or fusiontables#gradient or fusiontables#buckets
String columnName;
Gradient gradient;
ColorStylerBucket[] buckets;
}

}

