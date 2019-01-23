@file:Suppress("unused")

package io.github.kartoffelsup.ktqdsl

import com.mysema.codegen.CodeWriter
import com.mysema.codegen.Symbols.COMMA
import com.mysema.codegen.Symbols.EMPTY
import com.mysema.codegen.Symbols.SUPER
import com.mysema.codegen.Symbols.THIS
import com.mysema.codegen.model.ClassType
import com.mysema.codegen.model.Parameter
import com.mysema.codegen.model.SimpleType
import com.mysema.codegen.model.TypeCategory
import com.mysema.codegen.model.Types
import com.querydsl.codegen.EntityType
import com.querydsl.codegen.Property
import com.querydsl.codegen.SerializerConfig
import com.querydsl.codegen.TypeMappings
import com.querydsl.core.types.Path
import com.querydsl.core.types.PathMetadata
import com.querydsl.core.types.dsl.PathInits
import com.querydsl.sql.codegen.MetaDataSerializer
import com.querydsl.sql.codegen.NamingStrategy
import com.querydsl.sql.codegen.SQLCodegenModule
import java.util.Comparator
import javax.inject.Inject
import javax.inject.Named

open class KotlinMetaDataSerializer
/**
 * Create a new `KotlinMetaDataSerializer` instance
 *
 * @param namingStrategy      naming strategy for table to class and column to property conversion
 * @param innerClassesForKeys wrap key properties into inner classes (default: false)
 * @param imports             java user imports
 */
@Inject
constructor(
  typeMappings: TypeMappings,
  private val namingStrategy: NamingStrategy,
  @Named(SQLCodegenModule.INNER_CLASSES_FOR_KEYS) innerClassesForKeys: Boolean,
  @Named(SQLCodegenModule.IMPORTS) imports: Set<String>,
  @Named(SQLCodegenModule.COLUMN_COMPARATOR) columnComparator: Comparator<Property>?,
  @Named(SQLCodegenModule.ENTITYPATH_TYPE) entityPathType: Class<*>
) : MetaDataSerializer(
  typeMappings,
  namingStrategy,
  innerClassesForKeys,
  imports,
  columnComparator,
  entityPathType
) {

  override fun introDefaultInstance(
    writer: CodeWriter,
    entityType: EntityType,
    defaultName: String
  ) {
    val kotlinCodeWriter = checkWriter(writer)
    val variableName = if (!defaultName.isEmpty()) {
      defaultName
    } else {
      namingStrategy.getDefaultVariableName(entityType)
    }

    val alias = namingStrategy.getDefaultAlias(entityType)
    val queryType = typeMappings.getPathType(entityType, entityType, true)
    kotlinCodeWriter.beginCompanionObject()
    kotlinCodeWriter.publicStaticFinal(
      queryType,
      variableName,
      queryType.simpleName + "(\"" + alias + "\")"
    )
    kotlinCodeWriter.end()
  }

  override fun constructorsForVariables(writer: CodeWriter, model: EntityType) {
    val kotlinCodeWriter = checkWriter(writer)

    val localName = writer.getRawName(model)
    val genericName = writer.getGenericName(true, model)

    if (localName != genericName) {
      writer.suppressWarnings("all")
    }

    val hasEntityFields = model.hasEntityFields()
    val constructorExtends = constructorExtends(kotlinCodeWriter, localName, genericName, model)
    kotlinCodeWriter.beginConstructor(constructorExtends, Parameter("variable", Types.STRING))
    if (!hasEntityFields) {
      constructorContent(writer, model)
    }
    writer.end()

    val superCall = (SUPER + "(" + writer.getClassConstant(localName) + COMMA
      + "forVariable(variable), schema, table)")
    kotlinCodeWriter.beginConstructor(
      superCall, Parameter("variable", Types.STRING),
      Parameter("schema", Types.STRING),
      Parameter("table", Types.STRING)
    )
    constructorContent(writer, model)
    writer.end()

    val superCall2 = (SUPER + "(" + writer.getClassConstant(localName) + COMMA
      + "forVariable(variable), schema, \"" + model.data["table"].toString() + "\")")
    kotlinCodeWriter.beginConstructor(
      superCall2, Parameter("variable", Types.STRING),
      Parameter("schema", Types.STRING)
    )
    constructorContent(writer, model)
    writer.end()
  }

  override fun constructors(
    model: EntityType, config: SerializerConfig,
    writer: CodeWriter
  ) {
    val kotlinCodeWriter = checkWriter(writer)
    val localName = writer.getRawName(model)
    val genericName = writer.getGenericName(true, model)

    val hasEntityFields = model.hasEntityFields()
    val stringOrBoolean =
      model.originalCategory == TypeCategory.STRING || model.originalCategory == TypeCategory.BOOLEAN
    val thisOrSuper = if (hasEntityFields) THIS else SUPER
    val additionalParams = getAdditionalConstructorParameter(model)
    val classCast = if (localName == genericName) EMPTY else "(Class) "

    // String
    constructorsForVariables(writer, model)

    // Path
    pathConstructor(
      model,
      kotlinCodeWriter,
      localName,
      genericName,
      hasEntityFields,
      stringOrBoolean,
      additionalParams,
      classCast
    )

    // PathMetadata
    pathMetadataConstructor(
      model,
      kotlinCodeWriter,
      localName,
      genericName,
      hasEntityFields,
      stringOrBoolean,
      additionalParams,
      classCast
    )

    // PathMetadata, PathInits
    pathInitMetaConstructor(
      model,
      config,
      kotlinCodeWriter,
      localName,
      genericName,
      hasEntityFields,
      thisOrSuper,
      additionalParams,
      classCast
    )
  }

  private fun pathInitMetaConstructor(
    model: EntityType, config: SerializerConfig, writer: KotlinCodeWriter,
    localName: String, genericName: String,
    hasEntityFields: Boolean, thisOrSuper: String,
    additionalParams: String, classCast: String
  ) {
    if (hasEntityFields) {
      if (localName != genericName) {
        writer.suppressWarnings("all", "rawtypes", "unchecked")
      }
      val extendsConstructor =
        thisOrSuper + "(" + classCast + writer.getClassConstant(localName) + COMMA + "metadata, inits" + additionalParams + ")"
      writer.beginConstructor(extendsConstructor, PATH_METADATA, PATH_INITS)
      if (!hasEntityFields) {
        constructorContent(writer, model)
      }
      writer.end()
    }

    // Class, PathMetadata, PathInits
    if (hasEntityFields) {
      val type = ClassType(Class::class.java, KotlinTypeExtends(model))
      val extendsConstructor = "super(type, metadata, inits$additionalParams)"
      writer
        .beginConstructor(extendsConstructor, Parameter("type", type), PATH_METADATA, PATH_INITS)
      initEntityFields(writer, config, model)
      constructorContent(writer, model)
      writer.end()
    }
  }

  private fun pathMetadataConstructor(
    model: EntityType, writer: KotlinCodeWriter, localName: String,
    genericName: String, hasEntityFields: Boolean,
    stringOrBoolean: Boolean, additionalParams: String,
    classCast: String
  ) {
    if (hasEntityFields) {
      val extendsConstructor = "this(metadata, PathInits.getFor(metadata, INITS))"
      writer.beginConstructor(extendsConstructor, PATH_METADATA)
      writer.end()
    } else {
      if (localName != genericName) {
        writer.suppressWarnings("all", "rawtypes", "unchecked")
      }
      val extendsConstructor: String = if (stringOrBoolean) {
        "super(metadata)"
      } else {
        "super(" + classCast + writer.getClassConstant(localName) + COMMA +
          "metadata" + additionalParams + ")"
      }
      writer.beginConstructor(extendsConstructor, PATH_METADATA)
      constructorContent(writer, model)
      writer.end()
    }
  }

  private fun pathConstructor(
    model: EntityType, writer: KotlinCodeWriter, localName: String,
    genericName: String, hasEntityFields: Boolean, stringOrBoolean: Boolean,
    additionalParams: String, classCast: String
  ) {
    if (localName != genericName) {
      writer.suppressWarnings("all", "rawtypes", "unchecked")
    }
    val extendsConstructor: String = if (!hasEntityFields) {
      if (stringOrBoolean) {
        "super(path.getMetadata())"
      } else {
        "super(${classCast}path.getType(), path.getMetadata()$additionalParams)"
      }
    } else {
      "this(${classCast}path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS))"
    }

    val simpleModel = SimpleType(model)
    if (model.isFinal) {
      val type = ClassType(Path::class.java, simpleModel)
      writer.beginConstructor(extendsConstructor, Parameter("path", type))
    } else {
      val type = ClassType(Path::class.java, KotlinTypeExtends(simpleModel))
      writer.beginConstructor(extendsConstructor, Parameter("path", type))
    }
    if (!hasEntityFields) {
      constructorContent(writer, model)
    }
    writer.end()
  }

  private fun constructorExtends(
    kotlinCodeWriter: KotlinCodeWriter, localName: String,
    genericName: String, model: EntityType
  ): String {
    val stringOrBoolean =
      model.originalCategory == TypeCategory.STRING || model.originalCategory == TypeCategory.BOOLEAN
    val hasEntityFields = model.hasEntityFields()
    val thisOrSuper = if (hasEntityFields) THIS else SUPER
    val additionalParams = if (hasEntityFields) "" else getAdditionalConstructorParameter(model)

    return if (stringOrBoolean) {
      "$thisOrSuper(forVariable(variable)$additionalParams)"
    } else {
      thisOrSuper +
        "(" + (if (localName == genericName) EMPTY else "(Class) ") +
        kotlinCodeWriter.getClassConstant(localName) + COMMA + "forVariable(variable)" +
        (if (hasEntityFields) ", INITS" else EMPTY) +
        additionalParams +
        ")"
    }
  }

  private fun checkWriter(writer: CodeWriter): KotlinCodeWriter {
    check(
      writer is KotlinCodeWriter
    ) {
      "A ${KotlinCodeWriter::class.java} is required for ${KotlinMetaDataSerializer::class.java}"
    }
    return writer
  }

  companion object {
    private val PATH_METADATA = Parameter(
      "metadata", ClassType(
        PathMetadata::class.java
      )
    )

    private val PATH_INITS = Parameter("inits", ClassType(PathInits::class.java))
  }
}
