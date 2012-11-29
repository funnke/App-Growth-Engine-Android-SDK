package com.hookmobile.age;

import java.io.Serializable;

/**
 * A representation of the lead, or recommended invite, which includes phone number and device type.  
 * List of recommended invitation leads can be queried by issuing Discoverer.queryLeads().
 * @see Discoverer#queryLeads() queryLeads
 * 
 */
public class Lead implements Serializable {
	
	private static final long serialVersionUID = -1588467918247115146L;
	
	/**
	 * OS Type representing Android operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_ANDROID		= "android";
	/**
	 * OS Type representing IOS operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_IOS			= "ios";
	/**
	 * OS Type representing Windows Mobile operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_WINDOWS 		= "windows";
	/**
	 * OS Type representing Symbian operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_SYMBIAN 		= "symbian";
	/**
	 * OS Type representing RIM operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_RIM			= "rim";
	/**
	 * OS Type representing Non-smart phone operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_NON_SMART 	= "feature";
	/**
	 * OS Type representing Unknown operating system.
	 * @see Lead#getOsType()
	 */
	public static final String OS_TYPE_UNKNOWN 		= "unknown";

	private String phone;
	private String osType;
	
	/**
     * Creates a new Lead instance. 
     * 
     */
	public Lead(String phone, String osType) {
		this.phone = phone;
		this.osType = osType;
	}
	
	/**
     * Returns the phone number in international E.164 format.
     * Example: +12023334444
     * 
     * @return phone number.
     */
	public String getPhone() {
		return phone;
	}

	/**
     * Sets phone number.
     * 
     * @param phone phone number.
     */
	void setPhone(String phone) {
		this.phone = phone;
	}

	/**
     * Returns the operating system type.
     * Following possible String values are returned:
     * @see #OS_TYPE_ANDROID
     * @see #OS_TYPE_IOS
     * 
     * @return operating system type.
     */
	public String getOsType() {
		return osType;
	}

	/**
     * Sets operating system type.
     * 
     * @param osType operating system type.
     */
	void setOsType(String osType) {
		this.osType = osType;
	}
	
}
