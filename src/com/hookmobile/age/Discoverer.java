package com.hookmobile.age;

import static com.hookmobile.age.AgeUtils.doPost;
import static com.hookmobile.age.AgeUtils.getAddressbook;
import static com.hookmobile.age.AgeUtils.getDeviceOsInfo;
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

public class Discoverer {
	
	private static final String AGE_PREFERENCES				= "age_preferences";
	private static final String AGE_CURRENT_APP_KEY			= "current_app_key";
	private static final String AGE_TAG_HOOK				= "Hook";
	private static final String AGE_DEFAULT_REFERRAL_MSG	= "I thought you might be interested in this app %app%, check it out here %link%";
	
	private static final String MSG_INSTALL_CODE_REQUIRED	= "Install code not found! Please call discover first.";
	
	private static final String P_ADDRESSBOOK				= "addressBook";
	private static final String P_APP_KEY					= "appKey";
	private static final String P_DATE						= "date";
	private static final String P_DEVICE_INFO				= "deviceInfo";
	private static final String P_INSTALL_CODE				= "installCode";
	private static final String P_LEADS						= "leads";
	private static final String P_NAME						= "name";
	private static final String P_OS_TYPE					= "osType";
	private static final String P_PHONE						= "phone";
	private static final String P_REFERRAL_ID				= "referralId";
	private static final String P_REFERRAL_MESSAGE			= "referralMessage";
	private static final String P_REFERRAL_TEMPLATE			= "referralTemplate";
	private static final String P_REFERRALS					= "referrals";
	private static final String P_REFERENCE					= "reference";
	private static final String P_SEND_NOW					= "sendNow";
	private static final String P_TOTAL_CLICK_THROUGH		= "totalClickThrough";
	private static final String P_TOTAL_INVITEE				= "totalInvitee";
	private static final String P_USE_VIRTUAL_NUMBER		= "useVirtualNumber";
	private static final String P_VERIFIED					= "verified";
	private static final String P_VERIFY_MESSAGE			= "verifyMessage";
	private static final String P_VERIFY_MT					= "verifyMt";
	
	private static Discoverer instance;
	private static Context context;
	
	private static String server = "https://age.hookmobile.com";
	private static String smsDest = "+13025175040";
	
	private String appKey;
	private String devicePhone;
    private String installCode;
   
    private long lastReferralId;
    private List<Lead> cachedLeads;
    private List<String> cachedInstalls;
    private List<Referral> cachedReferrals;
    
    
    public static void activate(Context context, String appKey) {
    	Discoverer.context = context.getApplicationContext();
    	Discoverer.instance = new Discoverer(appKey);
    }
    
    public static Discoverer getInstance() {
    	if(instance != null) {
    		return instance;
    	}
    	
    	throw new IllegalStateException("Please activate first.");
    }
    
    public static void setSmsDest(String smsDest) {
    	Discoverer.smsDest = smsDest;
    }
    
    public static String getSmsDest() {
    	return smsDest;
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
    
    public String getAppKey() {
    	if(appKey == null) {
    		appKey = loadCurrentAppKey();
    	}
    	
    	return appKey;
    }
    
    public String getDevicePhone() {
    	if(devicePhone == null) {
    		devicePhone = queryDevicePhone(context);
    	}
    	
		return devicePhone;
	}

	public String getInstallCode() {
		if(installCode == null) {
			installCode = loadInstallCode();
		}
		
		return installCode;
	}

	public boolean isRegistered() {
		String installCode = getInstallCode();
		
    	if(isEmptyStr(installCode)) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
	public long getLastReferralId() {
		if(lastReferralId > 0) {
			return lastReferralId;
		}
		
		return -1;
	}
	
	public List<Lead> getCachedLeads() {
		if(cachedLeads != null) {
			return cachedLeads;
		}
		
		return Collections.<Lead>emptyList();
	}

	public List<String> getCachedInstalls() {
		if(cachedInstalls != null) {
			return cachedInstalls;
		}
		
		return Collections.<String>emptyList();
	}

	public List<Referral> getCachedReferrals() {
		if(cachedReferrals != null) {
			return cachedReferrals;
		}
		
		return Collections.<Referral>emptyList();
	}

	public String verifyDevice(boolean useMtVerification, String name) throws AgeException {
    	try {
    		String installCode = getInstallCode();
        	String url = server + "/newverify";
    		List<NameValuePair> form = new ArrayList<NameValuePair>();
    	    form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
    	    form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
    	    form.add(new BasicNameValuePair(P_VERIFY_MT, String.valueOf(useMtVerification)));
    	    form.add(new BasicNameValuePair(P_DEVICE_INFO, getDeviceOsInfo(context)));
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
    
    public boolean queryVerifiedStatus() throws AgeException {
    	try {
    		String installCode = getInstallCode();
        	
        	if(installCode != null) {
        		String url = server + "/queryverify";
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
    
    public void discover() throws AgeException {
		try {
			String installCode = getInstallCode();
	    	String url = server + "/newleads";
			List<NameValuePair> form = new ArrayList<NameValuePair>();
		    form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
		    form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
		    form.add(new BasicNameValuePair(P_ADDRESSBOOK, getAddressbook(context)));
		    form.add(new BasicNameValuePair(P_DEVICE_INFO, getDeviceOsInfo(context)));
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
    
    public List<String> queryInstalls(String direction) throws AgeException {
    	try {
    		String installCode = getInstallCode();
        	
        	if(installCode != null) {
        		List<String> installs = new ArrayList<String>();
            	String url = server + "/queryinstalls";
            	List<NameValuePair> form = new ArrayList<NameValuePair>();
        	    form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
        	    form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
        	    form.add(new BasicNameValuePair(P_REFERENCE, direction));
        	    
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
    
    public long newReferral(List<String> phones, boolean useVirtualNumber, String name) throws AgeException {
    	return newReferral(phones, useVirtualNumber, name, null);
    }

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
        			lastReferralId = json.isNull(P_REFERRAL_ID) ? -1 : json.getLong(P_REFERRAL_ID);
        			String referralMessage = json.isNull(P_REFERRAL_MESSAGE) ? AGE_DEFAULT_REFERRAL_MSG : json.getString(P_REFERRAL_MESSAGE);
        			
        			Log.d(AGE_TAG_HOOK, "lastReferralId: "+ lastReferralId);
        			
        			if(! useVirtualNumber) {
        				SmsManager sms = SmsManager.getDefault();
        				
        				for(String phone : phones) {
        					sms.sendTextMessage(phone, null, referralMessage, null, null);
        				}
        			}
    				
    				return lastReferralId;
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
    
    public Referral queryReferral(int referralId) throws AgeException {
    	if(referralId > 0) {
    		List<Referral> referrals = queryReferrals(referralId);
        	
        	if(referrals.size() > 0) {
        		return referrals.get(0);
        	}
    	}
    	
    	return null;
    }
    
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
    
    public static class Directions {
    	public final static String FORWARD		= "FORWARD";
    	public final static String BACKWARD		= "BACKWARD";
    	public final static String MUTUAL		= "MUTUAL";
    }
    
	public static class AgeResponse {
		
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