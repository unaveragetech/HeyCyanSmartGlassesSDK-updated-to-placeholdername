package com.sdk.glassessdksample.feature

typealias CustomAction = (params: Map<String, Any?>) -> String

object CustomFeatureApi {
    private val actions = mutableMapOf<String, CustomAction>()

    fun registerFeature(name: String, action: CustomAction) {
        actions[name] = action
    }

    fun execute(name: String, params: Map<String, Any?> = emptyMap()): String {
        return actions[name]?.invoke(params) ?: "Unknown feature: $name"
    }

    fun listFeatures(): List<String> = actions.keys.toList()
}
