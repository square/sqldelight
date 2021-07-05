package com.squareup.sqldelight.tests

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class GenerateSchemaTest {
  @Test fun `schema file generates correctly`() {
    val fixtureRoot = File("src/test/schema-file")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .setDebug(true) // Run in-process.
      .build()

    // verify
    assertThat(schemaFile.exists())
      .isTrue()

    schemaFile.delete()
  }

  @Test fun `generateSchema task can run twice`() {
    val fixtureRoot = File("src/test/schema-file")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .setDebug(true) // Run in-process.
      .build()

    // verify
    assertThat(schemaFile.exists())
      .isTrue()
    val lastModified = schemaFile.lastModified()

    while (System.currentTimeMillis() - lastModified <= 1000) {
      // last modified only updates per second.
      Thread.yield()
    }

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "--rerun-tasks", "generateMainDatabaseSchema", "--stacktrace")
      .setDebug(true) // Run in-process.
      .build()

    // verify
    assertThat(schemaFile.exists()).isTrue()
    assertThat(schemaFile.lastModified()).isNotEqualTo(lastModified)

    schemaFile.delete()
  }

  @Test fun `schema file generates correctly with existing sqm files`() {
    val fixtureRoot = File("src/test/schema-file-sqm")

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .setDebug(true) // Run in-process.
      .build()

    // verify
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/3.db")
    assertThat(schemaFile.exists())
      .isTrue()

    schemaFile.delete()
  }

  @Test fun `schema file generates correctly for android`() {
    val fixtureRoot = File("src/test/schema-file-android")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "generateDebugDatabaseSchema", "--stacktrace")
      .setDebug(true) // Run in-process.
      .build()

    // verify
    assertThat(schemaFile.exists()).isTrue()
  }
}
