package io.github.kartoffelsup.ktqdsl

import com.mysema.codegen.model.Type
import com.querydsl.codegen.EntityType
import com.querydsl.codegen.JavaTypeMappings

object JavaForKotlinTypeMappings : JavaTypeMappings() {
  override fun getQueryType(
    type: Type, model: EntityType?, exprType: Type, raw: Boolean,
    rawParameters: Boolean, extend: Boolean
  ): Type {
    val transformedType = JavaToKotlinTypeTransformer.transform(type)
    return super.getQueryType(transformedType, model, exprType, raw, rawParameters, extend)
  }
}
