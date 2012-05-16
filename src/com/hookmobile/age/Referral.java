package com.hookmobile.age;

import java.io.Serializable;

/**
 * A representation of the referral status.
 */
public class Referral implements Serializable {
	
	private static final long serialVersionUID = -3687121700384016649L;
	
	private long referralId;
	private int totalClickThrough;
	private int totalInvitee;
	private String invitationDate;
	
	
	public long getReferralId() {
		return referralId;
	}

	public void setReferralId(long referralId) {
		this.referralId = referralId;
	}
	
	public int getTotalClickThrough() {
		return totalClickThrough;
	}

	public void setTotalClickThrough(int totalClickThrough) {
		this.totalClickThrough = totalClickThrough;
	}

	public int getTotalInvitee() {
		return totalInvitee;
	}

	public void setTotalInvitee(int totalInvitee) {
		this.totalInvitee = totalInvitee;
	}

	public String getInvitationDate() {
		return invitationDate;
	}

	public void setInvitationDate(String invitationDate) {
		this.invitationDate = invitationDate;
	}
	
}
