@file:Suppress("unused")

package io.github.kartoffelsup.ktqdsl

import com.google.common.base.Function
import com.mysema.codegen.CodeWriter
import com.mysema.codegen.model.Parameter
import com.querydsl.codegen.EntityType
import com.querydsl.codegen.Property
import com.querydsl.codegen.Serializer
import com.querydsl.codegen.SerializerConfig
import com.querydsl.sql.ColumnMetadata
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import javax.annotation.Generated

open class KotlinBeanSerializer : Serializer {

  private val addToString: Boolean = false

  override fun serialize(
    model: EntityType,
    serializerConfig: SerializerConfig,
    writer: CodeWriter
  ) {
    if (writer !is KotlinCodeWriter) {
      throw IllegalArgumentException("A KotlinCodeWriter is required for KotlinBeanSerializer")
    }
    // package
    if (!model.packageName.isEmpty()) {
      writer.packageDecl(model.packageName)
    }

    // imports
    val classesToImport = buildImports(model)
    writer.importClasses(*classesToImport.toTypedArray())

    // header
    for (annotation in model.annotations) {
      writer.annotation(annotation)
    }

    writer.line("@Generated(\"", javaClass.name, "\")")
    writer.writeDataClassWithConstructorStart(model)

    // fields
    val properties = ArrayList(model.properties)
    properties.forEachIndexed { index, property ->
      val column = property.data["COLUMN"]
      val nullable = if (column is ColumnMetadata) {
        column.isNullable
      } else {
        true
      }

      val suffixWithComma = index < properties.size - 1
      writer.publicField(property.type, property.escapedName, nullable, suffixWithComma)
    }
    writer.append(")")
  }

  private fun buildImports(model: EntityType): MutableSet<String> {
    val classesToImport = getAnnotationTypes(model)

    classesToImport.add(Generated::class.java.name)
    if (model.hasLists()) {
      classesToImport.add(List::class.java.name)
    }
    if (model.hasCollections()) {
      classesToImport.add(Collection::class.java.name)
    }
    if (model.hasSets()) {
      classesToImport.add(Set::class.java.name)
    }
    if (model.hasMaps()) {
      classesToImport.add(Map::class.java.name)
    }
    if (model.hasArrays()) {
      classesToImport.add(Arrays::class.java.name)
    }
    return classesToImport
  }

  private fun getAnnotationTypes(model: EntityType): MutableSet<String> {
    val imports = HashSet<String>()
    for (annotation in model.annotations) {
      imports.add(annotation.javaClass.name)
    }
    return imports
  }

  companion object {
    private val propertyToParameter = Function<Property, Parameter> { p: Property? ->
      Parameter(p?.name, p?.type)
    }
  }
}
