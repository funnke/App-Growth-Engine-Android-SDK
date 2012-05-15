package com.hookmobile.age;

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

	public int getCode() {
		return code;
	}
	
}
