package com.example.childguard.domain

/** Чистый Kotlin, без Android-зависимостей — юнит-тестируется отдельно. */
class ThreatAnalyzer(
    private val rules: List<ThreatRule> = DefaultRules.all
) {

    enum class ThreatCategory(val displayName: String) {
        FRAUD("Мошенничество"),
        BULLYING("Буллинг/Угрозы"),
        OTHER("Прочее")
    }

    data class Verdict(
        val isThreat: Boolean,
        val category: ThreatCategory = ThreatCategory.OTHER,
        val matchedKeyword: String? = null
    )

    data class ThreatRule(val keyword: String, val category: ThreatCategory)

    object DefaultRules {
        val all: List<ThreatRule> = listOf(
            ThreatRule("переведи деньги", ThreatCategory.FRAUD),
            ThreatRule("переведите деньги", ThreatCategory.FRAUD),
            ThreatRule("код из смс", ThreatCategory.FRAUD),
            ThreatRule("код подтверждения", ThreatCategory.FRAUD),
            ThreatRule("арест счета", ThreatCategory.FRAUD),
            ThreatRule("заблокирован счет", ThreatCategory.FRAUD),
            ThreatRule("срочно переведи", ThreatCategory.FRAUD),
            ThreatRule("получите выплату", ThreatCategory.FRAUD),
            ThreatRule("ты никто", ThreatCategory.BULLYING),
            ThreatRule("убью тебя", ThreatCategory.BULLYING),
            ThreatRule("убей себя", ThreatCategory.BULLYING),
            ThreatRule("все узнают", ThreatCategory.BULLYING),
            ThreatRule("слив фото", ThreatCategory.BULLYING),
        )
    }

    fun analyze(text: String): Verdict {
        if (text.isBlank()) return Verdict(isThreat = false)

        // Нормализация: lowercase + снос пробелов/пунктуации между буквами
        // ("у б ь ю" -> "убью").
        val normalized = text.lowercase()
            .replace(Regex("[\\s\\p{Punct}]+"), " ")
            .replace(Regex("(?<=\\p{L}) (?=\\p{L})"), "")

        rules.forEach { rule ->
            if (normalized.contains(rule.keyword.lowercase())) {
                return Verdict(true, rule.category, rule.keyword)
            }
        }
        return Verdict(isThreat = false)
    }
}
