package com.hookmobile.age;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * Android to Javascript bridge.  This component enable access to 
 * AGE service from Javascript running inside WebView.  
 * 
 * @author kirktsai
 *
 */
public class WebViewBridge {
	private final static long FIRST_TIME_DISCOVERY_DELAY_MS = 3000;
	private final static String PLEASE_WAIT = "Please Wait...";
	private final static String INVITATION_SENT = "Invitation Sent!";
	
	private final static int HANDLE_SHOW_LOADING = 1;
	private final static int HANDLE_HIDE_LOADING = 2; 
	private final static int HANDLE_INVITATION_SENT = 3;
	private final static int HANDLE_SHOW_MESSAGE_DIALOG = 4;
	
	final private Activity activity;
	final private WebView webView;
	final private ProgressDialog progressDialog;
	private boolean useVirtualNumber;
	private long firstTimeDiscoveryDelayMs = FIRST_TIME_DISCOVERY_DELAY_MS;
	
	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if(msg.what == HANDLE_SHOW_LOADING) {
				progressDialog.setMessage(PLEASE_WAIT);
				progressDialog.show();
			} else if(msg.what == HANDLE_HIDE_LOADING) {
				progressDialog.cancel();
			} else if(msg.what == HANDLE_SHOW_MESSAGE_DIALOG) {
				String[] content = (String[])msg.obj;
				showErrorDialog(content[0], content[1], content[2]);
			} else if (msg.what == HANDLE_INVITATION_SENT) {
				progressDialog.setMessage(INVITATION_SENT);
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						progressDialog.cancel();
					}
				}, 750);
			}
		}

		private void showErrorDialog(String title, String message, String buttonText) {
			new AlertDialog.Builder(activity)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
	    				
	    			}
				})
				.show();
		}
	};
	
	/**
	 * Constructor to create helper class that send invitation via virtual number.
	 * @param activity
	 * @param appKey
	 */
	public WebViewBridge(Activity activity, WebView webView, String appKey) {
		this.activity = activity;
		this.webView = webView;
		this.useVirtualNumber = true;
		Discoverer.activate(activity.getBaseContext(), appKey);
		
		progressDialog = new ProgressDialog(this.activity);
		progressDialog.setMessage(PLEASE_WAIT);
		progressDialog.setCancelable(false);
	}
	
	/**
	 * Constructor to create helper class
	 * @param activity
	 * @param appKey
	 * @param useVirtualNumber
	 */
	public WebViewBridge(Activity activity, WebView webView, String appKey, boolean useVirtualNumber) {
		this(activity, webView, appKey);
		this.useVirtualNumber = useVirtualNumber;
	}

	/**
	 * Sets virtual number used for sending invitation.
	 * @param useVirtualNumber
	 */
	public void setUseVirtualNumber(boolean useVirtualNumber) {
		this.useVirtualNumber = useVirtualNumber;
	}


	/**
	 * initiate address book discovery process.
	 */
	@JavascriptInterface
	public void discover() {

		try {
			messageHandler.sendEmptyMessage(HANDLE_SHOW_LOADING);
			long delayToQueryLead = Discoverer.getInstance().hasPreviouslyDiscovered() ? 500 : firstTimeDiscoveryDelayMs;
			
			Discoverer.getInstance().discover();
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					List<Lead> listLeads;
					try {
						listLeads = Discoverer.getInstance().queryLeads();
						if(listLeads == null || listLeads.size() == 0)
							return;
						
						StringBuilder sb = new StringBuilder();
						sb.append("{\"leads\":[");
						
						for(Iterator<Lead> i = listLeads.iterator(); i.hasNext(); ) {
							Lead lead = i.next();
							sb.append("{\"phone\":\"");
							sb.append(lead.getPhone());
							sb.append("\",\"name\":\"");
							sb.append(com.hookmobile.age.utils.AgeUtils.lookupNameByPhone(activity, lead.getPhone()));
							
							if(i.hasNext())
								sb.append("\"},");
							else
								sb.append("\"}]}");
						}

						webView.loadUrl("javascript:displayLeads('" + sb.toString() + "')");
					} catch (AgeException e) {
						 displayError(e);
					} finally {
						messageHandler.sendEmptyMessage(HANDLE_HIDE_LOADING);
					}
				}
			}, delayToQueryLead);
		} catch (AgeException e) {
			displayError(e);
			messageHandler.sendEmptyMessage(HANDLE_HIDE_LOADING);
		}
	}
	
	/**
	 * Create invitation to be sent to given list of phone numbers.
	 * @param phones
	 * @return
	 */
	@JavascriptInterface
	public int newReferral(String[] phones){
		messageHandler.sendEmptyMessage(HANDLE_SHOW_LOADING);
		List<String> listPhones = new ArrayList<String>();
		for(int i = 0; i < phones.length; ++i)
			listPhones.add(phones[i]);
		try{
			Discoverer.getInstance().newReferral(listPhones, useVirtualNumber, null);
			messageHandler.sendEmptyMessage(HANDLE_INVITATION_SENT);
			return phones.length;
		} catch (AgeException e) {
			messageHandler.sendEmptyMessage(HANDLE_HIDE_LOADING);
			 displayError(e);
			 return -1;
		} finally {
		}
	}
	
	//Backward
	public void back() {
		activity.setResult(Activity.RESULT_OK, activity.getIntent());
		activity.finish();
	}

	//Display message.
	private void showMessage(String[] content) {
		Message msg = messageHandler.obtainMessage();
		msg.what = HANDLE_SHOW_MESSAGE_DIALOG;
		msg.obj = content;
		messageHandler.sendMessage(msg);
	}

	private void displayError(AgeException e) {
		String body = "Hook Mobile server encountered a problem: ";

		if (e.getMessage() != null) {
			body += e.getMessage();
		} else {
			body += "Unknown Error";
		}

		showMessage(new String[] { "Finished", body, "Dismiss" });
	}

	public void setFirstTimeDiscoveryDelayMs(long firstTimeDiscoveryDelayMs) {
		this.firstTimeDiscoveryDelayMs = firstTimeDiscoveryDelayMs;
	}


}
