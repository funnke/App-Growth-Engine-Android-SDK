package com.hookmobile.age;

import android.annotation.SuppressLint;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * A representation of the status of a referral/invitation previously sent.  To check
 * referral status, you can invoke Discover.queryReferral() by referral id.  Keep in mind 
 * that a referral may contain one or more recipients.
 * 
 * @see Discoverer#queryReferral(long) queryReferral
 */
@SuppressLint("SimpleDateFormat")
public class Referral implements Serializable {
	
	private static final long serialVersionUID = -3687121700384016649L;
	private static SimpleDateFormat df;
	{ 
		df= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private long referralId;
	private int totalClickThrough;
	private int totalInvitee;
	private Date invitationDate;
	
	/**
     * Creates a new Referral instance. 
     * 
     */
	Referral(long referralId, String invitationDate, int totalInvitee, int totalClickThrough) {
		this.referralId = referralId;
		this.totalInvitee = totalInvitee;
		this.totalClickThrough = totalClickThrough;
		
		try {
			this.invitationDate = (invitationDate != null) ? df.parse(invitationDate) : null;
		} catch (ParseException e) {
			e.printStackTrace();
		}  
	}
	
	/**
     * Returns the referral id.
     * 
     * @return referral id.
     */
	public long getReferralId() {
		return referralId;
	}

	/**
     * Returns the total number that the referral was clicked through.
     * 
     * @return total number that the referral was clicked through.
     */
	public int getTotalClickThrough() {
		return totalClickThrough;
	}

	/**
     * Returns the total number of users invited.
     * 
     * @return total number of users invited.
     */
	public int getTotalInvitee() {
		return totalInvitee;
	}

	/**
     * Returns the invitation date 
     * 
     * @return invitation date.
     */
	public Date getInvitationDate() {
		return this.invitationDate;
	}

}
