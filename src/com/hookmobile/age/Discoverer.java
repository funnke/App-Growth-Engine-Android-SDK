package com.hookmobile.age;

import static com.hookmobile.age.AgeClient.doPost;
import static com.hookmobile.age.AgeConstants.AGE_CURRENT_APP_KEY;
import static com.hookmobile.age.AgeConstants.AGE_INSTALL_TOKEN;
import static com.hookmobile.age.AgeConstants.AGE_LOG;
import static com.hookmobile.age.AgeConstants.AGE_PREFERENCES;
import static com.hookmobile.age.AgeConstants.E_DISCOVERY_EXPIRED;
import static com.hookmobile.age.AgeConstants.P_ADDRESSBOOK;
import static com.hookmobile.age.AgeConstants.P_ADDRESS_HASH;
import static com.hookmobile.age.AgeConstants.P_APP_KEY;
import static com.hookmobile.age.AgeConstants.P_DATE;
import static com.hookmobile.age.AgeConstants.P_DEVICE_INFO;
import static com.hookmobile.age.AgeConstants.P_INSTALL_CODE;
import static com.hookmobile.age.AgeConstants.P_INSTALL_TOKEN;
import static com.hookmobile.age.AgeConstants.P_LEADS;
import static com.hookmobile.age.AgeConstants.P_MAC_ADDRESS;
import static com.hookmobile.age.AgeConstants.P_NAME;
import static com.hookmobile.age.AgeConstants.P_OS_TYPE;
import static com.hookmobile.age.AgeConstants.P_PHONE;
import static com.hookmobile.age.AgeConstants.P_REFERENCE;
import static com.hookmobile.age.AgeConstants.P_REFERRALS;
import static com.hookmobile.age.AgeConstants.P_REFERRAL_ID;
import static com.hookmobile.age.AgeConstants.P_INSTALL_REFERRER;
import static com.hookmobile.age.AgeConstants.P_REFERRAL_MESSAGE;
import static com.hookmobile.age.AgeConstants.P_SDK_VERSION;
import static com.hookmobile.age.AgeConstants.P_SEND_NOW;
import static com.hookmobile.age.AgeConstants.P_TAPJOY_UDID;
import static com.hookmobile.age.AgeConstants.P_TOTAL_CLICK_THROUGH;
import static com.hookmobile.age.AgeConstants.P_TOTAL_INVITEE;
import static com.hookmobile.age.AgeConstants.P_USE_VIRTUAL_NUMBER;
import static com.hookmobile.age.AgeConstants.P_VERIFIED;
import static com.hookmobile.age.AgeConstants.P_VERIFY_MESSAGE;
import static com.hookmobile.age.AgeConstants.P_VERIFY_MT;
import static com.hookmobile.age.AgeUtils.getAddressHash;
import static com.hookmobile.age.AgeUtils.getAddressbook;
import static com.hookmobile.age.AgeUtils.getDeviceInfo;
import static com.hookmobile.age.AgeUtils.getPhoneCount;
import static com.hookmobile.age.AgeUtils.isOnline;
import static com.hookmobile.age.AgeUtils.isSmsSupported;
import static com.hookmobile.age.AgeUtils.loadLastPhoneCount;
import static com.hookmobile.age.AgeUtils.queryDevicePhone;
import static com.hookmobile.age.AgeUtils.saveLastPhoneCount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.hookmobile.age.AgeClient.AgeResponse;

/**
 * This is the central class that provides the AGE services.
 * Device verification, smart invitation, referrals tracking, and installs query.
 * You can also refer to Hook Mobile's <a href="http://hookmobile.com/android-tutorial.html" target="_blank">Android Tutorial</a> for more information.
 */
public class Discoverer {
	
	private static final String AGE_SDK_VERSION = "android/1.0.3";
	private static final String DEFAULT_REFERRAL = "This is a cool app: %app%, check it out here %link%";
	private static final String INSTALL_CODE_REQUIRED = "Install code not found! Please call discover first.";
	
	private static final int FIRST_UPLOAD_SIZE = 200; 

	private static String server = "https://age.hookmobile.com";
	private static String virtualNumber = "+13025175040";
	
	private static Discoverer instance;
	private static Context context;
	
	private volatile long newInstallInvokeTime;
	
	private String appKey;
	private String devicePhone;
	private String installCode;
	private String installToken;
	
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
		Discoverer.instance.newInstall();
	}
    
	
	public static void referUA(String referUAValue){
		
		GoogleAnalyticsTracker tracker;
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.startNewSession(referUAValue, context);
		tracker.setCustomVar(1, "Medium", "Mobile App");
		tracker.trackPageView("/main");
		Log.v("ReferralReceiver", "Dispacthing and closing");
		tracker.dispatch();
		tracker.stopSession();
		
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
		this.installToken = getInstallToken();
    	
		saveCurrentAppKey(context, appKey);
    	
		Log.i(AGE_LOG, "AGE SDK Version: "+ AGE_SDK_VERSION);
		Log.i(AGE_LOG, "devicePhone: "+ devicePhone);
		Log.i(AGE_LOG, "installCode: "+ installCode);
		Log.i(AGE_LOG, "installToken: "+ installToken);
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
	 * Gets the unique code associated with this install.
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
	
	private void newInstall() {
		TapjoyManager.init(context);
		newInstallInvokeTime = System.currentTimeMillis();
		
		Thread a = new Thread() {
			@Override
			public void run() {
				try {
					String installCode = getInstallCode();
					
					Log.i(AGE_LOG, getAddressHash(context));
					
					if(installCode == null && isOnline(context)) {
						String url = server + "/newinstall";
						List<NameValuePair> form = new ArrayList<NameValuePair>();
						form.add(new BasicNameValuePair(P_INSTALL_TOKEN, getInstallToken()));
						form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
						form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
						form.add(new BasicNameValuePair(P_TAPJOY_UDID, TapjoyManager.getTapjoyUDID()));
						form.add(new BasicNameValuePair(P_MAC_ADDRESS, TapjoyManager.getMacAddress()));
						form.add(new BasicNameValuePair(P_ADDRESS_HASH, getAddressHash(context)));
						form.add(new BasicNameValuePair(P_SDK_VERSION, AGE_SDK_VERSION));
						form.add(new BasicNameValuePair(P_DEVICE_INFO, getDeviceInfo(context)));
						String installReferrer = AgeHelper.retrieveInstallReferrer(context);
						if (installReferrer != null)
							form.add(new BasicNameValuePair(P_INSTALL_REFERRER, installReferrer));

						AgeResponse response = doPost(url, form);
						
						if(response.isSuccess()) {
							JSONObject json = response.getJson();
							installCode = json.isNull(P_INSTALL_CODE) ? null : json.getString(P_INSTALL_CODE);
							
							saveInstallCode(installCode);
						}
						else {
							Log.w(AGE_LOG, response.getMessage());
						}
					}
				}
				catch(Throwable t) {
					Log.i(AGE_LOG, t.getMessage());
				}
				
				newInstallInvokeTime = 0;
			}
		};
		a.start();
		a = null;
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
			waitForNewInstall();
			
			String installCode = getInstallCode();
			String url = server + "/verifydevice";
			List<NameValuePair> form = new ArrayList<NameValuePair>();
			form.add(new BasicNameValuePair(P_INSTALL_TOKEN, getInstallToken()));
			form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
			form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
			form.add(new BasicNameValuePair(P_TAPJOY_UDID, TapjoyManager.getTapjoyUDID()));
			form.add(new BasicNameValuePair(P_MAC_ADDRESS, TapjoyManager.getMacAddress()));
			form.add(new BasicNameValuePair(P_ADDRESS_HASH, getAddressHash(context)));
			form.add(new BasicNameValuePair(P_SDK_VERSION, AGE_SDK_VERSION));
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
	 * It takes AGE up to a couple of minutes to perform device detection and data mining.
	 * 
	 * @throws AgeException if the AGE request failed.
	 */
	public void discover() throws AgeException {
		try {
			if(discoverSync()) {
				discoverAsync();
			}
		}
		catch(AgeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new AgeException(e);
		}
	}
	
	private boolean discoverSync() throws Exception {
		waitForNewInstall();
		
		int phoneCount = getPhoneCount(context);
		int lastCount = loadLastPhoneCount(context);
		
		if(phoneCount != lastCount) {
			String installCode = getInstallCode();
			String url = server + "/discover";
			String addressBook = null;
			if(isDiscovered()) {
				addressBook = getAddressbook(context, Integer.MAX_VALUE, true);
				
				Log.i(AGE_LOG, "Phone Count: "+ phoneCount);
			}
			else {
				addressBook = getAddressbook(context, FIRST_UPLOAD_SIZE);
				
				if(phoneCount > FIRST_UPLOAD_SIZE) {
					phoneCount = FIRST_UPLOAD_SIZE;
				}
				
				Log.i(AGE_LOG, "Phone Count: "+ phoneCount);
			}
			
			List<NameValuePair> form = new ArrayList<NameValuePair>();
			form.add(new BasicNameValuePair(P_INSTALL_TOKEN, getInstallToken()));
			form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
			form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
			form.add(new BasicNameValuePair(P_TAPJOY_UDID, TapjoyManager.getTapjoyUDID()));
			form.add(new BasicNameValuePair(P_MAC_ADDRESS, TapjoyManager.getMacAddress()));
			form.add(new BasicNameValuePair(P_SDK_VERSION, AGE_SDK_VERSION));
			form.add(new BasicNameValuePair(P_ADDRESSBOOK, addressBook));
			form.add(new BasicNameValuePair(P_DEVICE_INFO, getDeviceInfo(context)));
			if(installCode != null) {
				form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
			}
		    
			AgeResponse response = doPost(url, form);
    		
			if(response.isSuccess()) {
				JSONObject json = response.getJson();
				installCode = json.isNull(P_INSTALL_CODE) ? null : json.getString(P_INSTALL_CODE);
				
				saveInstallCode(installCode);
				saveLastPhoneCount(context, phoneCount);
				
				return true;
			}
			else {
				throw new AgeException(response.getCode(), response.getMessage());
			}
		}
		
		return false;
	}
	
	private void discoverAsync() {
		Thread a = new Thread() {
			@Override
			public void run() {
				try {
					discoverSync();
				}
				catch(Throwable t) {
					Log.i(AGE_LOG, t.getMessage());
				}
			}
		};
		a.start();
		a = null;
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
					if(response.getCode() == E_DISCOVERY_EXPIRED) {
						Log.i(AGE_LOG, "Rediscovery required");
						
						saveLastPhoneCount(context, 0);
					}
					
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new IllegalStateException(INSTALL_CODE_REQUIRED);
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
					if(response.getCode() == E_DISCOVERY_EXPIRED) {
						Log.i(AGE_LOG, "Rediscovery required");
						
						saveLastPhoneCount(context, 0);
					}
					
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new IllegalStateException(INSTALL_CODE_REQUIRED);
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
	 * @param name the name of the app user or invitation sender
	 * @return the referral ID, which can be used to track the referral status later.
	 * @throws AgeException if the AGE request failed.
	 */
	public long newReferral(List<String> phones, boolean useVirtualNumber, String name) throws AgeException {
		try {
			String installCode = getInstallCode();
			if(! useVirtualNumber && ! isSmsSupported(context)) {
				Log.d(AGE_LOG, "SMS not supported, use virtual number instead.");
    			
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
        		
				AgeResponse response = doPost(url, form);
        		
				if(response.isSuccess()) {
					JSONObject json = response.getJson();
					long referralId = json.isNull(P_REFERRAL_ID) ? -1 : json.getLong(P_REFERRAL_ID);
					String referralMessage = json.isNull(P_REFERRAL_MESSAGE) ? DEFAULT_REFERRAL : json.getString(P_REFERRAL_MESSAGE);
        			
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
				throw new IllegalStateException(INSTALL_CODE_REQUIRED);
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
				throw new IllegalStateException(INSTALL_CODE_REQUIRED);
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
			appKey = loadCurrentAppKey(context);
		}
    	
		return appKey;
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
	
	private String loadCurrentAppKey(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
    	
		return prefs.getString(AGE_CURRENT_APP_KEY, null);
	}
    
	private void saveCurrentAppKey(Context context, String appKey) {
		if(appKey != null) {
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putString(AGE_CURRENT_APP_KEY, appKey);
			editor.commit();
		}
	}
	
	private boolean isDiscovered() {
		int count = loadLastPhoneCount(context);
		
		if(count > 0) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private synchronized String getInstallToken() {
		if(installToken == null) {
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			installToken = prefs.getString(AGE_INSTALL_TOKEN, null);
			
			if(installToken == null) {
				installToken = UUID.randomUUID().toString().toLowerCase();
				
				Editor editor = prefs.edit();
				editor.putString(AGE_INSTALL_TOKEN, installToken);
				editor.commit();
			}
		}
		
		return installToken;
	}

	private void waitForNewInstall() {
		if(newInstallInvokeTime > 0) {
			long diff = System.currentTimeMillis() - newInstallInvokeTime;
			
			if(diff < 3000) {
				long waitTime = 3000 - diff;
				
				try {
					Log.d(AGE_LOG, "Pause: "+ waitTime +" ms");
					
					Thread.sleep(waitTime);
				}
				catch(Exception e) {
					//Ignore
				}
			}
		}
	}
	
}