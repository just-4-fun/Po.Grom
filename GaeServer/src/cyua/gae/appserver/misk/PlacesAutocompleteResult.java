package cyua.gae.appserver.misk;


public class PlacesAutocompleteResult
{
public String status;//OK ,ZERO_RESULTS ,OVER_QUERY_LIMIT ,REQUEST_DENIED ,INVALID_REQUEST 
public Prediction[] predictions;


public static class Prediction
{
public String description;// contains the human-readable name for the returned result.
public String reference;// contains a unique token that you can use to retrieve additional information
public String id;// contains a unique stable identifier denoting this place.
public Term[] terms;
public String[] types;
}

public static class Term
{
public String value;
public int offset;
}


}
