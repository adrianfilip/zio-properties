package com.adrianfilip.zio.properties

import java.io.File

import com.typesafe.config.ConfigFactory
import izumi.reflect.Tags.Tag
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe._
import zio.config.{ ConfigSource, _ }
import zio.{ Has, Task, ZIO, ZLayer }

/**
 * Property resolution order:
 * - command line arguments
 * - system properties
 * - environment variables
 * - HOCON file
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
      appHoconSource <- profile.hoconFile match {
                         case Some(value) =>
                           fromHoconResource(s"/$value")
                         case None =>
                           fromHoconResourceIfPresent(
                             profile.profile.map(_.toLowerCase()).getOrElse(NO_PROFILE) match {
                               case NO_PROFILE => "/application.conf"
                               case PROD       => "/application.conf"
                               case profile    => s"/application-$profile.conf"
                             }
                           )
                       }
      appPropsSource <- profile.propertiesFile match {
                         case Some(value) =>
                           fromPropertiesResource(s"/$value", Some('.'), Some(','))
                         case None =>
                           fromPropertiesResourceIfPresent(
                             profile.profile.map(_.toLowerCase()).getOrElse(NO_PROFILE) match {
                               case NO_PROFILE => "/application.properties"
                               case PROD       => "/application.properties"
                               case profile    => s"/application-$profile.properties"
                             },
                             Some('.'),
                             Some(',')
                           )
                       }
    } yield List(argsConfigSource, systemPropsSource, envPropsSource, appHoconSource, appPropsSource)
  }

  /**
   * Will fail if the file is not found.
   *
   * @param file
   * @param keyDelimiter
   * @param valueDelimiter
   * @return
   */
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

  /**
   * Will not fail if file is not found. Instead it will create a ConfigSource from an empty java.util.Properties
   *
   * @param file
   * @param keyDelimiter
   * @param valueDelimiter
   * @return
   */
  def fromPropertiesResourceIfPresent[A](
    file: String,
    keyDelimiter: Option[Char] = None,
    valueDelimiter: Option[Char] = None
  ): Task[ConfigSource[String, String]] =
    for {
      properties <- ZIO.bracket(
                     ZIO.effect(getClass.getResourceAsStream(file))
                   )(r => ZIO.effectTotal(if (r != null) r.close())) { inputStream =>
                     ZIO.effect {
                       val properties = new java.util.Properties()
                       if (inputStream != null) {
                         properties.load(inputStream)
                       }
                       properties
                     }
                   }
    } yield ConfigSource.fromProperties(
      properties,
      file,
      keyDelimiter,
      valueDelimiter
    )

  /**
   * Will fail if the file is not found.
   *
   * @param file
   * @return
   */
  def fromHoconResource[A](file: String): Task[ConfigSource[String, String]] =
    for {
      resourceURI <- ZIO
                      .fromOption(Option(getClass.getResource(file)).map(_.toURI))
                      .mapError(_ => new RuntimeException(s"$file not found in classpath!"))
      fileInstance <- Task(new File(resourceURI))
      configSource <- ZIO
                       .fromEither(
                         TypesafeConfigSource.fromTypesafeConfig(ConfigFactory.parseFile(fileInstance).resolve)
                       )
                       .mapError(error => new RuntimeException(error))
    } yield configSource

  /**
   * Will not fail if file is not found. Instead it will create a ConfigSource from an empty HOCON string
   *
   * @param file
   * @return
   */
  def fromHoconResourceIfPresent[A](file: String): Task[ConfigSource[String, String]] =
    Option(getClass.getResource(file)).map(_.toURI) match {
      case Some(uri) =>
        Task(new File(uri)).flatMap(fileInstance =>
          TypesafeConfigSource
            .fromTypesafeConfig(ConfigFactory.parseFile(fileInstance).resolve) match {
            case Left(value)  => Task.fail(new RuntimeException(value))
            case Right(value) => Task.succeed(value)
          }
        )
      case None => Task.succeed(ConfigSource.empty)
    }

  final case class Profile(
    profile: Option[String],
    hoconFile: Option[String],
    propertiesFile: Option[String]
  )

  private def getProfile(configSource: ConfigSource[String, String]): Profile = {
    val desc   = descriptor[Profile]
    val params = desc.from(configSource)
    read(params) match {
      case Left(_)      => Profile(None, None, None)
      case Right(value) => value
    }
  }

}
