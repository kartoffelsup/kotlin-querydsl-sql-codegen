plugins {
  id("org.jetbrains.kotlin.jvm").version("1.3.10")
  id("maven-publish")
}

// If https://github.com/querydsl/querydsl/pull/2388 gets accepted
val queryDslCodegenVersion = "4.2.2-SNAPSHOT"
val queryDslVersion = "4.2.1"
val h2Version = "1.4.197"
val logbackVersion = "1.2.3"

group = "io.github.kartoffelsup"
version = "0.0.1-SNAPSHOT"

repositories {
  mavenLocal()
  mavenCentral()
}

publishing {
  repositories {
    mavenLocal()
  }
  publications {
    create<MavenPublication>("maven") {
      artifactId = rootProject.name
      println(components)
      from(components["java"])
    }
  }
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  compile("com.querydsl:querydsl-sql-codegen:$queryDslCodegenVersion")
  compile("com.querydsl:querydsl-sql:$queryDslVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("com.h2database:h2:$h2Version")
  testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
  // adding a dependency on kotlin-compiler introduces an old guava version bundled in the kotlin compiler
  // a which breaks querydsl
  // testImplementation("org.jetbrains.kotlin:kotlin-compiler")
}
