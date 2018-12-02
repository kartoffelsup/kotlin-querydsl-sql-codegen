package io.github.kartoffelsup.ktqdsl

import com.mysema.codegen.model.Type
import com.mysema.codegen.model.TypeExtends

class KotlinTypeExtends(varName: String?, type: Type?) : TypeExtends(varName, type) {
  constructor(type: Type?) : this(null, type)

  override fun getGenericName(
    asArgType: Boolean,
    packages: MutableSet<String>?,
    classes: MutableSet<String>?
  ): String {
    val genericName = super.getGenericName(asArgType, packages, classes)
    return genericName.replace("? extends ", "out ")
  }
}
