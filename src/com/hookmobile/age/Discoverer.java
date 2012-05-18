package com.hookmobile.age;

import static com.hookmobile.age.AgeUtils.doPost;
import static com.hookmobile.age.AgeUtils.getAddressbook;
import static com.hookmobile.age.AgeUtils.getDeviceInfo;
import static com.hookmobile.age.AgeUtils.isEmptyStr;
import static com.hookmobile.age.AgeUtils.isSmsSupported;
import static com.hookmobile.age.AgeUtils.queryDevicePhone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * This is the central class that provides the AGE services.
 * Device verification, smart invitation, referrals tracking, and installs query.
 * You can also refer to Hook Mobile's <a href="http://hookmobile.com/android-tutorial.html" target="_blank">Android Tutorial</a> for more information.
 */
public class Discoverer {
	
	private static final String AGE_PREFERENCES         = "age_preferences";
	private static final String AGE_CURRENT_APP_KEY     = "current_app_key";
	private static final String AGE_TAG_HOOK            = "Hook";
	
	private static final String MSG_DEFAULT_REFERRAL    	= "I thought you might be interested in this app %app%, check it out here %link%";
	private static final String MSG_INSTALL_CODE_REQUIRED	= "Install code not found! Please call discover first.";
	
	private static final String P_ADDRESSBOOK           = "addressBook";
	private static final String P_APP_KEY               = "appKey";
	private static final String P_DATE                  = "date";
	private static final String P_DEVICE_INFO           = "deviceInfo";
	private static final String P_INSTALL_CODE          = "installCode";
	private static final String P_LEADS                 = "leads";
	private static final String P_NAME                  = "name";
	private static final String P_OS_TYPE               = "osType";
	private static final String P_PHONE                 = "phone";
	private static final String P_REFERRAL_ID           = "referralId";
	private static final String P_REFERRAL_MESSAGE      = "referralMessage";
	private static final String P_REFERRAL_TEMPLATE     = "referralTemplate";
	private static final String P_REFERRALS             = "referrals";
	private static final String P_REFERENCE             = "reference";
	private static final String P_SEND_NOW              = "sendNow";
	private static final String P_TOTAL_CLICK_THROUGH   = "totalClickThrough";
	private static final String P_TOTAL_INVITEE         = "totalInvitee";
	private static final String P_USE_VIRTUAL_NUMBER    = "useVirtualNumber";
	private static final String P_VERIFIED              = "verified";
	private static final String P_VERIFY_MESSAGE        = "verifyMessage";
	private static final String P_VERIFY_MT             = "verifyMt";
	
	private static Discoverer instance;
	private static Context context;
	
	private static String server = "https://age.hookmobile.com";
	private static String virtualNumber = "+13025175040";
	
	private String appKey;
	private String devicePhone;
	private String installCode;
	
	private List<Lead> cachedLeads;
	private List<String> cachedInstalls;
	private List<Referral> cachedReferrals;
    
    
	/**
	 * Activates the AGE service.
	 * 
	 * @param context the Android context. 
	 * @param appKey the app key you register on Hook Mobile developers portal.
	 */
	public static void activate(Context context, String appKey) {
		Discoverer.context = context.getApplicationContext();
		Discoverer.instance = new Discoverer(appKey);
	}
    
	/**
	 * Gets the Discoverer. 
	 * 
	 * @return the Discoverer.
	 */
	public static Discoverer getInstance() {
		if(instance != null) {
			return instance;
		}
		
		throw new IllegalStateException("Please activate first.");
	}
    
	/**
	 * Gets the Hook Moible virtual number.
	 * 
	 * @return the virtual number.
	 */
	public static String getVirtualNumber() {
		return virtualNumber;
	}
    
	private Discoverer(String appKey) {
		if(appKey == null) {
			throw new IllegalStateException("App key cannot be null.");
		}
    	
		this.appKey = appKey;
		this.devicePhone = queryDevicePhone(context);
		this.installCode = loadInstallCode();
    	
		saveCurrentAppKey(appKey);
    	
		Log.i(AGE_TAG_HOOK, "devicePhone: "+ devicePhone);
		Log.i(AGE_TAG_HOOK, "installCode: "+ installCode);
	}
    
	/**
	 * Gets this device's phone number.
	 * 
	 * @return the phone number.
	 */
	public String getDevicePhone() {
		if(devicePhone == null) {
			devicePhone = queryDevicePhone(context);
		}
    	
		return devicePhone;
	}

	/**
	 * Gets the unique code associated with this install. The install code is obtained the first time discover method is invoked.
	 * 
	 * @return the install code.
	 */
	public String getInstallCode() {
		if(installCode == null) {
			installCode = loadInstallCode();
		}
		
		return installCode;
	}
	
	/**
	 * Gets the last queried leads. 
	 * 
	 * @return the cached leads.
	 */
	public List<Lead> getCachedLeads() {
		if(cachedLeads != null) {
			return cachedLeads;
		}
		
		return Collections.<Lead>emptyList();
	}

	/**
	 * Gets the last queried installs.
	 * 
	 * @return the cached installs.
	 */
	public List<String> getCachedInstalls() {
		if(cachedInstalls != null) {
			return cachedInstalls;
		}
		
		return Collections.<String>emptyList();
	}

	/**
	 * Gets the last queried referrals.
	 * 
	 * @return the cached referrals.
	 */
	public List<Referral> getCachedReferrals() {
		if(cachedReferrals != null) {
			return cachedReferrals;
		}
		
		return Collections.<Referral>emptyList();
	}

	/**
	 * Verifies user's device phone. There are two types of verification, MT and MO.
	 * In MT verification, a confirmation link will be sent to user's phone. user is then verified by clicking the link.
	 * In MO verification, user will complete the verification by sending a verification message with an unique code.
	 * This method returns the verification message.
	 * 
	 * @param useMtVerification The type of the verification. True to use MT verification; MO verification otherwise.
	 * @param name The name of the user.
	 * @return the verification message.
	 * @throws AgeException if the AGE request failed.
	 */
	public String verifyDevice(boolean useMtVerification, String name) throws AgeException {
		try {
			String installCode = getInstallCode();
			String url = server + "/verifydevice";
			List<NameValuePair> form = new ArrayList<NameValuePair>();
			form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
			form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
			form.add(new BasicNameValuePair(P_VERIFY_MT, String.valueOf(useMtVerification)));
			form.add(new BasicNameValuePair(P_DEVICE_INFO, getDeviceInfo(context)));
			if(installCode != null) {
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
			}
			if(name != null) {
				form.add(new BasicNameValuePair(P_NAME, name));
			}
			
			AgeResponse response = doPost(url, form);
			
			if(response.isSuccess()) {
				JSONObject json = response.getJson();
				installCode = json.isNull(P_INSTALL_CODE) ? null : json.getString(P_INSTALL_CODE);
				
				saveInstallCode(installCode);
				
				return json.isNull(P_VERIFY_MESSAGE) ? null : json.getString(P_VERIFY_MESSAGE);
			}
			else {
				throw new AgeException(response.getCode(), response.getMessage());
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
    
	/**
	 * Checks if the user has been verified or not.
	 * 
	 * @return true if user has been verified; false otherwise.
	 * @throws AgeException if the AGE request failed.
	 */
	public boolean queryVerifiedStatus() throws AgeException {
		try {
			String installCode = getInstallCode();
        	
			if(installCode != null) {
				String url = server + "/isverified";
				List<NameValuePair> form = new ArrayList<NameValuePair>();
				form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
        	    
				AgeResponse response = doPost(url, form);
         		
				if(response.isSuccess()) {
					JSONObject json = response.getJson();
         			
					return Boolean.parseBoolean(json.isNull(P_VERIFIED) ? null : json.getString(P_VERIFIED));
				}
				else {
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				return false;
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
    
	/**
	 * Submits a discovery request. User's address book will be securely uploaded to AGE server for the analysis.
	 * It might take a minute for AGE to perform device detection and data mining.
	 * 
	 * @throws AgeException if the AGE request failed.
	 */
	public void discover() throws AgeException {
		try {
			String installCode = getInstallCode();
			String url = server + "/discover";
			List<NameValuePair> form = new ArrayList<NameValuePair>();
			form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
			form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
			form.add(new BasicNameValuePair(P_ADDRESSBOOK, getAddressbook(context)));
			form.add(new BasicNameValuePair(P_DEVICE_INFO, getDeviceInfo(context)));
			if(installCode != null) {
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
			}
		    
			AgeResponse response = doPost(url, form);
    		
			if(response.isSuccess()) {
				JSONObject json = response.getJson();
				installCode = json.isNull(P_INSTALL_CODE) ? null : json.getString(P_INSTALL_CODE);
				
				saveInstallCode(installCode);
			}
			else {
				throw new AgeException(response.getCode(), response.getMessage());
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
    
	/**
	 * Gets a list of recommended invites from AGE. The result is optimized and filtered by the device types
	 * specified in your app profile on Hook Mobile developers portal.
	 * 
	 * @return the leads.
	 * @throws AgeException if the AGE request failed.
	 */
	public List<Lead> queryLeads() throws AgeException {
		try {
			String installCode = getInstallCode();
        	
			if(installCode != null) {
				List<Lead> leads = new ArrayList<Lead>();
				String url = server + "/queryleads";
				List<NameValuePair> form = new ArrayList<NameValuePair>();
				form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
        	    
				AgeResponse response = doPost(url, form);
        		
				if(response.isSuccess()) {
					JSONObject json = response.getJson();
					JSONArray jsonArray = json.isNull(P_LEADS) ? null : json.getJSONArray(P_LEADS);
        			
					if(jsonArray != null) {
						int count = jsonArray.length();
    					
						for(int i=0; i < count; i++) {
							if(! jsonArray.isNull(i)) {
								JSONObject leadObj = jsonArray.getJSONObject(i);
								Lead lead = new Lead();
								lead.setPhone(leadObj.isNull(P_PHONE) ? "Unknown" : leadObj.getString(P_PHONE));
								lead.setOsType(leadObj.isNull(P_OS_TYPE) ? "Unknown" : leadObj.getString(P_OS_TYPE));
    							
								leads.add(lead);
							}
						}
					}
					this.cachedLeads = leads;
        			
					return leads;
				}
				else {
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new IllegalStateException(MSG_INSTALL_CODE_REQUIRED);
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
    
	/**
	 * Gets a list of friends who have also installed the app. There are three query modes: <br/><br/>
	 * 1. Forward - Find contacts within your address book who has the same app. <br/>
	 * 2. Backward - Find other app users who has your phone number in their address book. <br/>
	 * 3. Mutual - Find contacts within your address book who has the same app and who also has your contact in his/her address book.
	 * 
	 * @param direction the query direction.
	 * @return the list of the phone numbers.
	 * @throws AgeException if the AGE request failed.
	 */
	public List<String> queryInstalls(Direction direction) throws AgeException {
		try {
			String installCode = getInstallCode();
        	
			if(installCode != null) {
				List<String> installs = new ArrayList<String>();
				String url = server + "/queryinstalls";
				List<NameValuePair> form = new ArrayList<NameValuePair>();
				form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
				form.add(new BasicNameValuePair(P_REFERENCE, direction.name()));
        	    
				AgeResponse response = doPost(url, form);
        		
				if(response.isSuccess()) {
					JSONObject json = response.getJson();
					JSONArray jsonArray = json.isNull(P_LEADS) ? null : json.getJSONArray(P_LEADS);
        			
					if(jsonArray != null) {
						int count = jsonArray.length();
    					
						for(int i=0; i < count; i++) {
							if(! jsonArray.isNull(i)) {
								installs.add(jsonArray.getString(i));
							}
						}
					}
					this.cachedInstalls = installs;
        			
					return installs;
				}
				else {
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new IllegalStateException(MSG_INSTALL_CODE_REQUIRED);
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
    
	/**
	 * Sends a referral message to the specified phone numbers. 
	 * It is typically a list selected from the leads returned by queryLeads method.
	 * 
	 * @param phones the recipients of the invitation.
	 * @param useVirtualNumber true to send via Hook Mobile virtual number; false to send via user's phone.
	 * @param name the name of the app user or invitation sender.
	 * @return the referral ID, which can be used to track the referral status later.
	 * @throws AgeException if the AGE request failed.
	 */
	public long newReferral(List<String> phones, boolean useVirtualNumber, String name) throws AgeException {
		return newReferral(phones, useVirtualNumber, name, null);
	}

	/**
	 * Sends a referral message to the specified phone numbers.
	 * It is typically a list selected from the leads returned by queryLeads method.
	 * 
	 * @param phones the recipients of the invitation.
	 * @param useVirtualNumber true to send via Hook Mobile virtual number; false to send via user's phone.
	 * @param name the name of the app user or invitation sender
	 * @param message the message template to use. It will overwrite the default one configured in the app profile.
	 * @return the referral ID, which can be used to track the referral status later.
	 * @throws AgeException if the AGE request failed.
	 */
	public long newReferral(List<String> phones, boolean useVirtualNumber, String name, String message) throws AgeException {
		try {
			String installCode = getInstallCode();
			if(! useVirtualNumber && ! isSmsSupported(context)) {
				Log.d(AGE_TAG_HOOK, "SMS not supported, use virtual number instead.");
    			
				useVirtualNumber = true;
			}
        	
			if(installCode != null) {
				String url = server + "/newreferral";
				List<NameValuePair> form = new ArrayList<NameValuePair>();
				form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
				for(String phone : phones) {
					form.add(new BasicNameValuePair(P_PHONE, phone));
				}
				form.add(new BasicNameValuePair(P_USE_VIRTUAL_NUMBER, String.valueOf(useVirtualNumber)));
				form.add(new BasicNameValuePair(P_SEND_NOW, "true"));
				if(name != null) {
					form.add(new BasicNameValuePair(P_NAME, name));
				}
				if(! isEmptyStr(message)) {
					form.add(new BasicNameValuePair(P_REFERRAL_TEMPLATE, message));
				}
        		
				AgeResponse response = doPost(url, form);
        		
				if(response.isSuccess()) {
					JSONObject json = response.getJson();
					long referralId = json.isNull(P_REFERRAL_ID) ? -1 : json.getLong(P_REFERRAL_ID);
					String referralMessage = json.isNull(P_REFERRAL_MESSAGE) ? MSG_DEFAULT_REFERRAL : json.getString(P_REFERRAL_MESSAGE);
        			
					if(! useVirtualNumber) {
						SmsManager sms = SmsManager.getDefault();
        				
						for(String phone : phones) {
							sms.sendTextMessage(phone, null, referralMessage, null, null);
						}
					}
    				
					return referralId;
				}
				else {
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new IllegalStateException(MSG_INSTALL_CODE_REQUIRED);
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
    
	/**
	 * Returns the specified referral. 
	 * 
	 * @param referralId the referral ID.
	 * @return a Referral representing the referral.
	 * @throws AgeException if the AGE request failed.
	 */
	public Referral queryReferral(int referralId) throws AgeException {
		if(referralId > 0) {
			List<Referral> referrals = queryReferrals(referralId);
        	
			if(referrals.size() > 0) {
				return referrals.get(0);
			}
		}
    	
		return null;
	}
    
	/**
	 * Returns all referrals sent from this app user.
	 * 
	 * @return the referral list.
	 * @throws AgeException if the AGE request failed.
	 */
	public List<Referral> queryReferrals() throws AgeException {
		List<Referral> referrals = queryReferrals(0);
		this.cachedReferrals = referrals;
    	
		return referrals;
	}
    
	private List<Referral> queryReferrals(int referralId) throws AgeException {
		try {
			String installCode = getInstallCode();
        	
			if(installCode != null) {
				List<Referral> referrals = new ArrayList<Referral>();
				String url = server + "/queryreferral";
				List<NameValuePair> form = new ArrayList<NameValuePair>();
				form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
				if(referralId > 0) {
					form.add(new BasicNameValuePair(P_REFERRAL_ID, String.valueOf(referralId)));
				}
        		
				AgeResponse response = doPost(url, form);
        		
				if(response.isSuccess()) {
					JSONObject json = response.getJson();
					JSONArray jsonArray = json.isNull(P_REFERRALS) ? null : json.getJSONArray(P_REFERRALS);
        			
					if(jsonArray != null) {
						int count = jsonArray.length();
    					
						for(int i=0; i < count; i++) {
							if(! jsonArray.isNull(i)) {
								JSONObject referralObj = jsonArray.getJSONObject(i);
								Referral referral = new Referral();
								referral.setReferralId(referralObj.isNull(P_REFERRAL_ID) ? -1 : referralObj.getLong(P_REFERRAL_ID));
								referral.setInvitationDate(referralObj.isNull(P_DATE) ? "" : referralObj.getString(P_DATE));
								referral.setTotalClickThrough(referralObj.isNull(P_TOTAL_CLICK_THROUGH) ? 0 : referralObj.getInt(P_TOTAL_CLICK_THROUGH));
								referral.setTotalInvitee(referralObj.isNull(P_TOTAL_INVITEE) ? 0 : referralObj.getInt(P_TOTAL_INVITEE));
    							
								referrals.add(referral);
							}
						}
					}
        			
					return referrals;
				}
				else {
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new IllegalStateException(MSG_INSTALL_CODE_REQUIRED);
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}

	private String getAppKey() {
		if(appKey == null) {
			appKey = loadCurrentAppKey();
		}
    	
		return appKey;
	}
    
	private String loadCurrentAppKey() {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
    	
		return prefs.getString(AGE_CURRENT_APP_KEY, null);
	}
    
	private void saveCurrentAppKey(String appKey) {
		if(appKey != null) {
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putString(AGE_CURRENT_APP_KEY, appKey);
			editor.commit();
		}
	}
    
	private String loadInstallCode() {
		String appKey = getAppKey();
    	
		if(appKey != null) {
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			
			return prefs.getString(appKey, null);
		}
    	
		return null;
	}
    
	private void saveInstallCode(String installCode) {
		if(installCode != null) {
			this.installCode = installCode;
    		
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putString(getAppKey(), installCode);
			editor.commit();
		}
	}
    
	static class AgeResponse {
		
		private int code;
		private String message;
		private JSONObject json;
		
		
		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public boolean isSuccess() {
			return code == 1000;
		}
		
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public JSONObject getJson() {
			return json;
		}

		public void setJson(JSONObject json) {
			this.json = json;
		}
		
	}
    
}