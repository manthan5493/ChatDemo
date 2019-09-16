package com.example.myapplication

class Message(val uId: String, val message: String) {
    var isOwnMessage: Boolean=false
    var timestamp: Long=0
    var isDelete: Boolean=false
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (uId != other.uId) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uId.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

}