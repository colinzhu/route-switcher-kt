package routeswitcher

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.http.ServerWebSocket
import java.io.OutputStream
import java.io.PrintStream


internal class WebSocketHandler(private val vertx: Vertx) : Handler<ServerWebSocket> {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(WebSocketHandler::class.java)
    }

    init {
        redirectStdOutToWeb()
    }

    override fun handle(webSocket: ServerWebSocket) {
        log.info("web socket connected")
        webSocket.writeTextMessage("Welcome to the Route Switcher. For historical log information, please refer to the 'route-switcher.log' file.")
        vertx.eventBus().consumer("console.log") { message: Message<Any?> ->
            webSocket.writeTextMessage(message.body() as String?) // redirect the message to websocket (web)
        }
    }

    private fun redirectStdOutToWeb() {
        val webConsoleOutputStream: OutputStream = object : OutputStream() {
            private val originalOutStream: OutputStream = System.out
            private val sb = StringBuilder()
            override fun write(b: Int) {
                if (b == '\n'.code) {
                    vertx.eventBus().publish("console.log", sb.toString())
                    sb.setLength(0)
                } else {
                    sb.append(b.toChar())
                }
                originalOutStream.write(b) //keep the original console output
            }
        }
        System.setOut(PrintStream(webConsoleOutputStream))
    }
}

