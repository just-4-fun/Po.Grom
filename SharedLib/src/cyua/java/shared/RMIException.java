package cyua.java.shared;

public class RMIException extends Exception
{
private static final long serialVersionUID = -4778837189099741040L;
//
public static final int FAIL_CODE = 583;
public static final int RETRY_CODE = 584;
public static final int FAIL_BEFORE_CODE = 0;// on client side before request
public static final int FAIL_AFTER_CODE = 1;// on client side after request
public static final int CANCELED_CODE = 2;
//
public int httpCode;
//-------------------------------------------------------------------
public RMIException(String message)
{
	this(FAIL_CODE, message);
}
//-------------------------------------------------------------------
public RMIException(int code, String message)
{
	super(message);
	httpCode = code;
}
//-------------------------------------------------------------------
public RMIException(int code, String message, Throwable cause)
{
	super(message, cause);
	httpCode = code;
}
//-------------------------------------------------------------------
public RMIException(int code, Throwable cause)
{
	super(cause);
	httpCode = code;
}
//-------------------------------------------------------------------
public RMIException(Throwable cause)
{
	super(cause);
	httpCode = FAIL_CODE;
}
@Override public String toString() {
	return getClass().getName()+"; httpCode = "+httpCode+"; message = "+getMessage();
}

}