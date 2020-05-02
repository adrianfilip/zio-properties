package com.adrianfilip

import _root_.zio.config._
import _root_.zio.config.ConfigSource
import _root_.zio.config.magnolia.DeriveConfigDescriptor.descriptor
import _root_.zio.ZLayer
import _root_.zio.ZIO
import _root_.zio.Has
import izumi.reflect.Tags.Tag

object myapi2 extends _root_.zio.App {

  def run(args: List[String]): ZIO[_root_.zio.ZEnv, Nothing, Int] = {
    val desc = descriptor[MyConfig]

    val program = for {
      layer <- library.createPropertiesLayer(None, desc)
      props <- getProperties[MyConfig].provideLayer(layer)
    } yield props

    program
      .flatMap(r => _root_.zio.console.putStrLn(r.toString()))
      .tapError(err => _root_.zio.console.putStrLn(err.toString()))
      .map(_ => 0) orElse ZIO.succeed(1)
  }

  private def getProperties[T: Tag]: ZIO[Config[T], Nothing, T] =
    for {
      properties <- config[T]
    } yield properties

  case class MyConfig(username: String, db: Db, aliases: List[String])
  case class Db(host: String, port: String)

  object library {

    def createPropertiesLayer[T: Tag](
      profile: Option[String],
      descriptor: ConfigDescriptor[String, String, T]
    ): ZIO[Any, Throwable, ZLayer[Any, ReadError[String], Has[T]]] =
      for {
        sources <- createSources(profile)
        desc    = descriptor.from(unifySources(sources))
        l       = ZLayer.fromEffect(ZIO.fromEither(read(desc)))
      } yield l

    private def unifySources(sources: List[ConfigSource[String, String]]): ConfigSource[String, String] =
      sources.reduce((s1, s2) => s1.orElse(s2))

    private def createSources(profile: Option[String]): ZIO[Any, Throwable, List[ConfigSource[String, String]]] = {

      val propertiesFile: String = profile.map(_.toLowerCase()).getOrElse("") match {
        case ""     => "/application.properties"
        case "prod" => "/application.properties"
        case s      => s"/application.$s.properties"
      }

      val appProperties =
        ConfigSource.fromPropertiesFile(getClass().getResource(propertiesFile).getPath(), Some('.'), Some(','))
      val envProperties = ConfigSource.fromSystemEnv(Some('_'), Some(','))

      ZIO.collectAll(List(appProperties, envProperties))
    }

  }

}
