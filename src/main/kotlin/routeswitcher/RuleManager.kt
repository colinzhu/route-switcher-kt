package routeswitcher

import io.vertx.core.Future


internal interface RuleManager {
    fun retrieveRules(): Set<Rule>
    fun addOrUpdate(rule: Rule): Future<Void>
    fun delete(rule: Rule): Future<Void>
}
