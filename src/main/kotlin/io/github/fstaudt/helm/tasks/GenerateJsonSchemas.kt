package io.github.fstaudt.helm.tasks

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.fstaudt.helm.HelmValuesAssistantExtension
import io.github.fstaudt.helm.HelmValuesAssistantPlugin.Companion.HELM_VALUES
import io.github.fstaudt.helm.HelmValuesAssistantPlugin.Companion.SCHEMA_VERSION
import io.github.fstaudt.helm.model.Chart
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

@CacheableTask
@Suppress("NestedLambdaShadowedImplicitParameter")
open class GenerateJsonSchemas : JsonSchemaGenerationTask() {
    companion object {
        const val GENERATE_JSON_SCHEMAS = "generateJsonSchemas"
        const val GENERATED = "$HELM_VALUES/generated"
    }

    @Nested
    lateinit var extension: HelmValuesAssistantExtension

    @InputFile
    @SkipWhenEmpty
    @PathSensitive(RELATIVE)
    var chartFile: File? = null

    @OutputDirectory
    val generatedSchemaDir = File(project.buildDir, GENERATED)

    @TaskAction
    fun generate() {
        val chart = chartFile?.inputStream().use { yamlMapper.readValue(it, Chart::class.java) }
        generateValuesSchemaFile(chart)
        generateGlobalValuesSchemaFile(chart)
    }

    private fun generateValuesSchemaFile(chart: Chart) {
        val repository = extension.publicationRepository()
        val jsonSchema = chart.toValuesJsonSchema()
        jsonSchema.objectNode("properties").objectNode("global").put("\$ref", repository.globalValuesSchemaFile)
        chart.dependencies.forEach { dependency ->
            extension.repositoryMappings[dependency.repository]?.let {
                val ref =
                    "${it.baseUri}/${dependency.name}/${dependency.version}/${it.valuesSchemaFile}".toRelativeUri()
                jsonSchema.objectNode("properties").objectNode(dependency.aliasOrName()).put("\$ref", ref)
                dependency.condition?.toPropertiesObjectNodeIn(jsonSchema)
                    ?.put("title", "Enable ${dependency.aliasOrName()} dependency (${dependency.fullName()})")
                    ?.put("description", EMPTY)
                    ?.put("type", "boolean")
            }
        }
        jsonMapper.writeValue(File(generatedSchemaDir, repository.valuesSchemaFile), jsonSchema)
    }

    private fun generateGlobalValuesSchemaFile(chart: Chart) {
        val repository = extension.publicationRepository()
        val jsonSchema = chart.toGlobalValuesJsonSchema()
        jsonSchema.allOf().let { allOf ->
            chart.dependencies.forEach { dependency ->
                extension.repositoryMappings[dependency.repository]?.let {
                    val ref = "${it.baseUri}/${dependency.name}/${dependency.version}/${it.globalValuesSchemaFile}"
                        .toRelativeUri()
                    allOf.add(ObjectNode(nodeFactory).put("\$ref", ref))
                }
            }
        }
        jsonMapper.writeValue(File(generatedSchemaDir, repository.globalValuesSchemaFile), jsonSchema)
    }

    private fun Chart.toValuesJsonSchema(): ObjectNode {
        val repository = extension.publicationRepository()
        val version = extension.publishedVersion ?: version
        return ObjectNode(nodeFactory)
            .put("\$schema", SCHEMA_VERSION)
            .put("\$id", "${repository.baseUri}/$name/$version/${repository.valuesSchemaFile}")
            .put("title", "Configuration for chart ${extension.publicationRepository}/$name/$version")
            .put("description", EMPTY)
    }

    private fun Chart.toGlobalValuesJsonSchema(): ObjectNode {
        val repository = extension.publicationRepository()
        val version = extension.publishedVersion ?: version
        return ObjectNode(nodeFactory)
            .put("\$schema", SCHEMA_VERSION)
            .put("\$id", "${repository.baseUri}/$name/$version/${repository.globalValuesSchemaFile}")
            .put("title", "Configuration of global values for chart ${extension.publicationRepository}/$name/$version")
            .put("description", EMPTY)
    }

    private fun String.toRelativeUri(): String {
        return extension.publicationRepository().let {
            val uri = URI(this)
            val publicationUri = URI(it.baseUri)
            when {
                uri.host == publicationUri.host && uri.path.startsWith(publicationUri.path) ->
                    uri.path.replace(publicationUri.path, "../..")
                uri.host == publicationUri.host ->
                    "../..${"/..".repeat(publicationUri.path.count { it == '/' })}${uri.path}"
                else -> this
            }
        }
    }
}
