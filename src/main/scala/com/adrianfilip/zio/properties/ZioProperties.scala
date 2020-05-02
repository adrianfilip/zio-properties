package com.adrianfilip.zio.properties

import zio.config._
import zio.config.ConfigSource
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.ZLayer
import zio.ZIO
import zio.Has
import izumi.reflect.Tags.Tag
import zio.Task

/**
  * Property resolution order:
  * - command line arguments
  * - system properties
  * - environment variables
  * - properties file
  */
object ZioProperties {

  def createPropertiesLayer[T: Tag](
    args: List[String],
    descriptor: ConfigDescriptor[String, String, T]
  ): ZIO[Any, Throwable, ZLayer[Any, ReadError[String], Has[T]]] =
    for {
      sources <- createSources(args)
      desc    = descriptor.from(unifySources(sources))
      l       = ZLayer.fromEffect(ZIO.fromEither(read(desc)))
    } yield l

  private def unifySources(sources: List[ConfigSource[String, String]]): ConfigSource[String, String] =
    sources.reduce((s1, s2) => s1.orElse(s2))

  private def createSources(args: List[String]): ZIO[Any, Throwable, List[ConfigSource[String, String]]] = {
    val NO_PROFILE = ""
    val PROD       = "prod"
    for {
      argsConfigSource  <- ZIO.succeed(ConfigSource.fromCommandLineArgs(args, Some('.'), Some(',')))
      systemPropsSource <- ConfigSource.fromSystemProperties(Some('_'), Some(','))
      envPropsSource    <- ConfigSource.fromSystemEnv(Some('_'), Some(','))
      profile           = getProfile(unifySources(List(argsConfigSource, systemPropsSource, envPropsSource)))
      propertiesFile = profile.propertiesFile match {
        case Some(value) => s"/$value"
        case None =>
          profile.profile.map(_.toLowerCase()).getOrElse(NO_PROFILE) match {
            case NO_PROFILE => "/application.properties"
            case PROD       => "/application.properties"
            case profile    => s"/application-$profile.properties"
          }
      }
      appPropsSource <- fromPropertiesResource(
                         propertiesFile,
                         Some('.'),
                         Some(',')
                       )

    } yield List(argsConfigSource, systemPropsSource, envPropsSource, appPropsSource)
  }

  def fromPropertiesResource[A](
    file: String,
    keyDelimiter: Option[Char] = None,
    valueDelimiter: Option[Char] = None
  ): Task[ConfigSource[String, String]] =
    for {
      properties <- ZIO.bracket(
                     ZIO.effect(getClass.getResourceAsStream(file))
                   )(r => ZIO.effectTotal(r.close())) { inputStream =>
                     ZIO.effect {
                       val properties = new java.util.Properties()
                       properties.load(inputStream)
                       properties
                     }
                   }
    } yield ConfigSource.fromProperties(
      properties,
      file,
      keyDelimiter,
      valueDelimiter
    )

  final case class Profile(profile: Option[String], propertiesFile: Option[String])

  private def getProfile(configSource: ConfigSource[String, String]): Profile = {
    val desc   = descriptor[Profile]
    val params = desc.from(configSource)
    read(params) match {
      case Left(_)      => Profile(None, None)
      case Right(value) => value
    }
  }

}

object myapi2 extends zio.App {
  import zio.console._

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val desc = descriptor[MyConfig]

    val program = for {
      layer <- ZioProperties.createPropertiesLayer(args, desc)
      props <- myEffect.provideLayer(layer)
    } yield props

    program
      .flatMap(r => putStrLn(r.toString()))
      .tapError(err => putStrLn(err.toString()))
      .map(_ => 0) orElse ZIO.succeed(1)
  }

  case class MyConfig(username: String, db: Db, aliases: List[String])
  case class Db(host: String, port: String)

  val myEffect: ZIO[Config[MyConfig], Nothing, MyConfig] =
    for {
      myProps <- config[MyConfig]
    } yield myProps

}
