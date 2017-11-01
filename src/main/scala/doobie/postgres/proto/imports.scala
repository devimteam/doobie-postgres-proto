package doobie.postgres.proto

import com.devim.protobuf.relay.Cursor
import doobie.imports._

object imports {

  implicit class RelayFragment(fr: Fragment) {
    def queryWithCursor[B: Composite](cursorColumn: String,
                                      cursor: Option[Cursor],
                                      alias: String = "data",
                                      cursorColumnType: String = "UUID")(
        implicit h: LogHandler = LogHandler.nop): Query0[B] =
      (Fragment.const(
        s"with $alias as (select row_number() over() as rnum,t.* from (") ++
        fr ++
        Fragment.const(") t) select max(rnum) over(),t.* from data t") ++
        sliding(cursor, alias, cursorColumn, cursorColumnType))
        .query[B]

    private def sliding(maybeCursor: Option[Cursor],
                        alias: String,
                        slidingColumn: String,
                        cursorColumnType: String): Fragment = {
      val columnFr = Fragment.const(slidingColumn)
      val aliasFr = Fragment.const(alias)

      Fragments.whereAndOpt(maybeCursor
        .map {
          case Cursor(before, after, None, Some(last)) =>
            Fragments.and(
              fr"rnum>=(select min(rnum) from t where" ++
                columnFr ++
                fr"= $after ::" ++
                Fragment.const(cursorColumnType) ++
                fr")",
              fr"rnum<=(select max(rnum) from t where" ++
                columnFr ++
                fr"= $before::" ++
                Fragment.const(cursorColumnType) ++
                fr")"
            ) ++ fr"order by rnum desc limit $last"
          case Cursor(before, after, first, _) =>
            Fragments.and(
              fr"rnum>=(select min(rnum) from" ++
                aliasFr ++
                fr"where" ++
                columnFr ++
                fr"= $after ::" ++
                Fragment.const(cursorColumnType) ++
                fr")",
              fr"rnum<=(select max(rnum) from" ++
                aliasFr ++
                fr"where" ++
                columnFr ++
                fr"= $before::" ++
                Fragment.const(cursorColumnType) ++
                fr")"
            ) ++ first.map(limit => fr"limit $limit").getOrElse(Fragment.empty)
        })

    }

  }

}
