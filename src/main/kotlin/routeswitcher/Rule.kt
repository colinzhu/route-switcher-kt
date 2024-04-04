package routeswitcher

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.serialization.Serializable

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Rule(
    val uriPrefix: String,
    val fromIP: String,
    val targetOptions: String,
    val target: String,
    val updateBy: String,
    val updateTime: Long,
    val remark: String? = null
) {
    constructor(): this("", "", "", "", "", 0, null) // for jackson
    override fun equals(other: Any?): Boolean {
        return other is Rule && uriPrefix == other.uriPrefix && fromIP == other.fromIP
    }

    override fun hashCode(): Int {
        return (uriPrefix + fromIP).hashCode()
    }
}