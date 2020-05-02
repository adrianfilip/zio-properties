import zio.App
import zio.config._
import zio.{ ZEnv, ZIO }
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.magnolia.DeriveConfigDescriptor

object HelloWorld2 extends App {

  final case class DBConfig(host: String, port: Int)
  final case class MyConfig(db: DBConfig, alias: Option[String])
  val myConfigSource: DeriveConfigDescriptor.Descriptor[MyConfig] = descriptor[MyConfig]

//   def createConfigDescriptor[T] = descriptor[T]

//   def createPropertyLayers =
//     Config.fromSystemEnv(
//       myConfigSource,
//       Some('_'),
//       Some(',')
//     )

  def createPropertyLayers = {
    val envConfigLayer = Config.fromMap(
      Map("DB_HOST" -> "localhost", "DB_PORT" -> "8080", "ALIAS" -> "mysite.com").map {
        case (k, v) => (k.toLowerCase, v)
      },
      myConfigSource,
      "",
      Some('_'),
      Some(',')
    )

    val appPropertiesConfigLayer = Config.fromMap(
      Map("db.host" -> "localhost", "db.port" -> "8080", "alias" -> "mysite.com"),
      myConfigSource,
      "",
      Some('.'),
      Some(',')
    )

    List(envConfigLayer, appPropertiesConfigLayer)
  }

  val evaluateProperties: ZIO[Config[MyConfig], Nothing, MyConfig] =
    for {
      r <- config[MyConfig]
    } yield r

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = prog().map(_ => 0) orElse ZIO.succeed(0)

  def prog(): ZIO[ZEnv, Throwable, Any] =
    ZIO.succeed(0)

  assert {
    val envConfigLayer = Config.fromMap(
      Map("DB_HOST" -> "localhost", "DB_PORT" -> "8080", "ALIAS" -> "mysite.com").map {
        case (k, v) => (k.toLowerCase, v)
      },
      myConfigSource,
      "",
      Some('_'),
      Some(',')
    )

    zio.Runtime.default
      .unsafeRun(evaluateProperties.provideLayer(envConfigLayer)) == MyConfig(
      DBConfig("localhost", 8080),
      Some("mysite.com")
    )
  }

  assert {

    val appPropertiesConfigLayer = Config.fromMap(
      Map("db.host" -> "localhost", "db.port" -> "8080", "alias" -> "mysite.com"),
      myConfigSource,
      "",
      Some('.'),
      Some(',')
    )

    zio.Runtime.default.unsafeRun(evaluateProperties.provideLayer(appPropertiesConfigLayer)) == MyConfig(
      DBConfig("localhost", 8080),
      Some("mysite.com")
    )
  }

}
