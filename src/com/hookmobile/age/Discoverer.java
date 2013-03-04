package com.hookmobile.age;

import static com.hookmobile.age.AgeClient.doPost;
import static com.hookmobile.age.AgeConstants.AGE_CURRENT_APP_KEY;
import static com.hookmobile.age.AgeConstants.AGE_INSTALL_TOKEN;
import static com.hookmobile.age.AgeConstants.AGE_LOG;
import static com.hookmobile.age.AgeConstants.AGE_PREFERENCES;
import static com.hookmobile.age.AgeConstants.AGE_QUEUED_TRACKING_EVENTS;
import static com.hookmobile.age.AgeException.E_ADDRESSBOOK_EXPIRED;
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
import static com.hookmobile.age.AgeConstants.P_CUSTOM_PARAM;
import static com.hookmobile.age.AgeConstants.P_TAPJOY_UDID;
import static com.hookmobile.age.AgeConstants.P_TOTAL_CLICK_THROUGH;
import static com.hookmobile.age.AgeConstants.P_TOTAL_INVITEE;
import static com.hookmobile.age.AgeConstants.P_USE_VIRTUAL_NUMBER;
import static com.hookmobile.age.AgeConstants.P_VERIFIED;
import static com.hookmobile.age.AgeConstants.P_VERIFY_MESSAGE;
import static com.hookmobile.age.AgeConstants.P_VERIFY_MT;
import static com.hookmobile.age.AgeConstants.P_EVENT_NAME;
import static com.hookmobile.age.AgeConstants.P_EVENT_VALUE;
import static com.hookmobile.age.utils.AgeUtils.getAddressHash;
import static com.hookmobile.age.utils.AgeUtils.getAddressbook;
import static com.hookmobile.age.utils.AgeUtils.getDeviceInfo;
import static com.hookmobile.age.utils.AgeUtils.getPhoneCount;
import static com.hookmobile.age.utils.AgeUtils.isOnline;
import static com.hookmobile.age.utils.AgeUtils.isSmsSupported;
import static com.hookmobile.age.utils.AgeUtils.loadLastPhoneCount;
import static com.hookmobile.age.utils.AgeUtils.queryDevicePhone;
import static com.hookmobile.age.utils.AgeUtils.saveLastPhoneCount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.hookmobile.age.AgeClient.AgeResponse;
import com.hookmobile.age.utils.TapjoyManager;

/**
 * This is the central class that provides the AGE services:
 * device verification, smart invitation, referrals tracking, and installs query.  You must first call
 * {@link #activate(Context, String)} to validate your assigned appKey when your app is initialized.  
 * Then you can get the default instance of Discoverer by invoking {@link #getInstance()} method.  With
 * that, you can start generating referral/invitation.
 * <p>
 * Before sending out any referral, you must first issue {@link #discover()} to scan
 * device address book for contacts having supported mobile devices.  Within seconds, you may issue
 *{@link #queryLeads()} to retrieve list of suggested contacts for referral.  App user will
 * select one or more of these contacts for {@link #newReferral(List, boolean, String)}.
 * Once referral is sent, you may issue {@link Discoverer#queryReferral(long)} for
 * status of referral.
 * 
 * You can also refer to Hook Mobile's <a href="http://hookmobile.com/android-tutorial.html" target="_blank">Android Tutorial</a> for more information.
 */
public class Discoverer {
	public static final int MAX_UPLOAD_SIZE = 2000; 
	public static final int FIRST_UPLOAD_SIZE = 200; 

	private static final String AGE_SDK_VERSION = "android/1.1.3";
	private static final String DEFAULT_REFERRAL = "This is a cool app: %app%, check it out here %link%";
	
	private static String server = "https://age.hookmobile.com";
	private static String virtualNumber = "+13025175040";
	
	private static Discoverer instance;
	private static Context context;
	private static boolean passContactName = true;
	
	private volatile long newInstallInvokeTime;
	
	private String appKey;
	private String devicePhone;
	private String installCode;
	private String installToken;
	
	private List<Lead> cachedLeads;
	private List<String> cachedInstalls;
	private List<Referral> cachedReferrals;
	private JSONObject queuedTrackingEvents;
	
	/**
	 * Activates the AGE service. This method must be invoked when the app is launched.
	 * This method should only be invoked once.  
	 * @param context the Android context. 
	 * @param appKey the app key you register on Hook Mobile developers portal.
	 */
	public static void activate(Context context, String appKey) {
		Discoverer.context = context.getApplicationContext();
		Discoverer.instance = new Discoverer(appKey);
		Discoverer.instance.newInstall();
		Discoverer.instance.sendQueuedEvents();
	}
    
	/**
	 * Activates the AGE service. This method must be invoked when the app is launched.
	 * This method should only be invoked once.  
	 * @param context the Android context. 
	 * @param appKey the app key you register on Hook Mobile developers portal.
	 * @param customParam for storing custom parameter such as app assigned user_id.
	 */
	public static void activate(Context context, String appKey, String customParam) {
		Discoverer.context = context.getApplicationContext();
		Discoverer.instance = new Discoverer(appKey);
		Discoverer.instance.newInstall(customParam);
		Discoverer.instance.sendQueuedEvents();
	}
  
	/**
	 * Gets the Discoverer singleton instance.  Must be preceded with call to
	 * {@link #activate(Context, String)} to validate your assigned appKey.
	 * @throws IllegalStateException activate not called prior to this method.
	 */
	public static Discoverer getInstance() {
		if(instance != null) {
			return instance;
		}
		
		throw new IllegalStateException("Please activate first.");
	}
    
	/**
	 * Submits a discovery request. User's address book will be securely uploaded to AGE server for the analysis.
	 * It takes AGE seconds to identify device OS for each uploaded contacts.  First 
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
	
	/**
	 * Gets a list of recommended invites from AGE. The result is optimized and filtered by the device types
	 * specified in your app profile on Hook Mobile developers portal.
	 * 
	 * @return the recommended invitation leads.  The size of leads is limited to 20.
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
								Lead lead = new Lead(leadObj.isNull(P_PHONE) ? "Unknown" : leadObj.getString(P_PHONE), 
										leadObj.isNull(P_OS_TYPE) ? "Unknown" : leadObj.getString(P_OS_TYPE));
    							
								leads.add(lead);
							}
						}
					}
					this.cachedLeads = leads;
        			
					return leads;
				}
				else {
					if(response.getCode() == E_ADDRESSBOOK_EXPIRED) {
						Log.i(AGE_LOG, "Rediscovery required");
						
						saveLastPhoneCount(context, 0);
					}
					
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new AgeException(AgeException.E_NOT_YET_DISCOVERED, "Invalid State. Must invoke discover() first");
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
				throw new AgeException(AgeException.E_NOT_YET_DISCOVERED, "Invalid State. Must invoke discover() first");
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
	public Referral queryReferral(long referralId) throws AgeException {
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
					if(response.getCode() == E_ADDRESSBOOK_EXPIRED) {
						Log.i(AGE_LOG, "Rediscovery required");
						
						saveLastPhoneCount(context, 0);
					}
					
					throw new AgeException(response.getCode(), response.getMessage());
				}
			}
			else {
				throw new AgeException(AgeException.E_NOT_YET_DISCOVERED, "Invalid State. Must invoke discover() first");
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
	 * Gets the Hook Moible virtual number used for referral.
	 * 
	 * @return the virtual number.
	 */
	public static String getVirtualNumber() {
		return virtualNumber;
	}

	/**
	 * Control whether or not name of contact is to be passed to server.
	 * Default value is TRUE.
	 * 
	 * @param passContactName
	 */
	public static void setPassContactName(boolean passContactName) {
		Discoverer.passContactName = passContactName;
	}

	/**
	 * Send out any previously queued events to server.
	 */
	public void sendQueuedEvents() {
		trackEvent(null, null);
	}

	/**
	 * Track user engagement with user defined milestones.  
	 * @param eventName event name to be tracked.
	 * @param eventValue event value to be tracked.
	 */
	public void trackEvent(final String eventName, final String eventValue) {
		final String installCode = this.getInstallCode();
		
		if (installCode == null)
			return;

		Thread a = new Thread() {
			@Override
			public void run() {
				JSONObject queuedTrackingEvents = loadQueuedTrackingEvents();
				try {
					if(installCode != null) {
						if (eventName != null && eventName.length() > 0)
							queuedTrackingEvents.put(eventName, eventValue);
						
						if (isOnline(context)) {
							for (@SuppressWarnings("unchecked")
							Iterator<String> itr=queuedTrackingEvents.keys(); itr.hasNext();) {
								String key = itr.next();
								String value = queuedTrackingEvents.getString(key);
								String url = server + "/trackevent";
								List<NameValuePair> form = new ArrayList<NameValuePair>();
								form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
								form.add(new BasicNameValuePair(P_INSTALL_CODE, installCode));
								form.add(new BasicNameValuePair(P_EVENT_NAME, key));
								form.add(new BasicNameValuePair(P_EVENT_VALUE, value));
								form.add(new BasicNameValuePair(P_SDK_VERSION, AGE_SDK_VERSION));
								
								AgeResponse response = doPost(url, form);
								Log.w(AGE_LOG, response.getMessage());
							}
							queuedTrackingEvents = null;
						}
					}
				} catch(Throwable t) {
					Log.i(AGE_LOG, t.getMessage());
				} finally {
					saveQueuedTrackingEvents(queuedTrackingEvents);
				}
			}
		};
		a.start();
		a = null;
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
	 * Gets the last cached queried leads.  It may be empty.
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
	 * Gets the last cached queried installs. It may be empty.
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
	 * Gets the last queried referrals. It may be empty.
	 * 
	 * @return the cached referrals.
	 */
	public List<Referral> getCachedReferrals() {
		if(cachedReferrals != null) {
			return cachedReferrals;
		}
		
		return Collections.<Referral>emptyList();
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

	private void newInstall() {
		newInstall(null);
	}
	
	private void newInstall(final String customParam) {
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
						if (customParam != null)
							form.add(new BasicNameValuePair(P_CUSTOM_PARAM, customParam));
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
    

	private boolean discoverSync() throws Exception {
		waitForNewInstall();
		
		int phoneCount = getPhoneCount(context);
		int lastCount = loadLastPhoneCount(context);
		
		if(phoneCount != lastCount) {
			String installCode = getInstallCode();
			String url = server + "/discover";
			JSONArray addressBook = null;
			if(hasPreviouslyDiscovered()) {
				addressBook = getAddressbook(context, MAX_UPLOAD_SIZE, true);
				
				Log.i(AGE_LOG, "Phone Count: "+ phoneCount + ", Processed Count: " + addressBook.length());
			}
			else {
				addressBook = getAddressbook(context, FIRST_UPLOAD_SIZE, passContactName);
				
				if(phoneCount > FIRST_UPLOAD_SIZE) {
					phoneCount = FIRST_UPLOAD_SIZE;
				}
				
				Log.i(AGE_LOG, "Phone Count: "+ phoneCount + ", Processed Count: " + addressBook.length());
			}
			
			List<NameValuePair> form = new ArrayList<NameValuePair>();
			form.add(new BasicNameValuePair(P_INSTALL_TOKEN, getInstallToken()));
			form.add(new BasicNameValuePair(P_APP_KEY, getAppKey()));
			form.add(new BasicNameValuePair(P_PHONE, getDevicePhone()));
			form.add(new BasicNameValuePair(P_TAPJOY_UDID, TapjoyManager.getTapjoyUDID()));
			form.add(new BasicNameValuePair(P_MAC_ADDRESS, TapjoyManager.getMacAddress()));
			form.add(new BasicNameValuePair(P_SDK_VERSION, AGE_SDK_VERSION));
			form.add(new BasicNameValuePair(P_ADDRESSBOOK, addressBook.toString()));
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
	



	private List<Referral> queryReferrals(long referralId) throws AgeException {
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
								Referral referral = 
									new Referral(referralObj.isNull(P_REFERRAL_ID) ? -1 : referralObj.getLong(P_REFERRAL_ID),
												 referralObj.isNull(P_DATE) ? "" : referralObj.getString(P_DATE),
											 	 referralObj.isNull(P_TOTAL_INVITEE) ? 0 : referralObj.getInt(P_TOTAL_INVITEE),
												 referralObj.isNull(P_TOTAL_CLICK_THROUGH) ? 0 : referralObj.getInt(P_TOTAL_CLICK_THROUGH));
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
				throw new AgeException(AgeException.E_NOT_YET_DISCOVERED, "Invalid State. Must invoke discover() first");
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
	
	boolean hasPreviouslyDiscovered() {
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
	
	private JSONObject loadQueuedTrackingEvents() {
		if (this.queuedTrackingEvents == null) {
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			String jsonString = prefs.getString(AGE_QUEUED_TRACKING_EVENTS, null);
	    	if (jsonString != null) {
	    		try {
	    			this.queuedTrackingEvents = new JSONObject(jsonString);
				} catch (JSONException e) {
					this.queuedTrackingEvents = new JSONObject();
				}
	    	} else {
	    		this.queuedTrackingEvents = new JSONObject();
	    	}
		}
		return this.queuedTrackingEvents;
	}
    
	private void saveQueuedTrackingEvents(JSONObject queuedTrackingEvents) {
		this.queuedTrackingEvents = queuedTrackingEvents;
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(AGE_QUEUED_TRACKING_EVENTS, queuedTrackingEvents != null ? queuedTrackingEvents.toString() : null);
		editor.commit();
	}
	
}