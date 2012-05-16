package com.hookmobile.age;

import java.io.Serializable;

/**
 * A representation of the lead, or recommended invite, which includes phone number and device type.
 */
public class Lead implements Serializable {
	
	private static final long serialVersionUID = -1588467918247115146L;
	
	private String phone;
	private String osType;
	
	
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}
	
}
