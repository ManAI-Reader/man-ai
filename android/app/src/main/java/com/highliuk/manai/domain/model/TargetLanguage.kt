package com.highliuk.manai.domain.model

enum class TargetLanguage(val code: String, val displayName: String) {
    EN("EN", "English"),
    IT("IT", "Italiano"),
    ES("ES", "Español"),
    PT_BR("PT-BR", "Português (Brasil)"),
    FR("FR", "Français"),
    DE("DE", "Deutsch"),
    ZH("ZH", "中文"),
    KO("KO", "한국어"),
    RU("RU", "Русский"),
    PL("PL", "Polski");

    companion object {
        fun fromCode(code: String): TargetLanguage =
            entries.find { it.code == code } ?: EN
    }
}
