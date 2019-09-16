# ChatDemo
Chat Demo with pubnub

1. After login/signup in PubNub Create 1 Applicaion  and enable Stream Controller and Storage & PlayBack from your keyset 
2. In your Android Project you need to add two dependecies in your App Gradle.
```
    implementation 'com.pubnub:pubnub-gson:4.23.0'
    implementation 'com.google.code.gson:gson:2.8.5'
```
3. Add Internet Permission in your AndroidMenifest.xml

   ``` <uses-permission android:name="android.permission.INTERNET" />```
     
4. In your ChatActivity we need to first initialize PubNub
```
        val pnConfiguration = PNConfiguration()
        pnConfiguration.publishKey = PUB_KEY
        pnConfiguration.subscribeKey = SUB_KEY
        pnConfiguration.logVerbosity = PNLogVerbosity.BODY
        pnConfiguration.reconnectionPolicy = PNReconnectionPolicy.LINEAR
        pnConfiguration.maximumReconnectionRetries = 10
        mPubNub = PubNub(pnConfiguration)
```
      (Note : For publisher key and subscriber key you can find it from key
      section in PubNub Web DashBoard)

5. Now first we will send message in our channel
```
  private val mChannelName: String = "TestChannel"
  mPubNub
 	  .publish()
 	  .channel(mChannelName)
 	  .shouldStore(true)
 	  .message(“your message object prefer json for conversion”)
 	  .async(object : PNCallback<PNPublishResult>() {
 		  override fun onResponse(result: PNPublishResult, status: PNStatus) {
 			  if (status.isError) {
 				  “Error in Message Sending”
 			  } 
 		  }
 	  })
```
```
  (Note : Channel name should be unique per conversion section in PubNub Web DashBoard)
```
6. To receive message we need to add listener to channel.
```
  private var subscribeCallback: SubscribeCallback = object :SubscribeCallback() {
      		override fun status(pubnub: PubNub, status: PNStatus) {    }
      		override fun message(pubnub: PubNub, message: PNMessageResult) {
        		  runOnUiThread {
              			// “Your Message Object” 
       			   }
  		  }
    	  	override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {    }
	  }
```
7. If we need to send push notification then we need to create firebase project and add FCM dependency.
  Then we need to add FCM server key from firebase project setting and add in PubNub dashboard
8. To send push notification we must need to send pushnotification data along with message json

  ***Push notification data must be inside pn_gcm.data object***

  ***To remove self or any other user from getting push notification add all token in json array and send in pn_gcm.pn_exceptions object***
