package io.github.kartoffelsup.ktqdsl

import com.mysema.codegen.model.SimpleType
import com.mysema.codegen.model.Type
import com.mysema.codegen.model.TypeCategory
import com.querydsl.codegen.EntityType
import com.querydsl.codegen.JavaTypeMappings

object JavaForKotlinTypeMappings : JavaTypeMappings() {
  override fun getQueryType(
    type: Type, model: EntityType?, exprType: Type, raw: Boolean,
    rawParameters: Boolean, extend: Boolean
  ): Type {
    val simpleName = type.simpleName
    return when (simpleName) {
      "Integer", "int" -> SimpleType(exprType, SimpleType("kotlin.Int", "kotlin", "Int"))
      "Short", "short" -> SimpleType(exprType, SimpleType("kotlin.Short", "kotlin", "Short"))
      "Float", "float" -> SimpleType(exprType, SimpleType("kotlin.Float", "kotlin", "Float"))
      "Double", "double" -> SimpleType(exprType, SimpleType("kotlin.Double", "kotlin", "Double"))
      "Byte", "byte" -> SimpleType(exprType, SimpleType("kotlin.Byte", "kotlin", "Byte"))
      "Long", "long" -> SimpleType(exprType, SimpleType("kotlin.Long", "kotlin", "Long"))
      else -> super.getQueryType(type, model, exprType, raw, rawParameters, extend)
    }
  }
}
