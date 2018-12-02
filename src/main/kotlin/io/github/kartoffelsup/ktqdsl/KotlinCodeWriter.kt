@file:Suppress("unused")

package io.github.kartoffelsup.ktqdsl

import com.google.common.base.Function
import com.mysema.codegen.AbstractCodeWriter
import com.mysema.codegen.CodeWriter
import com.mysema.codegen.CodegenException
import com.mysema.codegen.StringUtils
import com.mysema.codegen.Symbols
import com.mysema.codegen.model.ClassType
import com.mysema.codegen.model.Parameter
import com.mysema.codegen.model.Type
import com.mysema.codegen.model.Types
import java.io.Writer
import java.lang.reflect.InvocationTargetException
import java.util.Arrays
import java.util.HashSet
import java.util.Stack

open class KotlinCodeWriter : AbstractCodeWriter<KotlinCodeWriter> {
  private val classes = HashSet<String>()

  private val packages = HashSet<String>()
  private val types = Stack<Type>()

  constructor(appendable: Writer) : super(appendable, 2)

  constructor(appendable: Writer, spaces: Int) : super(appendable, spaces)

  override fun getRawName(type: Type): String {
    val simpleName = type.simpleName
    return when (simpleName) {
      "Integer", "int" -> "Int"
      "Short", "short" -> "Short"
      "String" -> "String"
      "Float", "float" -> "Float"
      "Double", "double" -> "Double"
      "Byte", "byte" -> "Byte"
      "Long", "long" -> "Long"
      else -> type.getRawName(this.packages, this.classes)
    }
  }

  override fun getGenericName(asArgType: Boolean, type: Type): String {
    if (type == Types.VOID) {
      return "Unit"
    }
    val simpleName = type.simpleName
    return when (simpleName) {
      "Integer", "int" -> "Int"
      "Short", "short" -> "Short"
      "String" -> "String"
      "Float", "float" -> "Float"
      "Double", "double" -> "Double"
      "Byte", "byte" -> "Byte"
      "Long", "long" -> "Long"
      else -> type.getGenericName(asArgType, packages, classes)
    }
  }

  override fun getClassConstant(className: String): String {
    return "$className::class.java"
  }

  override fun annotation(annotation: Annotation): CodeWriter {
    beginLine().append("@").appendType(annotation.javaClass)
    val methods = annotation.javaClass.declaredMethods
    if (methods.size == 1 && methods[0].name == "value") {
      try {
        val value = methods[0].invoke(annotation)
        append("(")
        annotationConstant(value)
        append(")")
      } catch (e: IllegalArgumentException) {
        throw CodegenException(e)
      } catch (e: IllegalAccessException) {
        throw CodegenException(e)
      } catch (e: InvocationTargetException) {
        throw CodegenException(e)
      }

    } else {
      var first = true
      for (method in methods) {
        try {
          val value = method.invoke(annotation)
          if (value == null || value == method.defaultValue) {
            continue
          } else if (value.javaClass.isArray && Arrays.equals(
              value as Array<*>,
              method.defaultValue as Array<*>
            )
          ) {
            continue
          } else if (!first) {
            append(Symbols.COMMA)
          } else {
            append("(")
          }
          append(method.name).append("=")
          annotationConstant(value)
        } catch (e: IllegalArgumentException) {
          throw CodegenException(e)
        } catch (e: IllegalAccessException) {
          throw CodegenException(e)
        } catch (e: InvocationTargetException) {
          throw CodegenException(e)
        }

        first = false
      }
      if (!first) {
        append(")")
      }
    }
    return nl()
  }

  override fun annotation(annotation: Class<out Annotation>): CodeWriter {
    return beginLine().append("@").appendType(annotation).nl()
  }

  fun writeDataClassWithConstructorStart(type: Type): KotlinCodeWriter {
    packages.add(type.packageName)
    beginLine("data class ", type.getGenericName(false, packages, classes), "(")
    types.push(type)
    return this
  }

  override fun beginClass(type: Type): CodeWriter {
    return beginClass(type, null)
  }

  override fun beginClass(type: Type, superClass: Type?, vararg interfaces: Type): CodeWriter {
    packages.add(type.packageName)
    beginLine("class ", type.getGenericName(false, packages, classes))
    if (superClass != null) {
      append(":").append(superClass.getGenericName(false, packages, classes))
    }
    if (interfaces.isNotEmpty()) {
      append(":")
      appendCommaSeparatedTypes(interfaces)
    }
    append(" {").nl().nl()
    goIn()
    types.push(type)
    return this
  }

  override fun <T> beginConstructor(
    parameters: Collection<T>,
    transformer: Function<T, Parameter>
  ): CodeWriter {
    types.push(types.peek())
    beginLine("constructor(").params(parameters, transformer)
      .append(" {").nl()
    return goIn()
  }

  override fun beginConstructor(vararg parameters: Parameter): CodeWriter {
    types.push(types.peek())
    beginLine("constructor").params(*parameters)
      .append(" {").nl()
    return goIn()
  }

  fun beginConstructor(superCall: String, vararg parameters: Parameter): CodeWriter {
    types.push(types.peek())
    beginLine("constructor")
      .params(*parameters)
      .append(" :")
      .append(superCall)
      .append(" {")
      .nl()
    return goIn()
  }

  override fun beginInterface(type: Type, vararg interfaces: Type): CodeWriter {
    packages.add(type.packageName)
    beginLine("interface ", type.getGenericName(false, packages, classes))
    if (interfaces.isNotEmpty()) {
      append("extends ")
      appendCommaSeparatedTypes(interfaces)
    }
    append(" {").nl().nl()
    goIn()
    types.push(type)
    return this
  }

  override fun <T> beginPublicMethod(
    type: Type, methodName: String, parameters: Collection<T>,
    transformer: Function<T, Parameter>
  ): CodeWriter {
    return beginMethod("", type, methodName, *transform(parameters, transformer))
  }

  override fun beginPublicMethod(
    type: Type,
    methodName: String,
    vararg parameters: Parameter
  ): CodeWriter {
    return beginMethod("", type, methodName, *parameters)
  }

  override fun <T> beginStaticMethod(
    type: Type, methodName: String, parameters: Collection<T>,
    transformer: Function<T, Parameter>
  ): CodeWriter {
    return beginMethod("", type, methodName, *transform(parameters, transformer))
  }

  override fun beginStaticMethod(
    type: Type,
    methodName: String,
    vararg parameters: Parameter
  ): CodeWriter {
    return beginMethod("", type, methodName, *parameters)
  }

  override fun end(): CodeWriter {
    return this.end(true)
  }

  fun end(pop: Boolean): CodeWriter {
    if (pop) {
      this.types.pop()
    }
    this.goOut()
    append('}')
      .nl()
    return this
  }

  override fun field(type: Type, name: String): CodeWriter {
    return field("var ", type, name, false)
  }

  override fun imports(vararg imports: Class<*>): CodeWriter {
    return this.imports(false, *imports)
  }

  private fun imports(wildcard: Boolean, vararg imports: Class<*>): CodeWriter {
    for (cl in imports) {
      this.classes.add(cl.name)
      this.line("import ", cl.name, if(wildcard) ".*" else "")
    }
    this.nl()
    return this
  }

  override fun imports(vararg imports: Package): CodeWriter {
    for (p in imports) {
      this.packages.add(p.name)
      this.line("import ", p.name, ".*")
    }

    this.nl()
    return this
  }

  override fun importClasses(vararg imports: String): CodeWriter {
    for (cl in imports) {
      this.classes.add(cl)
      this.line("import ", cl)
    }
    this.nl()
    return this
  }

  override fun importPackages(vararg imports: String): CodeWriter {
    for (p in imports) {
      this.packages.add(p)
      this.line("import ", p, ".*;")
    }
    this.nl()
    return this
  }

  override fun javadoc(vararg strings: String): CodeWriter {
    return this
  }

  override fun packageDecl(packageName: String): CodeWriter {
    packages.add(packageName)
    return line("package ", packageName).nl()
  }

  override fun privateField(type: Type, name: String): CodeWriter {
    return field("private var", type, name, false)
  }

  override fun privateFinal(type: Type, name: String): CodeWriter {
    return field("private val", type, name, false)
  }

  override fun privateFinal(type: Type, name: String, value: String): CodeWriter {
    return field("private val", type, name, value, false)
  }

  override fun privateStaticFinal(type: Type, name: String, value: String): CodeWriter {
    return field("private val", type, name, value, false)
  }

  override fun protectedField(type: Type, name: String): CodeWriter {
    return field("protected var", type, name, false)
  }

  override fun protectedFinal(type: Type, name: String): CodeWriter {
    return field("protected val", type, name, false)
  }

  override fun protectedFinal(type: Type, name: String, value: String): CodeWriter {
    return field("protected val", type, name, value, false)
  }

  override fun publicField(type: Type, name: String): CodeWriter {
    return field("var", type, name, false)
  }

  override fun publicField(type: Type, name: String, value: String): CodeWriter {
    return field("var", type, name, value, false)
  }

  fun publicField(type: Type, name: String, nullable: Boolean): CodeWriter {
    return field("var", type, name, nullable)
  }

  override fun publicFinal(type: Type, name: String): CodeWriter {
    return field("val", type, name, false)
  }

  override fun publicFinal(type: Type, name: String, value: String): CodeWriter {
    return field("val", type, name, value, false)
  }

  override fun publicStaticFinal(type: Type, name: String, value: String): CodeWriter {
    return field("val", type, name, value, false)
  }

  override fun staticimports(vararg classes: Class<*>): CodeWriter {
    return imports(true, *classes)
  }

  override fun suppressWarnings(s: String): CodeWriter {
    return this
  }

  override fun suppressWarnings(vararg strings: String): CodeWriter {
    return this
  }

  fun beginCompanionObject() {
    append("companion object {").nl()
    goIn()
  }

  private fun annotationConstant(value: Any?) {
    if (value?.javaClass?.isArray == true) {
      append("{")
      var first = true
      for (o in value as Array<*>) {
        if (!first) {
          append(", ")
        }
        annotationConstant(o)
        first = false
      }
      append("}")
    } else if (value is Class<*>) {
      appendType(value).append("::class.java")
    } else if (value is Number || value is Boolean) {
      append(value.toString())
    } else if (value is Enum<*>) {
      if (classes.contains(value.javaClass.name) || packages.contains(value.javaClass.getPackage().name)) {
        append(value.name)
      } else {
        append(value.javaClass.declaringClass.name).append(Symbols.DOT)
          .append(value.name)
      }
    } else if (value is String) {
      val escaped = StringUtils.escapeJava(value.toString())
      append(Symbols.QUOTE).append(escaped.replace("\\/", "/")).append(Symbols.QUOTE)
    } else {
      throw IllegalArgumentException("Unsupported annotation value : $value")
    }
  }

  private fun appendType(type: Class<*>): KotlinCodeWriter {
    val classType = ClassType(type)
    if (classes.contains(type.name) || packages.contains(type.getPackage().name)) {
      append(getGenericName(false, classType))
    } else {
      append(getGenericName(false, classType))
    }
    return this
  }

  private fun <T> params(
    parameters: Collection<T>,
    transformer: Function<T, Parameter>
  ): KotlinCodeWriter {
    append("(")
    var first = true
    for (param in parameters) {
      if (!first) {
        append(Symbols.COMMA)
      }
      param(transformer.apply(param)!!)
      first = false
    }
    append(")")
    return this
  }

  private fun params(vararg params: Parameter?): KotlinCodeWriter {
    append("(")
    for (i in params.indices) {
      if (i > 0) {
        append(Symbols.COMMA)
      }
      params[i]?.let { param(it) }
    }
    append(")")
    return this
  }

  private fun param(parameter: Parameter) {
    append(parameter.name)
    append(" : ")
    append(getGenericName(true, parameter.type))
  }

  private fun appendCommaSeparatedTypes(types: Array<out Type>) {
    for (i in types.indices) {
      if (i > 0) {
        append(Symbols.COMMA)
      }
      append(getGenericName(false, types[i]))
    }
  }

  private fun beginMethod(
    modifiers: String, returnType: Type, methodName: String,
    vararg args: Parameter?
  ): KotlinCodeWriter {
    types.push(types.peek())
    beginLine(modifiers, "fun ", methodName).params(*args)
      .append(" : ").append(getGenericName(true, returnType))
      .append(" {")
      .nl()
    return goIn()
  }

  private fun <T> transform(
    parameters: Collection<T>,
    transformer: Function<T, Parameter>
  ): Array<Parameter?> {
    val rv = arrayOfNulls<Parameter>(parameters.size)
    var i = 0
    for (value in parameters) {
      rv[i++] = transformer.apply(value)
    }
    return rv
  }

  private fun field(
    modifier: String,
    type: Type,
    name: String,
    nullable: Boolean
  ): KotlinCodeWriter {
    return this.append(modifier)
      .append(" ")
      .append(name)
      .append(" : ")
      .append(getGenericName(false, type))
      .append(if (nullable) "?" else "")
  }

  private fun field(
    modifier: String, type: Type, name: String, value: String,
    nullable: Boolean
  ): KotlinCodeWriter {
    return this.line(
      modifier,
      " ",
      name,
      " : ",
      getGenericName(true, type),
      if (nullable) "?" else "",
      " = ",
      value
    )
  }
}
