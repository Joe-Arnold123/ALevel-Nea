package com.example.actualcoursework

class MessageCache {
      var cache = mutableSetOf<String>()
}


fun addMessageToCache(message: String,cache: MutableSet<String>): Boolean {

    val messageTTL=message.takeLast(2)

    val messageId=message.take(2)
    if (!cache.contains(messageId) and (messageTTL.toInt() > 0) ) {
        cache.add(messageId)
        return true


}

    else if (!cache.contains(messageId) and (messageTTL.toInt() == 0)){
        cache.add(messageId)
        return false
    }
    else return false
}
fun removeMessageFromCache(message: String,cache: MutableSet<String>) {

}
fun getId(message: String): String{
    return message.takeLast(2)

}
fun dupplicate(message: String,cache: MutableSet<String>): Boolean {
    if (cache.contains(getId(message)) )
        return true
    else return false
}