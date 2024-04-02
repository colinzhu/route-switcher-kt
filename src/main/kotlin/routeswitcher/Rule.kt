package routeswitcher

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Rule(
    val uriPrefix: String,
    val fromIP: String,
    val targetOptions: String,
    val target: String,
    val updateBy: String,
    val updateTime: Long,
    val remark: String? = null
) {
    constructor(): this("", "", "", "", "", 0, null)
    override fun equals(other: Any?): Boolean {
        return other is Rule && uriPrefix == other.uriPrefix && fromIP == other.fromIP
    }

    override fun hashCode(): Int {
        return (uriPrefix + fromIP).hashCode()
    }
}