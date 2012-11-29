package com.hookmobile.age;

/**
 * Define the filter criteria for querying app user's relationship to app user's address book
 * contacts via {@link Discoverer#queryInstalls(Direction)}.
 */
public enum Direction {
	
	/**
	 * Finds contacts within your address book who has the same app.
	 */
	FORWARD,
	/**
	 * Finds other app users who has your phone number in their address book.
	 */
	BACKWARD,
	/**
	 * Finds contacts within your address book who has the same app and who also has your contact in his/her address book. 
	 */
	MUTUAL;
	
}
