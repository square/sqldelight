package com.squareup.sqldelight.driver.test

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.internal.Atomic
import com.squareup.sqldelight.internal.getValue
import com.squareup.sqldelight.internal.setValue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class DriverTest {
  protected lateinit var driver: SqlDriver
  protected val schema = object : Schema {
    override val version: Int = 1

    override fun create(driver: SqlDriver) {
      driver.execute(
        0,
        """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(),
        0
      )
      driver.execute(
        1,
        """
              |CREATE TABLE nullability_test (
              |  id INTEGER PRIMARY KEY,
              |  integer_value INTEGER,
              |  text_value TEXT,
              |  blob_value BLOB,
              |  real_value REAL
              |);
            """.trimMargin(),
        0
      )
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Int,
      newVersion: Int
    ) {
      // No-op.
    }
  }
  private var transacter by Atomic<Transacter?>(null)

  abstract fun setupDatabase(schema: Schema): SqlDriver

  private fun changes(): Long? {
    // wrap in a transaction to ensure read happens on transaction thread/connection
    return transacter!!.transactionWithResult {
      val mapper: (SqlCursor) -> Long? = {
        it.next()
        it.getLong(0)
      }
      driver.executeQuery(null, "SELECT changes()", mapper, 0)
    }
  }

  @BeforeTest fun setup() {
    driver = setupDatabase(schema = schema)
    transacter = object : TransacterImpl(driver) {}
  }

  @AfterTest fun tearDown() {
    transacter = null
    driver.close()
  }

  @Test fun `insert can run multiple times`() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }
    fun query(mapper: (SqlCursor) -> Unit) {
      driver.executeQuery(3, "SELECT * FROM test", mapper, 0)
    }

    query {
      assertFalse(it.next())
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }

    query {
      assertTrue(it.next())
      assertFalse(it.next())
    }

    assertEquals(1, changes())

    query {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
    }

    insert {
      bindLong(1, 2)
      bindString(2, "Jake")
    }
    assertEquals(1, changes())

    query {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    driver.execute(5, "DELETE FROM test", 0)
    assertEquals(2, changes())

    query {
      assertFalse(it.next())
    }
  }

  @Test fun `query can run multiple times`() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }
    assertEquals(1, changes())
    insert {
      bindLong(1, 2)
      bindString(2, "Jake")
    }
    assertEquals(1, changes())

    fun query(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> Unit) {
      driver.executeQuery(6, "SELECT * FROM test WHERE value = ?", mapper, 1, binders)
    }

    query(
      binders = {
        bindString(1, "Jake")
      },
      mapper = {
        assertTrue(it.next())
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
      }
    )

    // Second time running the query is fine
    query(
      binders = {
        bindString(1, "Jake")
      },
      mapper = {
        assertTrue(it.next())
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
      }
    )
  }

  @Test fun `SqlResultSet getters return null if the column values are NULL`() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }
    insert {
      bindLong(1, 1)
      bindLong(2, null)
      bindString(3, null)
      bindBytes(4, null)
      bindDouble(5, null)
    }
    assertEquals(1, changes())

    val mapper: (SqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0)
  }
}
