package org.jmailen.gradle.kotlinter.support

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.EditorConfigOverride
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import java.io.File

internal fun editorConfigOverride(ktLintParams: KtLintParams): EditorConfigOverride {
    val rules = ktLintParams.disabledRules

    return if (rules.isEmpty()) {
        EditorConfigOverride.emptyEditorConfigOverride
    } else {
        EditorConfigOverride.from(DefaultEditorConfigProperties.disabledRulesProperty to rules.joinToString(separator = ","))
    }
}

internal fun resetEditorconfigCacheIfNeeded(
    wasEditorConfigChanged: Property<Boolean>,
    logger: Logger,
) {
    if (wasEditorConfigChanged.get()) {
        logger.info("Editorconfig changed, resetting KtLint caches")
        KtLint.trimMemory() // Calling trimMemory() will also reset internal loaded `.editorconfig` cache
    }
}

internal fun ProjectLayout.findApplicableEditorConfigFiles(): Sequence<File> {
    val projectEditorConfig = projectDirectory.file(".editorconfig").asFile

    return generateSequence(seed = projectEditorConfig) { editorconfig ->
        if (editorconfig.isRootEditorConfig) {
            null
        } else {
            editorconfig.parentFile?.parentFile?.resolve(".editorconfig")
        }
    }
}

private val File.isRootEditorConfig: Boolean
    get() {
        if (!isFile || !canRead()) return false

        return useLines { lines ->
            lines.any { line -> line.matches(editorConfigRootRegex) }
        }
    }

/**
 * According to https://editorconfig.org/ root-most EditorConfig file contains line with `root=true`
 */
private val editorConfigRootRegex = "^root\\s?=\\s?true".toRegex()
