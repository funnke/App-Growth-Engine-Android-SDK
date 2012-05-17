package com.hookmobile.age;

/**
 * An exception that provides information on AGE error.
 */
public class AgeException extends Exception {
	
	private static final long serialVersionUID = 5754443174032107538L;
	
	private int code;
	
	
	public AgeException(int code, String message) {
		super(message);
		this.code = code;
	}
	
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
