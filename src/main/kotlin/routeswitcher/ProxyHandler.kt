package routeswitcher

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.net.SocketAddress
import io.vertx.httpproxy.HttpProxy
import io.vertx.kotlin.core.http.httpClientOptionsOf
import io.vertx.kotlin.core.http.requestOptionsOf
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

internal class ProxyHandler(private val vertx: Vertx): Handler<HttpServerRequest>{
    private val httpProxy: HttpProxy
    private val httpsProxy: HttpProxy
    private val ruleService: RuleService = RuleServiceFileStoreImpl()

    companion object {
        private val log = LoggerFactory.getLogger(ProxyHandler::class.java)
    }

    init {
        httpProxy = prepareHttpProxy(isSsl = false)
        httpsProxy = prepareHttpProxy(isSsl = true)
    }

    override fun handle(request: HttpServerRequest) {
        val target = findRule(request)!!.target
        val proxy = if (target.startsWith("https")) httpsProxy else httpProxy
        proxy.handle(request)
    }

    private fun prepareHttpProxy(isSsl: Boolean): HttpProxy {
        val httpClient = vertx.createHttpClient(httpClientOptionsOf(ssl = isSsl))
        return HttpProxy.reverseProxy(httpClient).also { it.originRequestProvider(this::prepareTargetRequest) }
    }

    private fun prepareTargetRequest(serverRequest: HttpServerRequest, client: HttpClient): Future<HttpClientRequest?> {
        val firstMatchedRule = findRule(serverRequest)
        val targetServer = firstMatchedRule!!.target
        val uuid = UUID.randomUUID().toString().splitToSequence("-").last()
        val fromIP = serverRequest.remoteAddress().host()
        val method = serverRequest.method().name()
        val targetUrl = targetServer + serverRequest.uri()
        log.info("request:  $uuid [${serverRequest.uri()}] [$fromIP] [$method] => $targetUrl")

        return client.request(requestOptionsOf(server = getTargetSocketAddress(targetServer)))
            .onSuccess { clientRequest ->
                clientRequest.response()
                    .onSuccess { log.info("response: {} [{}]", uuid, it.statusCode()) }
                    .onFailure { log.error("error: {}", uuid, it) }
            }
            .onFailure { err -> log.error("error: {}", uuid, err) }
    }

    fun findRule(serverRequest: HttpServerRequest): Rule? {
        val uri = serverRequest.uri()
        val fromIP = serverRequest.remoteAddress().host()

        return ruleService.retrieveRules().firstOrNull {
            uri.startsWith(it.uriPrefix) && it.fromIP.split(",").contains(fromIP)
        } ?: ruleService.retrieveRules().firstOrNull {
            uri.startsWith(it.uriPrefix) && it.fromIP.isEmpty()
        }
    }

    private fun getTargetSocketAddress(targetServer: String): SocketAddress {
        val uri = URI.create(targetServer)
        val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
        return SocketAddress.inetSocketAddress(port, uri.host)
    }

}

