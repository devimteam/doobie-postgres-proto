package doobie.postgres.proto

import com.dimafeng.testcontainers.SingleContainer
import org.testcontainers.containers.{
  PostgreSQLContainer => OTPostgresSQLContainer
}

class PostgresContainer private (database: String,
                                 username: String,
                                 password: String)
    extends SingleContainer[OTPostgresSQLContainer[_]] {

  type OTCContainer = OTPostgresSQLContainer[T] forSome {
    type T <: OTPostgresSQLContainer[T]
  }
  override val container: OTCContainer = new OTPostgresSQLContainer()
    .withDatabaseName(database)
    .asInstanceOf[OTCContainer]
    .withUsername(username)
    .asInstanceOf[OTCContainer]
    .withPassword(password)

  def driverClassName: String = container.getDriverClassName

  def jdbcUrl: String = container.getJdbcUrl

  def testQueryString: String = container.getTestQueryString

}

object PostgresContainer {
  def apply(database: String = "test",
            username: String = "test",
            password: String = "test") =
    new PostgresContainer(database, username, password)
}
