# App Growth Engine for Android v1.1.5

Hook Mobile has developed a unique and powerful tool for mobile app developers to market your app: App Growth Engine (AGE) SDK. This open source Java library allows you to integrate AGE into your Android application.


# Getting Started

<h3>Step 1: Register your Android App with Hook Mobile</h3>

To begin integrating with the Hook Mobile AGE Platform, <a href ="http://hookmobile.com/login.html">register your app with Hook Mobile</a> and enter your app's information.

<img src="http://hookmobile.com/images/screenshot/create-app.png"/>
You are going to need the app key when setting up your Android app.

<h3>Step 2: Install the AGE Android SDK</h3>

Before you begin development with the AGE Android SDK, you will need to install the Android SDK and download the AGE SDK. Please notice that AGE SDK requires Android SDK 2.1 (API Level 7) or above.

* Install <a href ="http://developer.android.com/sdk/index.html">Android SDK</a>
* Download <a href ="https://github.com/hookmobile/App-Growth-Engine-Android-SDK">AGE Android SDK (GitHub)</a>

To install the AGE SDK, copy age-1.0.2.jar to libs folder and add it to classpath in your Android project. Also, you need to add following permissions to your AndroidManifest.xml.

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.SEND_SMS"/>
NOTE: android.permission.SEND_SMS is not required if you decide to use only Hook Mobile virtual number to send invitations.

Step 3: Use the AGE Android SDK

Once you have created an application, you can start the SDK when you initialize your activity with the app key you have registered. The first parameter is the type of Android Context. You can pass this object in your Android Activity.

@Override
protected void onCreate(android.os.Bundle savedInstanceState) {
 
    Discoverer.activate(this, "Your-App-Key");
    // ... ...
 
}
The usage of the SDK is illustrated in the sample application. Just open the Eclipse project, fill in the app key in the HookMobileSample class in the package com.hookmobile.age.sample, and run the project (ideally in a physical Android phone attached to the dev computer). The buttons in the sample application demonstrate key actions you can perform with the SDK.


# Sample Application

This library includes a sample application to guide you in development.

