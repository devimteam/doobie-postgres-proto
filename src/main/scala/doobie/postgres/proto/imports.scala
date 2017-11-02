package doobie.postgres.proto

import com.devim.protobuf.relay.Cursor
import doobie.imports._

object imports {

  implicit class RelayFragment(fr: Fragment) {
    def queryWithCursor[B: Composite](cursorColumn: String,
                                      cursor: Option[Cursor],
                                      alias: String = "data",
                                      cursorColumnType: String = "UUID")(
        implicit h: LogHandler = LogHandler.nop): Query0[B] = {
      val wrapped = wrapQuery(alias)
      sliding(wrapped, cursor, alias, cursorColumn, cursorColumnType).query[B]
    }

    private def wrapQuery(alias: String): Fragment = {
      Fragment.const(
        s"with $alias as (select row_number() over() as rnum,t.* from (") ++
        fr ++
        Fragment.const(") t) select max(rnum) over(),t.* from data t")
    }

    private def sliding(query: Fragment,
                        maybeCursor: Option[Cursor],
                        alias: String,
                        slidingColumn: String,
                        cursorColumnType: String): Fragment = {
      val columnFr = Fragment.const(slidingColumn)
      val aliasFr = Fragment.const(alias)

      maybeCursor
        .map {
          case Cursor(beforeMaybe, afterMaybe, None, Some(last)) =>
            fr"select * from (" ++
              query ++
              Fragments.whereAndOpt(
                afterMaybe.map(after =>
                  afterPredicate(aliasFr, cursorColumnType, columnFr, after)),
                beforeMaybe.map(before =>
                  beforePredicate(aliasFr, cursorColumnType, columnFr, before))
              ) ++
              fr"order by rnum desc" ++
              Fragment.const(s"limit $last ) t order by t.rnum asc")
          case Cursor(beforeMaybe, afterMaybe, first, _) =>
            query ++
              Fragments.whereAndOpt(
                afterMaybe.map(after =>
                  afterPredicate(aliasFr, cursorColumnType, columnFr, after)),
                beforeMaybe.map(before =>
                  beforePredicate(aliasFr, cursorColumnType, columnFr, before))
              ) ++ first
              .map(limit => Fragment.const(s"limit $limit"))
              .getOrElse(Fragment.empty)
        }
        .getOrElse(query)

    }

    private def afterPredicate(alias: Fragment,
                               cursorColumnType: String,
                               columnFr: Fragment,
                               after: String): Fragment = {
      fr"rnum>=(select min(rnum) from" ++
        alias ++
        fr"where" ++
        columnFr ++
        fr"=$after::" ++
        Fragment.const(cursorColumnType) ++
        fr")"
    }

    private def beforePredicate(alias: Fragment,
                                cursorColumnType: String,
                                columnFr: Fragment,
                                before: String): Fragment = {
      fr"rnum<=(select max(rnum) from" ++
        alias ++
        fr"where" ++
        columnFr ++
        fr"=$before::" ++
        Fragment.const(cursorColumnType) ++
        fr")"
    }
  }

}
