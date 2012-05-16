package com.hookmobile.age.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hookmobile.age.AgeException;
import com.hookmobile.age.Discoverer;
import com.hookmobile.age.Discoverer.Directions;
import com.hookmobile.age.R;

public class HookMobileSample extends Activity implements OnClickListener {
	
	private String appKey = "Your-App-Key";
	
    private static int HANDLE_SHOW_LOADING								= 1;
    private static int HANDLE_HIDE_LOADING								= 2; 
    private static int HANDLE_VERIFICATION_STATUS_ENABLE				= 3;
    private static int HANDLE_SHOW_MESSAGE_DIALOG						= 4;
    private static int HANDLE_GET_RECOMMENDED_INVITES_BUTTON_ENABLE		= 5;
    private static int HANDLE_INSTALLS_REFERRALS_ENABLE					= 6;
    
    private Button verifyDeviceButton;
    private Button verificationStatusButton;
    private Button discoverContactsButton;
    private Button recommendInvitesButton;
    private Button installsButton;
    private Button referralsButton;
    private ProgressDialog progressDialog;
    
    private Handler handler = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		if(msg.what == HANDLE_SHOW_LOADING) {
    			progressDialog.show();
    		}
    		else if(msg.what == HANDLE_HIDE_LOADING) {
    			progressDialog.cancel();
    		}
    		else if(msg.what == HANDLE_VERIFICATION_STATUS_ENABLE) {
    			verificationStatusButton.setEnabled(true);
    		}
    		else if(msg.what == HANDLE_SHOW_MESSAGE_DIALOG) {
    			String[] content = (String[])msg.obj;
    			showErrorDialog(content[0], content[1], content[2]);
    		}
    		else if(msg.what == HANDLE_GET_RECOMMENDED_INVITES_BUTTON_ENABLE) {
    			recommendInvitesButton.setEnabled(true);
    		}
    		else if(msg.what == HANDLE_INSTALLS_REFERRALS_ENABLE) {
    			installsButton.setEnabled(true);
    			referralsButton.setEnabled(true);
    		}
    	}
    };
    
    
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	
    	verifyDeviceButton = (Button)findViewById(R.id.verify_device);
    	verifyDeviceButton.setOnClickListener(this);
    	verifyDeviceButton.setVisibility(View.GONE);
    	verificationStatusButton = (Button)findViewById(R.id.verification_status);
    	verificationStatusButton.setOnClickListener(this);
    	verificationStatusButton.setEnabled(false);
    	verificationStatusButton.setVisibility(View.GONE);
    	
    	discoverContactsButton = (Button)findViewById(R.id.discover_contacts);
    	discoverContactsButton.setOnClickListener(this);
    	recommendInvitesButton = (Button)findViewById(R.id.recommend_invites);
    	recommendInvitesButton.setOnClickListener(this);
    	recommendInvitesButton.setEnabled(false);
    	
    	installsButton = (Button)findViewById(R.id.installs);
    	installsButton.setOnClickListener(this);
    	installsButton.setEnabled(false);
    	referralsButton = (Button)findViewById(R.id.referrals);
    	referralsButton.setOnClickListener(this);
    	referralsButton.setEnabled(false);
    	
    	Discoverer.activate(this, appKey);
    	
    	progressDialog = new ProgressDialog(this);
    	progressDialog.setMessage("Please Wait...");
    	progressDialog.setCancelable(false);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	if(progressDialog != null) {
    		progressDialog.cancel();
    		progressDialog = null;
    	}
    }

    @Override
    public void onClick(View v) {
    	int id = v.getId();
    	
    	switch(id) {
    		case R.id.verify_device: {
    			verifyDevice();
    			break;
    		}
    		case R.id.verification_status: {
    			queryVerifiedStatus();
    			break;
    		}
    		case R.id.discover_contacts: {
    			discoverContacts();
    			break;
    		}
    		case R.id.recommend_invites: {
    			showRecommendedInvites();
    			break;
    		}
    		case R.id.installs: {
    			showInstallsQueryDirectionDialog();
    			break;
    		}
    		case R.id.referrals: {
    			showReferrals();
    			break;
    		}
    	}
    }
    
	private void verifyDevice() {
    	Thread a = new Thread() {
    		@Override
    		public void run() {
    			super.run();
    			handler.sendEmptyMessage(HANDLE_SHOW_LOADING);
				
    			try {
    				String message = Discoverer.getInstance().verifyDevice(false, null);
    				String number = Discoverer.getSmsDest();
					
    				handler.sendEmptyMessage(HANDLE_VERIFICATION_STATUS_ENABLE);
					
    				Uri smsToUri = Uri.parse("smsto:" + number);
    				Intent intent = new Intent(android.content.Intent.ACTION_SENDTO, smsToUri);  
    				intent.putExtra("sms_body", message);   
					
    				startActivity(intent);
    			}
    			catch(AgeException e) {
    				displayError(e);
    			}
    			
    			handler.sendEmptyMessage(HANDLE_HIDE_LOADING);
    		}
    	};
    	a.start();
    	a = null;
	}
    
    private void queryVerifiedStatus() {
    	Thread a = new Thread() {
			@Override
			public void run() {
				super.run();
				handler.sendEmptyMessage(HANDLE_SHOW_LOADING);
				
				try {
					boolean verified = Discoverer.getInstance().queryVerifiedStatus();
					
					if(verified) {
						showMessage(new String[] {"Verified", "Your device has been verified.", "Dismiss"});
					}
					else {
						showMessage(new String[] {"Not Verified", "Your device has NOT been verified. It might take a few minutes for us to receive and process the verification SMS.", "Dismiss"});
					}
				}
				catch(AgeException e) {
					displayError(e);
				}
				
				handler.sendEmptyMessage(HANDLE_HIDE_LOADING);
			}
		};
		a.start();
		a = null;
    }
    
    private void discoverContacts() {
    	Thread a = new Thread() {
			@Override
			public void run() {
				super.run();
				handler.sendEmptyMessage(HANDLE_SHOW_LOADING);
				
				try {
					Discoverer.getInstance().discover();
					
					showMessage(new String[] {"Finished", "Discover order successfully submitted. Please wait a few minutes to query the recommendations from the API.", "Dismiss"});
					
					handler.sendEmptyMessageDelayed(HANDLE_GET_RECOMMENDED_INVITES_BUTTON_ENABLE, 200);
					handler.sendEmptyMessageDelayed(HANDLE_INSTALLS_REFERRALS_ENABLE, 100);
				}
				catch(AgeException e) {
					displayError(e);
				}
				
				handler.sendEmptyMessage(HANDLE_HIDE_LOADING);
			}
		};
		a.start();
		a = null;
    }
    
    private void showRecommendedInvites() {
    	Thread a = new Thread() {
			@Override
			public void run() {
				super.run();
				handler.sendEmptyMessage(HANDLE_SHOW_LOADING);
				
				try {
					Discoverer.getInstance().queryLeads();
					
					Intent intent = new Intent(HookMobileSample.this, SendInvitationsView.class);
					startActivity(intent);
				}
				catch(AgeException e) {
					displayError(e);
				}
	    
				handler.sendEmptyMessage(HANDLE_HIDE_LOADING);
			}
		};
		a.start();
		a = null;
    }

    private void showInstallsQueryDirectionDialog() {
    	String[] menu = new String[] {"Forward", "Backward", "Mutual", "Cancel"};

    	new AlertDialog.Builder(this)
    		.setTitle("Direction of query")
    		.setItems(menu, new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				if(which == 0) {
    					queryInstalls(Directions.FORWARD);
    				}
    				else if(which == 1) {
    					queryInstalls(Directions.BACKWARD);
    				}
    				else if(which == 2) {
    					queryInstalls(Directions.MUTUAL);
    				}
    			}
    		})
    		.show();
    }

    private void queryInstalls(final String direction) {
	    Thread a = new Thread() {
	    	@Override
	    	public void run() {
	    		super.run();
	    		handler.sendEmptyMessage(HANDLE_SHOW_LOADING);
	    		
	    		try {
	    			Discoverer.getInstance().queryInstalls(direction);
	    			
	    			Intent a = new Intent(HookMobileSample.this, ShowInstallsView.class);
    				startActivity(a);
	    		}
	    		catch(AgeException e) {
	    			displayError(e);
	    		}
	    		
	    		handler.sendEmptyMessage(HANDLE_HIDE_LOADING);
	    	}
	    };
	    a.start();
	    a = null;
    }
    
    private void showReferrals() {
    	Thread a = new Thread() {
			@Override
			public void run() {
				super.run();
				handler.sendEmptyMessage(HANDLE_SHOW_LOADING);
				
				try {
					Discoverer.getInstance().queryReferrals();
					
					Intent a = new Intent(HookMobileSample.this, ShowReferralsView.class);
					startActivity(a);
				}
				catch(AgeException e) {
					displayError(e);
				}
				
				handler.sendEmptyMessage(HANDLE_HIDE_LOADING);
			}
		};
		a.start();
		a = null;
    }
    
    private void displayError(AgeException e) {
    	String body = "Hook Mobile server encountered a problem: ";
		
		if(e.getMessage() != null) {
			body += e.getMessage();
		}
		else {
			body += "Unknown Error";
		}
		
		showMessage(new String[] {"Finished", body, "Dismiss"});
    }

    private void showMessage(String[] content) {
    	Message msg = handler.obtainMessage();
		msg.what = HANDLE_SHOW_MESSAGE_DIALOG;
		msg.obj = content;
		handler.sendMessage(msg);
    }
    
    private void showErrorDialog(String title, String message, String buttonText) {
    	new AlertDialog.Builder(this)
    		.setTitle(title)
    		.setMessage(message)
    		.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				
    			}
    		})
    		.show();
    }
    
}

