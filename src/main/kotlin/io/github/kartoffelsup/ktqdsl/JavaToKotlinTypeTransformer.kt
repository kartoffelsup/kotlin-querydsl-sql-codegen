package io.github.kartoffelsup.ktqdsl

import com.mysema.codegen.model.SimpleType
import com.mysema.codegen.model.Type

private fun Type.asSimpleKotlinType(kotlinName: String): Type {
  return SimpleType(
    category,
    "kotlin.$kotlinName",
    "kotlin",
    kotlinName,
    isPrimitive,
    isFinal
  )
}

object JavaToKotlinTypeTransformer {
  fun transform(type: Type): Type {
    val simpleName = type.simpleName
    // TODO kartoffelsup: What about arrayType, componentType, enclosingType, parameters?
    return when (simpleName) {
      "Integer", "int" -> type.asSimpleKotlinType("Int")
      "Short", "short" -> type.asSimpleKotlinType("Short")
      "Float", "float" -> type.asSimpleKotlinType("Float")
      "Double", "double" -> type.asSimpleKotlinType("Double")
      "Byte", "byte" -> type.asSimpleKotlinType("Byte")
      "Long", "long" -> type.asSimpleKotlinType("Long")
      "Boolean", "boolean" -> type.asSimpleKotlinType("Boolean")
      "String" -> type.asSimpleKotlinType("String")
      "byte[]", "Byte[]" -> type.asSimpleKotlinType("ByteArray")
      else -> type
    }
  }
}
