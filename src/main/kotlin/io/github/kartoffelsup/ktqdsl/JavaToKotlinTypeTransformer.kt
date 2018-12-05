package io.github.kartoffelsup.ktqdsl

import com.mysema.codegen.model.SimpleType
import com.mysema.codegen.model.Type

object JavaToKotlinTypeTransformer {
  fun transform(type: Type): Type {
    val simpleName = type.simpleName
    // TODO kartoffelsup: What about arrayType, componentType, enclosingType, parameters?
    // TODO kartoffelsup: Generalize
    return when (simpleName) {
      "Integer", "int" -> SimpleType(
        type.category,
        "kotlin.Int",
        "kotlin",
        "Int",
        type.isPrimitive,
        type.isFinal
      )
      "Short", "short" -> SimpleType(
        type.category,
        "kotlin.Short",
        "kotlin",
        "Short",
        type.isPrimitive,
        type.isFinal
      )
      "Float", "float" -> SimpleType(
        type.category,
        "kotlin.Float",
        "kotlin",
        "Float",
        type.isPrimitive,
        type.isFinal
      )
      "Double", "double" -> SimpleType(
        type.category,
        "kotlin.Double",
        "kotlin",
        "Double",
        type.isPrimitive,
        type.isFinal
      )
      "Byte", "byte" -> SimpleType(
        type.category,
        "kotlin.Byte",
        "kotlin",
        "Byte",
        type.isPrimitive,
        type.isFinal
      )
      "Long", "long" -> SimpleType(
        type.category,
        "kotlin.Long",
        "kotlin",
        "Long",
        type.isPrimitive,
        type.isFinal
      )
      "Boolean", "boolean" -> SimpleType(
        type.category,
        "kotlin.Boolean",
        "kotlin",
        "Boolean",
        type.isPrimitive,
        type.isFinal
      )
      "String" -> SimpleType(
        type.category,
        "kotlin.String",
        "kotlin",
        "String",
        type.isPrimitive,
        type.isFinal
      )
      "byte[]", "Byte[]" -> SimpleType(
        type.category,
        "kotlin.ByteArray",
        "kotlin",
        "ByteArray",
        type.isPrimitive,
        type.isFinal
      )
      else -> type
    }
  }
}
