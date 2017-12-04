package doobie.postgres.proto

import org.scalatest.FlatSpec
import shapeless._
import shapeless.tag._
import cats._
import cats.data._
import cats.implicits._

class RelayPageExtractorTest extends FlatSpec {
  import doobie.postgres.proto.imports._

  trait IdTag
  type Id = String @@ IdTag
  type Result = Long :: Long :: String :: Id :: HNil

  implicit val show: Show[Id] = (id: Id) => id.toString

  it should "check hasPrevious correctly" in {
    val list = List[Result](
      40L :: 41L :: "test" :: tag[IdTag]("x1") :: HNil,
      41L :: 41L :: "test1" :: tag[IdTag]("x2") :: HNil
    )

    val result = list.extract[Id]

    assert(!result.hasNextPage)
    assert(result.hasPreviousPage)
    assert(result.start.contains("x1"))
    assert(result.end.contains("x2"))
  }

  it should "check hasNext correctly" in {
    val list = List(
      40L :: 41L :: "test" :: tag[IdTag]("x1") :: HNil,
      41L :: 42L :: "test1" :: tag[IdTag]("x2") :: HNil
    )

    val result = list.extract[Id]

    assert(result.hasNextPage)
    assert(result.hasPreviousPage)
    assert(result.start.contains("x1"))
    assert(result.end.contains("x2"))
  }

}
