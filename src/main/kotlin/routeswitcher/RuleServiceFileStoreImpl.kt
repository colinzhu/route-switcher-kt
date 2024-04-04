package routeswitcher

import io.vertx.core.Future
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

internal class RuleServiceFileStoreImpl : RuleService {
    private val rules: MutableSet<Rule> = HashSet()

    companion object {
        private val log = LoggerFactory.getLogger(RuleServiceFileStoreImpl::class.java)
        private const val RULES_FILE_NAME = "rules.json"
    }

    init {
        loadRules()
    }

    private fun loadRules() {
        runCatching {
            val rulesStr = Files.readString(Path.of(RULES_FILE_NAME))
            Json.decodeFromString<Array<Rule>>(rulesStr).run { rules.addAll(toSet()) }
        }.onFailure {
            log.warn("no rules.json file found or failed to load rules, use empty rules", it)
        }
    }

    private fun persistRules() {
        Files.writeString(Path.of(RULES_FILE_NAME), Json.encodeToString(rules))
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

