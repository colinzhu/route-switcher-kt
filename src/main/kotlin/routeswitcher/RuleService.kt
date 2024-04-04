package routeswitcher

import io.vertx.core.Future


internal interface RuleService {
    fun retrieveRules(): Set<Rule>
    fun addOrUpdate(rule: Rule): Future<Void>
    fun delete(rule: Rule): Future<Void>
}
