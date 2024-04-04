package routeswitcher

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory


internal class RuleManageHandler(private val vertx: Vertx) {
    private val ruleService: RuleService = RuleServiceFileStoreImpl()
    companion object {
        private val log = LoggerFactory.getLogger(RuleManageHandler::class.java)
    }

    fun createRouter(): Router {
        return Router.router(vertx).apply {
            route(HttpMethod.GET, "/api/rules").handler { retrieveRules(it) }
            route(HttpMethod.POST, "/api/rules").handler { addOrUpdateOneRule(it) }
            route(HttpMethod.DELETE, "/api/rules").handler { deleteOneRule(it) }
        }
    }

    private fun retrieveRules(routingContext: RoutingContext) {
        log.debug("get rules request body:{}", routingContext.body().asString())
        runCatching { ruleService.retrieveRules() }
            .onSuccess { routingContext.json(it) }
            .onFailure { routingContext.response().setStatusCode(500).end(Json.encode(mapOf("reason" to it.message))) }
    }

    private fun addOrUpdateOneRule(routingContext: RoutingContext) {
        log.debug("update one rule request body:{}", routingContext.body().asString())

        try {
            val rule = routingContext.body().asPojo(Rule::class.java)
            ruleService.addOrUpdate(rule).onSuccess { _ -> routingContext.json(ruleService.retrieveRules()) }
        } catch (e: Exception) {
            log.error("fail to add or update rule", e)
            routingContext.response().setStatusCode(500).end(Json.encode(mapOf("reason" to e.message)))
        }
    }

    private fun deleteOneRule(routingContext: RoutingContext) {
        log.debug("delete one rule request body:{}", routingContext.body().asString())
        try {
            val rule = routingContext.body().asPojo(Rule::class.java)
            ruleService.delete(rule).onSuccess { _ -> routingContext.json(ruleService.retrieveRules()) }
        } catch (e: Exception) {
            log.error("fail to delete rule", e)
            routingContext.response().setStatusCode(500).end(Json.encode(mapOf("reason" to e.message)))
        }
    }
}

