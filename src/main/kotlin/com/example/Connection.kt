package com.example

import io.ktor.websocket.*
import java.util.concurrent.atomic.*

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    var name = "user${lastId.getAndIncrement()}"
    var channel: Channel? = null
}