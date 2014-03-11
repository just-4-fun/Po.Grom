package cyua.gae.appserver.urlfetch;

/*******************************************/
public enum Scope
{
FUSION_OLD("fusiontables"),
FUSION("https://www.googleapis.com/auth/fusiontables"),
DOCS("writely");
String id;
Scope(String _id) {id = _id;}
}