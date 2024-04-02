package routeswitcher

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.*
import io.vertx.core.net.SocketAddress
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.httpproxy.HttpProxy
import java.net.URI
import java.util.*

class RouteSwitcherReverseVerticle : AbstractVerticle() {
    private lateinit var httpProxy: HttpProxy
    private lateinit var httpsProxy: HttpProxy
    private lateinit var defaultRequestHandler: Router
    private val ruleManager: RuleManager = RuleManagerFileStoreImpl()

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(RouteSwitcherReverseVerticle::class.java)
    }

    override fun start() {
        httpProxy = prepareHttpProxy(false)
        httpsProxy = prepareHttpProxy(true)
        defaultRequestHandler = prepareDefaultRequestHandler()
        val port = config().getInteger("port")
        vertx.createHttpServer(HttpServerOptions().setSsl(false).setPort(port))
            .webSocketHandler(WebSocketHandler(vertx))
            .requestHandler { handleRequest(it) }
            .listen(port)
            .onSuccess { log.info("reverse proxy server started at port: {}", it.actualPort()) }
            .onFailure { log.error("error start reverse proxy server", it) }
    }

    private fun prepareDefaultRequestHandler(): Router {
        return Router.router(vertx).apply {
            route().handler(StaticHandler.create("web"))
            route().handler(BodyHandler.create())
            route("/route-switcher/*").handler(StaticHandler.create("./"))
            route("/rule-manage/*").subRouter(RuleManageHandler(vertx, ruleManager).createRouter())
        }
    }

    private fun handleRequest(request: HttpServerRequest) {
        getFirstMatchedRule(request)?.apply {
            if (target.startsWith("https"))
                httpsProxy.handle(request)
            else
                httpProxy.handle(request)
        } ?: defaultRequestHandler.handle(request)
    }

    private fun prepareHttpProxy(isSsl: Boolean): HttpProxy {
        val httpClient = vertx.createHttpClient(HttpClientOptions().setSsl(isSsl))
        return HttpProxy.reverseProxy(httpClient).also { it.originRequestProvider(this::prepareTargetRequest) }
    }

    private fun prepareTargetRequest(serverRequest: HttpServerRequest, client: HttpClient): Future<HttpClientRequest?> {
        val firstMatchedRule = getFirstMatchedRule(serverRequest)
        val targetServer = firstMatchedRule!!.target
        val uuid = UUID.randomUUID().toString().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[4]
        val fromIP = serverRequest.remoteAddress().host()
        val method = serverRequest.method().name()
        val targetUrl = targetServer + serverRequest.uri()
        log.info("request:  $uuid [${serverRequest.uri()}] [$fromIP] [$method] => $targetUrl")
        return client.request(RequestOptions().setServer(getTargetSocketAddress(targetServer)))
            .onSuccess { clientRequest ->
                clientRequest.response()
                    .onSuccess { log.info("response: {} [{}]", uuid, it.statusCode()) }
                    .onFailure { log.error("error: {}", uuid, it) }
            }
            .onFailure { err -> log.error("error: {}", uuid, err) }
    }

    private fun getFirstMatchedRule(serverRequest: HttpServerRequest): Rule? {
        val uri = serverRequest.uri()
        val fromIP = serverRequest.remoteAddress().host()

        return ruleManager.retrieveRules()
            .filter { uri.startsWith(it.uriPrefix) && it.fromIP.split(",").contains(fromIP) }
            .getOrElse(0) {
                ruleManager.retrieveRules()
                    .filter { entry -> uri.startsWith(entry.uriPrefix) && entry.fromIP.isEmpty() }
                    .getOrNull(0)
            }
    }

    private fun getTargetSocketAddress(targetServer: String): SocketAddress {
        val uri = URI.create(targetServer)
        val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
        return SocketAddress.inetSocketAddress(port, uri.host)
    }
}
