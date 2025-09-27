package com.chieftain.game.models.data

class AgentMemory {
    var memories: Map<MemoryType, Map<Int, String>> = mapOf()

    companion object {
        enum class MemoryType {
            LOCATION,
            PRICE,
            AGENT
        }
    }
}