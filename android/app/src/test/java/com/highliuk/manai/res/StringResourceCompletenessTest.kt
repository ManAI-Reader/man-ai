package com.highliuk.manai.res

import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringResourceCompletenessTest {

    private val resDir = File("src/main/res")
    private val defaultStringsFile = File(resDir, "values/strings.xml")

    @Test
    fun allStringResourcesAreTranslatedInEveryLocale() {
        val defaultKeys = parseResourceKeys(defaultStringsFile)
        val localeDirs = resDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("values-") && File(file, "strings.xml").exists()
        } ?: emptyArray()

        check(localeDirs.isNotEmpty()) { "No locale directories found under $resDir" }

        val missing = mutableMapOf<String, List<String>>()

        for (localeDir in localeDirs) {
            val localeFile = File(localeDir, "strings.xml")
            val localeKeys = parseResourceKeys(localeFile)
            val missingKeys = defaultKeys - localeKeys
            if (missingKeys.isNotEmpty()) {
                missing[localeDir.name] = missingKeys.sorted()
            }
        }

        if (missing.isNotEmpty()) {
            val report = missing.entries.joinToString("\n") { (locale, keys) ->
                "  $locale is missing: ${keys.joinToString(", ")}"
            }
            fail("Translation gaps found:\n$report")
        }
    }

    private fun parseResourceKeys(file: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val keys = mutableSetOf<String>()

        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            strings.item(i).attributes.getNamedItem("name")?.nodeValue?.let { keys.add(it) }
        }

        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            plurals.item(i).attributes.getNamedItem("name")?.nodeValue?.let { keys.add(it) }
        }

        return keys
    }
}
