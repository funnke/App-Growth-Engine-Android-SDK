package com.hookmobile.age;

/**
 * Represents the different directions that installs can be queried upon.
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
