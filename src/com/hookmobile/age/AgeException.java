package com.hookmobile.age;

/**
 * An exception that provides information on AGE error.
 */
public class AgeException extends Exception {
	
	private static final long serialVersionUID = 5754443174032107538L;

	public static final int E_SUCCESS							= 1000;
	public final static int E_INSTALL_NOT_FOUND 				= 2000;
	public final static int E_NOT_YET_DISCOVERED 				= 2001;							
	public final static int E_AUTHENTICATION_FAILED 			= 2002;
	public final static int E_INVALID_REFERRAL_ID 				= 2011;
	public final static int E_HASHING_MISMATCH 					= 2014;
	public final static int E_SERVICE_DISABLED 					= 3500;
	public final static int E_QUERY_REFERRAL_DISABLED 			= 3501;						
	public final static int E_ADDRESSBOOK_EXPIRED 				= 3502;							
	public final static int E_REFERRAL_TARGET_URL_NOT_CONFIGURED = 3600;

	// retryable error
	public final static int E_SYSTEM_BUSY 						= 3000;
	public final static int E_DAILY_INSTALL_QUOTA_EXCEEDED 		= 3020;
	public final static int E_MONTHLY_INSTALL_QUOTA_EXCEEDED 	= 3021;
	public final static int E_LIFETIME_INSTALL_QUOTA_EXCEEDED 	= 3022;
	public final static int E_API_CALL_THROTTLED_NEW_INSTALL 	= 3023;
	public final static int E_API_CALL_THROTTLED_NEW_ORDER 		= 3024;
	public final static int E_API_CALL_THROTTLED_QUERY_ORDER 	= 3025;

	private int code;
	
	/**
	 * Creates a new AgeException instance from a status code and message string
	 * 
	 * @param code error code.
	 * @param message error message.
	 */
	public AgeException(int code, String message) {
		super(message);
		this.code = code;
	}
	
	/**
	 * Creates a new AgeException instance from a Throwable.
	 * 
	 * @param cause cause implements Throwable.
	 */
	public AgeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Gets AGE status code.
	 * 
	 * @return the status code.
	 */
	public int getCode() {
		return code;
	}
	
}
