package routeswitcher

import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


internal class RuleManagerFileStoreImpl : RuleManager {
    private val rules: MutableSet<Rule> = HashSet()

    companion object {
        private val log = LoggerFactory.getLogger(RuleManagerFileStoreImpl::class.java)
        private const val RULES_FILE_NAME = "rules.json"
    }

    init {
        loadRules()
    }

    private fun loadRules() {
        runCatching {
            val rulesStr = Files.readString(Path.of(RULES_FILE_NAME))
            JsonArray(rulesStr).forEach { rules.add((it as JsonObject).mapTo(Rule::class.java)) }
        }.onFailure {
            log.warn("no rules.json file found or failed to load rules, use empty rules")
        }
    }

    private fun persistRules() {
        try {
            Files.writeString(Path.of(RULES_FILE_NAME), JsonArray(rules.stream().toList()).encodePrettily())
        } catch (e: IOException) {
            log.error("fail to save rules to file", e)
            throw RuntimeException(e)
        }
    }

    override fun retrieveRules(): Set<Rule> {
        return rules
    }

    override fun addOrUpdate(rule: Rule): Future<Void> {
        rules.remove(rule)
        rules.add(rule)
        persistRules()
        return Future.succeededFuture()
    }

    override fun delete(rule: Rule): Future<Void> {
        rules.remove(rule)
        persistRules()
        return Future.succeededFuture()
    }
}

