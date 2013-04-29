# App Growth Engine for Android v1.1.5

Hook Mobile has developed a unique and powerful tool for mobile app developers to market your app: App Growth Engine (AGE) SDK. This open source Java library allows you to integrate AGE into your Android application.


# Getting Started

<h3>Step 1: Register your Android App with Hook Mobile</h3>

To begin integrating with the Hook Mobile AGE Platform, <a href ="http://hookmobile.com/login.html">register your app with Hook Mobile</a> and enter your app's information.

<img src="http://hookmobile.com/images/screenshot/create-app.png"/><br>
You are going to need the app key when setting up your Android app.

<h3>Step 2: Install the AGE Android SDK</h3>

Before you begin development with the AGE Android SDK, you will need to install the Android SDK and download the AGE SDK. Please notice that AGE SDK requires Android SDK 2.1 (API Level 7) or above.

* Install <a href ="http://developer.android.com/sdk/index.html">Android SDK</a>
* Download <a href ="https://github.com/hookmobile/App-Growth-Engine-Android-SDK">AGE Android SDK (GitHub)</a>

To install the AGE SDK, copy <code>age-1.1.5.jar</code> to libs folder and add it to classpath in your Android project. Also, you need to add following permissions to your <code>AndroidManifest.xml</code>.

<pre><code>		&lt;uses-permission android:name="android.permission.INTERNET"&gt;
	 &lt;uses-permission android:name="android.permission.READ_CONTACTS"&gt;
	 &lt;uses-permission android:name="android.permission.READ_PHONE_STATE"&gt;
     &lt;uses-permission android:name="android.permission.SEND_SMS"&gt;
     &lt;uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"&gt;
     &lt;uses-permission android:name="android.permission.ACCESS_WIFI_STATE"&gt;</code></pre>

NOTE: <code>android.permission.SEND_SMS</code> is not required if you decide to use only Hook Mobile virtual number to send invitations.

# Step 3: Use the AGE Android SDK

Once you have created an application, you can start the SDK when you initialize your activity with the app key you have registered. The first parameter is the type of Android <code>Context</code>. You can pass <code>this</code> object in your Android <code>Activity</code>.

<pre><code><a href= "http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Override.html">@Override</a>
protected void onCreate(android.os.Bundle savedInstanceState) {
 
    Discoverer.activate(<b>this</b>, <b>"Your-App-Key"</b>);
    // ... ...
 
}
</code></pre>

The usage of the SDK is illustrated in the sample application. Just open the Eclipse project, fill in the app key in the <code>HookMobileSample</code> class in the package <code>com.hookmobile.age.sample</code>, and run the project (ideally in a physical Android phone attached to the dev computer). The buttons in the sample application demonstrate key actions you can perform with the SDK.

<img src="http://hookmobile.com/images/screenshot/android-sample-app.png"/>


# Smart Invitation

<h3>Step 1: Discover</h3>

To get a list of contacts from user's addressbook that are most likely to install your app, you need to execute a discovery call:

<code>Discoverer.getInstance().discover();</code>

<img src="http://hookmobile.com/images/screenshot/android-sample-leads.png"/>

<h3>Step 2: Get Recommended Invites</h3>

It takes Hook Mobile seconds to determine the devices for each of the phone numbers, and come up with an optimized list. The query returns you a list of <code>Leads</code>, which contains phone numbers and device types ordered by the mobile relationship.

<code><a href ="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/List.html">List</a><Lead> leads = Discoverer.getInstance().queryLeads();</code>

Now, you can prompt your user to send personal invites to their friends to maximize the chance of referral success!

<h3>Step 3: Send Invitations</h3>

The AGE platform enables you to track the performance of your referrals via customized URLs that you can use in invite messages. The <code>newReferral</code> method creates a referral message with the custom URL.

<code>long referralId = Discoverer.getInstance().newReferral(phones, useVirtualNumber, name, message)</code>;

The <code>phones</code> parameter is a <code>List</code> of phone numbers you wish to send referrals to. It is typically a list selected from the leads returned by <code>Discoverer.getInstance().queryLeads()</code>. The <code>useVirtualNumber</code> option specifies whether AGE should send out the referrals via its own virtual number. If not, the referrals will be sent out from the app user's device. The <code>name</code> parameter is the name of the app user, or invitation sender. The optional <code>message</code> parameter takes a message template in case you need to overwrite the default one configured in your app profile. Following is a sample message template with the predefined placeholders or variables:

<code><a href ="http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/String.html">String</a> message = "I thought you might be interested in this app, check it out here %link% - %name%";</code>

<code>%link%</code> - Customized referral URL to download the app. <br>
<code>%name%</code> - Name of the invitation sender supplied within <code>newReferral</code> call.

<img src="http://hookmobile.com/images/screenshot/android-sample-send.png"/><br>
NOTE: if your device is not a SMS device (e.g., a tablet), the AGE server will send out the referral message via the virtual number automatically.

# Tracking Referrals and Installs

<h3>Step 1: Track Your Referrals</h3>

The AGE API also allows you to track all referrals you have sent from any device, and get the referrals' click throughs. This makes it possible for you to track referral performance of individual devices, and potentially reward the users who generate the most referral click throughs.

<code><a href ="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/List.html">List</a><Referral> referrals = Discoverer.getInstance().queryReferrals();</code>

Or, if you would like to query an individual referral:
<code>Referral referral = Discoverer.getInstance().queryReferral(referralId);</code><br>
<code>referralId</code> parameter is the ID of the referral you want to query.

<h3>Step 2: Track Friends Who Install The Same App</h3>

The AGE platform allows you to find friends who also install the same app from your addressbook. To query for friends installs in your addressbook, you must call the <code>discover</code> method first. And then, you can call the <code>queryInstalls</code>. This method takes a string parameter that indicates how the searching and matching of addressbook should be done.

* <code>FORWARD</code> - Find contacts within your address book who has the same app.

* <code>BACKWARD</code> - Find other app users who has your phone number in their address book. When to use this? When the app wants to suggest a long lost friend who has your contact, but not vice versa.

* <code>MUTUAL</code> - Find contacts within your address book who has the same app and who also has your contact in his/her address book. This query may be useful for engaging a friend to play in multi-player game who already plays the game.

Below is an example:

<code><a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/List.html">List</a><String> installs = Discoverer.getInstance().queryInstalls(Directions.FORWARD);</code>

<img src="http://hookmobile.com/images/screenshot/android-sample-track.png"/><img src="http://hookmobile.com/images/screenshot/android-sample-installs.png"/>

You can also find this tutorial on our <a href="http://hookmobile.com/android-tutorial.html">website</a>.
