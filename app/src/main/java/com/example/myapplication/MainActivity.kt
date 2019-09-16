package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.Utils.dpFromPx
import com.example.myapplication.Utils.getUniqueID
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.*
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.enums.PNPushType
import com.pubnub.api.enums.PNReconnectionPolicy
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNDeleteMessagesResult
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.push.PNPushAddChannelResult
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var mPubNub: PubNub
    private var firebaseToken = ""
    private val mChannelName: String = "Demo-Channel111"
    private var messages = ArrayList<Message>()
    private lateinit var adapter: MessageAdapter
    private var subscribeCallback: SubscribeCallback = object : SubscribeCallback() {
        override fun status(pubnub: PubNub, status: PNStatus) {
            Log.e(MainActivity::class.java.canonicalName, "status")
        }

        override fun message(pubnub: PubNub, message: PNMessageResult) {
            runOnUiThread {
                addMessage(message)
            }
        }

        //        {"message":"xx","uId":"c73aae38ebcaf859"}
        override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {
            Log.e(MainActivity::class.java.canonicalName, "presence")
            Toast.makeText(this@MainActivity, "presence", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setMessageAdapter()
        initializePubNub()
        initializeMessageListener()
        btnSend.setOnClickListener {
            Utils.hideKeyboard(this)
            sendMessage()
        }
        getOldMessages()
        registerToken()
    }

    private fun setMessageAdapter() {
        adapter = MessageAdapter(messages, this)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter
        rvMessages.addItemDecoration(SpacesItemDecoration(dpFromPx(this, 5).toInt()))
    }

    private fun initializePubNub() {
        val pnConfiguration = PNConfiguration()
        pnConfiguration.publishKey = PUB_KEY
        pnConfiguration.subscribeKey = SUB_KEY
        pnConfiguration.logVerbosity = PNLogVerbosity.BODY
        pnConfiguration.reconnectionPolicy = PNReconnectionPolicy.LINEAR
        pnConfiguration.maximumReconnectionRetries = 10
        mPubNub = PubNub(pnConfiguration)

    }

    private fun initializeMessageListener() {
        mPubNub.run {
            addListener(subscribeCallback)
            subscribe()
                .channels(Arrays.asList(mChannelName))
                .withPresence()
                .execute()
        }
    }

    private fun getOldMessages() {
        mPubNub.history()
            .channel(mChannelName)
            .count(100)
            .start(getLastTime())
            .includeTimetoken(true)
            .async(object : PNCallback<PNHistoryResult>() {
                override fun onResponse(result: PNHistoryResult?, status: PNStatus) {
                    if (!status.isError) {
                        if (result != null && result.messages.isNotEmpty()) {
                            for (obj in result.messages) {
                                val message = Gson().fromJson<Message>(obj.entry, Message::class.java)
                                message.isOwnMessage = message.uId.equals(getUniqueID(this@MainActivity))
                                message.timestamp = obj.timetoken
                                messages.add(message)
                            }
                            adapter.notifyDataSetChanged()
                            rvMessages.scrollToPosition(messages.size - 1)
                        }
                    }
                }
            })
    }

    private fun registerToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnSuccessListener { instanceIdResult -> sendToken(instanceIdResult.token) }
    }

    private fun sendToken(token: String) {
        firebaseToken = token
        mPubNub.addPushNotificationsOnChannels()
            .pushType(PNPushType.GCM)
            .channels(Arrays.asList(mChannelName))
            .deviceId(token)
            .async(object : PNCallback<PNPushAddChannelResult>() {
                override fun onResponse(result: PNPushAddChannelResult, status: PNStatus) {

                }
            })
    }

    private fun addMessage(message: PNMessageResult) {
        val receivedMessage = Gson().fromJson<Message>(message.message, Message::class.java)
        if (!receivedMessage.isDelete) {
            receivedMessage.isOwnMessage = receivedMessage.uId == getUniqueID(this)
            receivedMessage.timestamp = message.timetoken
            messages.add(receivedMessage)
            adapter.notifyItemInserted(messages.size - 1)
            rvMessages.scrollToPosition(messages.size - 1)
        } else {
            val pos = messages.indexOf(receivedMessage)
            if (pos != -1 && !receivedMessage.isOwnMessage) {
                messages.removeAt(pos)
                adapter.notifyItemRemoved(pos)
            }

        }
    }

    private fun getLastTime(): Long? {
        if (!messages.isNullOrEmpty()) {
            return messages[0].timestamp
        }
        return null
    }

    private fun sendMessage() {
        if (etMsg.text.toString().isEmpty()) {
            return
        }
        mPubNub
            .publish()
            .channel(mChannelName)
            .shouldStore(true)
            .message(generateMessage(etMsg.text.toString()))
            .async(object : PNCallback<PNPublishResult>() {
                override fun onResponse(result: PNPublishResult, status: PNStatus) {
                    if (status.isError) {
                        Toast.makeText(this@MainActivity, "Message Not Sent", Toast.LENGTH_SHORT).show()
                    } else {
                        etMsg.setText("")
                    }
                }
            })
    }

    private fun generateMessage(rawMessage: String): JsonElement {
        val message = Message(getUniqueID(this), rawMessage)
        val rawJsonObj = JsonParser().parse(Gson().toJson(message)).asJsonObject
        val pushData = JsonObject()
        pushData.addProperty("msg", rawMessage)
        pushData.addProperty("id", getUniqueID(this))

        val pushDataException = JsonArray()
        pushDataException.add(firebaseToken)

        val pushNotificationPayLoad = JsonObject()
        pushNotificationPayLoad.add("pn_exceptions", pushDataException)
        pushNotificationPayLoad.add("data", pushData)

        rawJsonObj.add("pn_gcm", pushNotificationPayLoad)
        return rawJsonObj
    }

    private fun deleteMessage(clickedItemPosition: Int) {
        mPubNub.deleteMessages()
            .channels(Arrays.asList(mChannelName))
            .start(messages[clickedItemPosition].timestamp - 1)
            .end(messages[clickedItemPosition].timestamp + 1)
            .async(object : PNCallback<PNDeleteMessagesResult>() {
                override fun onResponse(result: PNDeleteMessagesResult?, status: PNStatus?) {
                    if (status != null && !status.isError) {
                        messages.removeAt(clickedItemPosition)
                        adapter.notifyItemRemoved(clickedItemPosition)
//                        sendDeleteMessage(messages[clickedItemPosition].timestamp)
                    }
                }
            })
    }


    private fun sendDeleteMessage(messageTimeStamp: Long) {
        mPubNub
            .publish()
            .channel(mChannelName)
            .message(generateDeleteMessage(messageTimeStamp))
            .async(object : PNCallback<PNPublishResult>() {
                override fun onResponse(result: PNPublishResult, status: PNStatus) {
                    if (status.isError) {
                        Toast.makeText(this@MainActivity, "Message Not Sent", Toast.LENGTH_SHORT).show()
                    } else {
                        etMsg.setText("")
                    }
                }
            })
    }

    private fun generateDeleteMessage(messageTimeStamp: Long): JsonElement {
        val message = Message(getUniqueID(this), "")
        message.timestamp = messageTimeStamp
        message.isDelete = true
        return JsonParser().parse(Gson().toJson(message)).asJsonObject
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                deleteAllMessages()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

    }

    private fun deleteAllMessages() {
        mPubNub.deleteMessages().channels(Arrays.asList(mChannelName))
            .async(object : PNCallback<PNDeleteMessagesResult>() {
                override fun onResponse(result: PNDeleteMessagesResult?, status: PNStatus?) {
                    if (status != null && !status.isError) {
                        messages.clear()
                        adapter.notifyDataSetChanged()
                    }
                }
            })
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 0) {
            val clickedItemPosition = item.order
            deleteMessage(clickedItemPosition)
        }
        return super.onContextItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPubNub.unsubscribeAll()
        mPubNub.forceDestroy()
    }
}
