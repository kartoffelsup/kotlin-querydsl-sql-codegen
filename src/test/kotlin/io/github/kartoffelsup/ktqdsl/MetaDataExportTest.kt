package io.github.kartoffelsup.ktqdsl

import com.querydsl.sql.codegen.MetaDataExporter
import java.nio.file.Files
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MetaDataExportTest {
  private lateinit var connection: Connection
  private lateinit var metadata: DatabaseMetaData

  @BeforeTest
  fun setup() {
    Class.forName("org.h2.Driver")
    val url = "jdbc:h2:mem:testdb" + System.currentTimeMillis()
    connection = DriverManager.getConnection(url, "sa", "")
    createTables(connection)
    metadata = connection.metaData
  }

  @Test
  fun testKotlinGeneration() {
    val target = Files.createTempDirectory("ktqdsl").toFile()
    target.deleteOnExit()

    val metaDataExporter = MetaDataExporter().apply {
      setPackageName("io.github.kartoffelsup.sql")
      setTargetFolder(target)
      setBeanPackageName("io.github.kartoffelsup.bean")
      setBeansTargetFolder(target)
      setBeanSerializer(KotlinBeanSerializer())
      setCustomCodeWriter { writer -> KotlinCodeWriter(writer!!) }
      setCustomFileSuffix(".kt")
      setSerializerClass(KotlinMetaDataSerializer::class.java)
      setTypeMappings(JavaForKotlinTypeMappings)
      setSchemaPattern("PUBLIC")
    }

    metaDataExporter.export(metadata)
    val classesMethod = metaDataExporter.javaClass.declaredMethods.first { it.name == "getClasses" }
    val classes: Set<*> = classesMethod.run {
      isAccessible = true
      invoke(metaDataExporter) as Set<*>
    }
    println(classes)
    // FIXME kartoffelsup: Compile and check for errors? Compare with existing types?
    // adding a dependency on kotlin-compiler introduces an old guava version bundled in the kotlin compiler
    // which breaks querydsl
  }

  @AfterTest
  fun tearDown() {
    connection.close()
  }

  private fun createTables(connection: Connection) {
    val statement = connection.createStatement()
    statement.also {
      // reserved words
      it.execute("create table reserved (id int, `while` int)")

      it.execute("create table class (id int)")

      // underscore
      it.execute("create table underscore (e_id int, c_id int)")

      // bean generation
      it.execute("create table beangen1 (\"SEP_Order\" int)")

      // default instance clash
      it.execute("create table definstance (id int, definstance int, definstance1 int)")

      // class with pk and fk classes
      it.execute("create table pkfk (id int primary key, pk int, fk int)")

      // camel case
      it.execute("create table \"camelCase\" (id int)")
      it.execute("create table \"vwServiceName\" (id int)")

      // simple types
      it.execute("create table date_test (d date)")
      it.execute("create table date_time_test (dt datetime)")

      // complex type
      it.execute("create table survey (id int, name varchar(30))")

      // new line
      it.execute("create table \"new\nline\" (id int)")

      it.execute("create table newline2 (id int, \"new\nline\" int)")

      it.execute(
        "create table employee("
          + "id INT, "
          + "firstname VARCHAR(50), "
          + "lastname VARCHAR(50), "
          + "salary DECIMAL(10, 2), "
          + "datefield DATE, "
          + "timefield TIME, "
          + "superior_id int, "
          + "survey_id int, "
          + "survey_name varchar(30), "
          + "CONSTRAINT PK_employee PRIMARY KEY (id), "
          + "CONSTRAINT FK_superior FOREIGN KEY (superior_id) REFERENCES employee(id))"
      )

      // multi key
      it.execute("create table multikey(id INT, id2 VARCHAR, id3 INT, CONSTRAINT pk_multikey PRIMARY KEY (id, id2, id3) )")

      //  M_PRODUCT_BOM_ID
      it.execute(
        ("create table product(id int, "
          + "m_product_bom_id int, "
          + "m_productbom_id int, "
          + "constraint product_bom foreign key (m_productbom_id) references product(id))")
      )
    }
  }
}