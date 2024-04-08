package routeswitcher

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

internal class RuleManageHandler(private val vertx: Vertx) {
    private val ruleService = RuleServiceFileStoreImpl()

    companion object {
        private val log = LoggerFactory.getLogger(RuleManageHandler::class.java)
    }

    fun createRouter(): Router {
        return Router.router(vertx).apply {
            route(HttpMethod.GET, "/api/rules").handler(::retrieveRules)
            route(HttpMethod.POST, "/api/rules").handler(::addOrUpdateOneRule)
            route(HttpMethod.DELETE, "/api/rules").handler(::deleteOneRule)
        }
    }

    private fun retrieveRules(ctx: RoutingContext) {
        runCatching { ruleService.retrieveRules() }
            .onSuccess(ctx::json)
            .onFailure { handleThrowable(ctx, it) }
    }

    private fun addOrUpdateOneRule(ctx: RoutingContext) {
        log.info("update one rule request body:{}", ctx.body().asString())
        handleOneRule(ctx, ruleService::addOrUpdate)
    }

    private fun deleteOneRule(ctx: RoutingContext) {
        log.info("delete one rule request body:{}", ctx.body().asString())
        handleOneRule(ctx, ruleService::delete)
    }

    private inline fun handleOneRule(ctx: RoutingContext, ruleHandler: (Rule) -> Future<Void>) {
        runCatching {
            ruleHandler.invoke(ctx.body().asPojo(Rule::class.java))
                .onSuccess { ctx.json(ruleService.retrieveRules()) }
                .onFailure { handleThrowable(ctx, it) }
        }.onFailure {
            handleThrowable(ctx, it)
        }
    }

    private fun handleThrowable(routingContext: RoutingContext, e: Throwable) {
        log.error("fail to handle request", e)
        routingContext.response().setStatusCode(500).end(Json.encode(mapOf("reason" to e.message)))
    }
}

