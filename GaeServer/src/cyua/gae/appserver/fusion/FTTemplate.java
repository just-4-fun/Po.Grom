package cyua.gae.appserver.fusion;

import java.io.Serializable;

public class FTTemplate implements Serializable
{
private static final long serialVersionUID = -3719390553648439898L;
String templateId,// unique within the context of a particular table
tableId,
name, // Optional
body; // HTML to customize the contents
Boolean isDefaultForTable;
String[] automaticColumnNames;// Only one of body or automaticColumnNames can be specified.


// -------------------------------------------------------------------
FTTemplate()
{
// No args constructor is required by Gson
}


public FTTemplate(FTTable tab, String id, boolean isdefault, String[] colNames)
{
	tableId = tab.tableId;
	templateId = id;
	isDefaultForTable = isdefault;
	automaticColumnNames = colNames;
}

// -------------------------------------------------------------------
public boolean equals(FTTemplate tmp)
{
	return templateId.equals(tmp.templateId) 
			&& automaticColumnNames != null 
			&& tmp.automaticColumnNames != null 
			&& automaticColumnNames.length == tmp.automaticColumnNames.length;
}

}
