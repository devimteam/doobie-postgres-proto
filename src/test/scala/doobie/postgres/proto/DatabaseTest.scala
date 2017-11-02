package doobie.postgres.proto

import java.util.UUID

import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import doobie.imports._
import doobie.scalatest._
import doobie.postgres.imports._
import doobie.postgres.proto.imports._
import cats._
import cats.data._
import cats.implicits._
import com.devim.protobuf.relay.Cursor
import fs2.interop.cats._
import org.scalatest.FlatSpec
import shapeless._

import scala.util.Random

class DatabaseTest
    extends FlatSpec
    with ForAllTestContainer
    with IOLiteChecker {
  private val database = Random.alphanumeric.take(32).mkString
  private val username = Random.alphanumeric.take(32).mkString
  private val password = Random.alphanumeric.take(32).mkString

  override val container = PostgresContainer(database, username, password)

  override lazy val transactor = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    container.jdbcUrl,
    username,
    password
  )

  def withPreparedData(
      testCode: (String, UUID, UUID, UUID, UUID, UUID) => Any) {
    val tableName = Random.alphanumeric.filter(_.isLetter).take(32).mkString

    val query
      : Fragment = fr"create table " ++ Fragment.const(tableName) ++ fr"(id UUID,value varchar(255))"
    query.update.run.transact(transactor).unsafePerformIO

    def insertQuery(id: UUID, value: String): Update0 =
      (fr"insert into " ++ Fragment.const(tableName) ++ fr" values($id,$value)").update

    val (id1, id2, id3, id4, id5) = (UUID.randomUUID(),
                                     UUID.randomUUID(),
                                     UUID.randomUUID(),
                                     UUID.randomUUID(),
                                     UUID.randomUUID())

    val insertTransaction = for {
      _ <- insertQuery(id1, Random.nextString(32)).run
      _ <- insertQuery(id2, Random.nextString(32)).run
      _ <- insertQuery(id3, Random.nextString(32)).run
      _ <- insertQuery(id4, Random.nextString(32)).run
      _ <- insertQuery(id5, Random.nextString(32)).run
    } yield ()

    insertTransaction.transact(transactor).unsafePerformIO

    testCode(tableName, id1, id2, id3, id4, id5)

  }

  "cursor query" should "works correctly with provided \"first\" argument" in withPreparedData {
    (tableName, id1, id2, id3, id4, id5) =>
      val selectQuery: Fragment = fr"select id from " ++ Fragment.const(
        tableName)
      val cursor = Some(Cursor(first = Some(2)))

      val result = selectQuery
        .queryWithCursor[Long :: Long :: UUID :: HNil]("id", cursor)
        .list
        .transact(transactor)
        .unsafePerformIO

      assert(result.size == 2)
      assert(result(0).select[UUID] == id1)
      assert(result(1).select[UUID] == id2)
  }

  it should "works correctly with provided \"last\" argument" in withPreparedData {
    (tableName, id1, id2, id3, id4, id5) =>
      val selectQuery: Fragment = fr"select id from " ++ Fragment.const(
        tableName)
      val cursor = Some(Cursor(last = Some(2)))

      val result = selectQuery
        .queryWithCursor[Long :: Long :: UUID :: HNil]("id", cursor)
        .list
        .transact(transactor)
        .unsafePerformIO

      assert(result.size == 2)
      assert(result(0).select[UUID] == id4)
      assert(result(1).select[UUID] == id5)
  }

  it should "works correctly with provided \"before\" and \"after\" argument" in withPreparedData {
    (tableName, id1, id2, id3, id4, id5) =>
      val selectQuery: Fragment = fr"select id from " ++ Fragment.const(
        tableName)
      val cursor =
        Some(Cursor(before = Some(id4.toString), after = Some(id2.toString)))

      val result = selectQuery
        .queryWithCursor[Long :: Long :: UUID :: HNil]("id", cursor)
        .list
        .transact(transactor)
        .unsafePerformIO

      assert(result.size == 3)
      assert(result(0).select[UUID] == id2)
      assert(result(1).select[UUID] == id3)
      assert(result(2).select[UUID] == id4)
  }

  it should "works correctly with provided \"after\" and \"first\" argument" in withPreparedData {
    (tableName, id1, id2, id3, id4, id5) =>
      val selectQuery: Fragment = fr"select id from " ++ Fragment.const(
        tableName)
      val cursor =
        Some(Cursor(first = Some(2), after = Some(id2.toString)))

      implicit val h = LogHandler(v => println(v))

      val result = selectQuery
        .queryWithCursor[Long :: Long :: UUID :: HNil]("id", cursor)
        .list
        .transact(transactor)
        .unsafePerformIO

      assert(result.size == 2)
      assert(result(0).select[UUID] == id2)
      assert(result(1).select[UUID] == id3)
  }

}
