package io.github.fstaudt.helm.tasks

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.fstaudt.helm.HelmValuesAssistantExtension
import io.github.fstaudt.helm.HelmValuesAssistantPlugin.Companion.HELM_VALUES
import io.github.fstaudt.helm.HelmValuesAssistantPlugin.Companion.SCHEMA_VERSION
import io.github.fstaudt.helm.model.Chart
import io.github.fstaudt.helm.tasks.DownloadJsonSchemas.Companion.DOWNLOADS
import io.github.fstaudt.helm.tasks.UnpackJsonSchemas.Companion.HELM_SCHEMA_FILE
import io.github.fstaudt.helm.tasks.UnpackJsonSchemas.Companion.UNPACK
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileFilter

@CacheableTask
open class AggregateJsonSchema : JsonSchemaGenerationTask() {
    companion object {
        const val AGGREGATE_JSON_SCHEMA = "aggregateJsonSchema"
        const val AGGREGATED_SCHEMA_FILE = "aggregated-values.schema.json"
    }

    @Nested
    lateinit var extension: HelmValuesAssistantExtension

    @InputFile
    @SkipWhenEmpty
    @PathSensitive(RELATIVE)
    var chartFile: File? = null

    @InputDirectory
    @PathSensitive(RELATIVE)
    val unpackSchemasDir = File(project.buildDir, "$HELM_VALUES/$UNPACK")

    @OutputFile
    val aggregatedSchemaFile: File = File(project.buildDir, "$HELM_VALUES/$AGGREGATED_SCHEMA_FILE")

    @TaskAction
    fun aggregate() {
        val chart = chartFile?.inputStream().use { yamlMapper.readValue(it, Chart::class.java) }
        val jsonSchema = chart.toAggregatedValuesJsonSchema()
        val properties = jsonSchema.objectNode("properties")
        val globalProperties = properties.objectNode("global")
        unpackSchemasDir.listFiles()?.forEach {
            if (it.isDirectory && it.containsFile(HELM_SCHEMA_FILE)) {
                val ref = "$UNPACK/${it.name}/$HELM_SCHEMA_FILE"
                properties.objectNode(it.name).put("\$ref", ref)
            }
        }
        chart.dependencies.forEach { dependency ->
            extension.repositoryMappings[dependency.repository]?.let { repository ->
                val ref = "$DOWNLOADS/${dependency.aliasOrName()}/${repository.valuesSchemaFile}"
                properties.objectNode(dependency.aliasOrName()).put("\$ref", ref)
                val globalRef = "$DOWNLOADS/${dependency.aliasOrName()}/${repository.globalValuesSchemaFile}"
                globalProperties.allOf().add(ObjectNode(nodeFactory).put("\$ref", globalRef))
            }
            dependency.condition?.toPropertiesObjectNodeIn(jsonSchema)
                ?.put("title", "Enable ${dependency.aliasOrName()} dependency (${dependency.fullName()})")
                ?.put("description", EMPTY)
                ?.put("type", "boolean")
        }
        jsonMapper.writeValue(aggregatedSchemaFile, jsonSchema)
    }

    private fun Chart.toAggregatedValuesJsonSchema(): ObjectNode {
        return ObjectNode(nodeFactory)
            .put("\$schema", SCHEMA_VERSION)
            .put("\$id", "$name/$version/$AGGREGATED_SCHEMA_FILE")
            .put("title", "Configuration for chart $name/$version")
            .put("description", EMPTY)
    }

    private fun File.containsFile(fileName: String) = listFiles(FileFilter { it.name == fileName })?.any() ?: false
}