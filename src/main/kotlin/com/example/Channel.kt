package com.example

import java.util.*


const val DEFAULT_CHANNEL_NAME = "general"

class Channel(val name: String) {
    val users = Collections.synchronizedSet<Connection>(LinkedHashSet())
    val channels = mutableMapOf<String, Channel>()
}
