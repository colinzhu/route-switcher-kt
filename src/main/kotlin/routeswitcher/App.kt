package routeswitcher

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.deploymentOptionsOf
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path


object App {
    private val log = LoggerFactory.getLogger(App::class.java)
    private const val APP_CONFIG_FILE = "config.json"

    @JvmStatic
    fun main(args: Array<String>) {
        val vertx = Vertx.vertx()
        vertx.deployVerticle(RouteSwitcherVerticle::class.java, deploymentOptionsOf(config = loadConfig()))
            .onFailure {log.error(it.message, it)}
    }

    private fun loadConfig(): JsonObject {
        return runCatching {
            JsonObject(Files.readString(Path.of(APP_CONFIG_FILE)))
        }.getOrElse {
            log.warn("no config.json file found, use random port")
            JsonObject().put("port", 0)
        }
    }
}