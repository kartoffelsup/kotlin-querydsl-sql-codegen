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
  private var addFullConstructor: Boolean = false

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
    val importedClasses = getAnnotationTypes(model)
    //    for (Type iface : interfaces) {
    //      importedClasses.add(iface.getFullName());
    //    }
    importedClasses.add(Generated::class.java.name)
    if (model.hasLists()) {
      importedClasses.add(List::class.java.name)
    }
    if (model.hasCollections()) {
      importedClasses.add(Collection::class.java.name)
    }
    if (model.hasSets()) {
      importedClasses.add(Set::class.java.name)
    }
    if (model.hasMaps()) {
      importedClasses.add(Map::class.java.name)
    }
    if (model.hasArrays()) {
      importedClasses.add(Arrays::class.java.name)
    }
    writer.importClasses(*importedClasses.toTypedArray())

    // header
    for (annotation in model.annotations) {
      writer.annotation(annotation)
    }

    writer.line("@Generated(\"", javaClass.name, "\")")

    //    if (!interfaces.isEmpty()) {
    //      Type superType = null;
    //      if (printSupertype && model.getSuperType() != null) {
    //        superType = model.getSuperType().getType();
    //      }
    //      Type[] ifaces = interfaces.toArray(new Type[interfaces.size()]);
    //      writer.beginClass(model, superType, ifaces);
    //    } else

    writer.writeDataClassWithConstructorStart(model)
    //
    //    bodyStart(model, writer);
    //
    //    if (addFullConstructor) {
    //      addFullConstructor(model, writer);
    //    }

    // fields
    val properties = ArrayList(model.properties)
    for (i in properties.indices) {
      val property = properties[i]
      val column = property.data["COLUMN"]
      val nullable: Boolean
      if (column is ColumnMetadata) {
        nullable = column.isNullable
      } else {
        nullable = true
      }
      writer.publicField(property.type, property.escapedName, nullable)
      if (i < properties.size - 1) {
        writer.append(",").nl()
      }
    }
    writer.append(")")
  }

  protected fun addFullConstructor(model: EntityType, writer: CodeWriter) {
    // public empty constructor
    writer.beginConstructor()
    writer.end()

    // full constructor
    writer.beginConstructor(model.properties, propertyToParameter)
    for (property in model.properties) {
      writer.line("this.", property.escapedName, " = ", property.escapedName, ";")
    }
    writer.end()
  }

  fun setAddFullConstructor(addFullConstructor: Boolean) {
    this.addFullConstructor = addFullConstructor
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
