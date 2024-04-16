package routeswitcher

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.core.http.httpServerOptionsOf
import org.slf4j.LoggerFactory

class RouteSwitcherVerticle : AbstractVerticle() {
    companion object {
        private val log = LoggerFactory.getLogger(RouteSwitcherVerticle::class.java)
    }

    override fun start(promise: Promise<Void>) {
        val defaultHandler = staticAndRuleManageHandler()
        val proxyHandler = ProxyHandler(vertx)

        val portNbr = config().getInteger("port")
        vertx.createHttpServer(httpServerOptionsOf(ssl = false, port = portNbr))
            .webSocketHandler(WebSocketHandler(vertx))
            .requestHandler { proxyHandler.findRule(it)?.run { proxyHandler.handle(it) } ?: defaultHandler.handle(it) }
            .listen(portNbr)
            .onSuccess {
                log.info("reverse proxy server started at port: ${it.actualPort()}")
                promise.complete()
            }
            .onFailure { promise.fail("error start reverse proxy server, " + it.message) }
    }

    private fun staticAndRuleManageHandler(): Router {
        return Router.router(vertx).apply {
            route().handler(StaticHandler.create("web"))
            route().handler(BodyHandler.create())
            route("/route-switcher/*").handler(StaticHandler.create("./"))
            route("/rule-manage/*").subRouter(RuleManageHandler(vertx).createRouter())
        }
    }

}
