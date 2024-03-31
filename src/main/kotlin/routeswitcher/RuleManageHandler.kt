package routeswitcher

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory


internal class RuleManageHandler(private val vertx: Vertx, private val ruleManager: RuleManager) {
    companion object {
        private val log = LoggerFactory.getLogger(RuleManageHandler::class.java)
    }

    fun createRouter(): Router {
        val router = Router.router(vertx)
        router.route(HttpMethod.GET, "/api/rules").handler { retrieveRules(it) }
        router.route(HttpMethod.POST, "/api/rules").handler { addOrUpdateOneRule(it) }
        router.route(HttpMethod.DELETE, "/api/rules").handler { deleteOneRule(it) }
        return router
    }

    private fun retrieveRules(routingContext: RoutingContext) {
        log.debug("get rules request body:{}", routingContext.body().asString())
        routingContext.json(ruleManager.retrieveRules())
    }

    private fun addOrUpdateOneRule(routingContext: RoutingContext) {
        log.debug("update one rule request body:{}", routingContext.body().asString())
        try {
            val rule = routingContext.body().asPojo(Rule::class.java)
            ruleManager.addOrUpdate(rule).onSuccess { _ -> routingContext.json(ruleManager.retrieveRules()) }
        } catch (e: Exception) {
            routingContext.response().setStatusCode(500).end(Json.encode(mapOf("reason" to e.message)))
        }
    }

    private fun deleteOneRule(routingContext: RoutingContext) {
        log.debug("delete one rule request body:{}", routingContext.body().asString())
        try {
            val rule = routingContext.body().asPojo(Rule::class.java)
            ruleManager.delete(rule).onSuccess { _ -> routingContext.json(ruleManager.retrieveRules()) }
        } catch (e: Exception) {
            routingContext.response().setStatusCode(500).end(Json.encode(mapOf("reason" to e.message)))
        }
    }
}

