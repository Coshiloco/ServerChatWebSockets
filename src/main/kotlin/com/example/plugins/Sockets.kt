package com.example.plugins


import com.example.Channel
import com.example.Connection
import com.example.DEFAULT_CHANNEL_NAME
import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet


fun Application.configureSockets() {
    install(WebSockets) {
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val channels = Collections.synchronizedMap(mutableMapOf<String, Channel>())
        webSocket("/chat") {
            println("Adding user!")
            val connection = Connection(this)
            var currentChannel = channels.getOrPut(DEFAULT_CHANNEL_NAME, { Channel(DEFAULT_CHANNEL_NAME) })
            currentChannel.users += connection
            connection.channel = currentChannel

            try {
                send("You are connected! There are ${currentChannel.users.count()} users here.")
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    if (receivedText.startsWith("/")) {
                        handleCommand(receivedText, connection, channels)
                    } else {
                        val textWithUsername = "[${connection.name}]: $receivedText"
                        currentChannel.users.forEach {
                            it.session.send(textWithUsername)
                        }
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Removing $connection!")
                currentChannel.users -= connection
            }
        }
    }
}

suspend fun handleCommand(
    command: String,
    user: Connection,
    channels: MutableMap<String, Channel>
) {
    when {
        command.startsWith("/join") -> {
            val channelName = command.substring(6).trim()
            val channel = channels.getOrPut(channelName, { Channel(channelName) })
            user.channel?.users?.remove(user)
            channel.users.add(user)
            user.channel = channel
            user.session.send("You have joined the $channelName channel.")
        }

        command.startsWith("/leave") -> {
            user.channel?.users?.remove(user)
            user.channel = null
            val defaultChannel = channels.getValue(DEFAULT_CHANNEL_NAME)
            defaultChannel.users.add(user)
            user.session.send("You have left the current channel.")
        }

        command.startsWith("/nick") -> {
            val newNick = command.substring(6).trim()
            user.name = newNick
            user.session.send("Your nickname has been updated to $newNick.")
        }

        command.startsWith("/msg") -> {
            val parts = command.split(" ", limit = 3)
            val targetName = parts[1]
            val message = parts[2]
            val target = user.channel?.users?.firstOrNull { it.name == targetName }
            if (target != null) {
                val messageWithUsername = "[${user.name} -> ${target.name}]: $message"
                user.session.send(messageWithUsername)
                target.session.send(messageWithUsername)
            } else {
                user.session
                    .send("User $targetName not found in current channel.")
            }
        }

        else -> {
            user.session.send("Unknown command: $command")
        }
    }
}


