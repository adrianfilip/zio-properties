package com.adrianfilip.zio.properties

import zio._
import zio.config.config
import zio.console._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor

import HelloWorld._

object HelloWorld {
  case class AppProperties(
    profile: Option[String],
    propertiesFile: Option[String],
    username: String,
    db: Db,
    aliases: List[String]
  )
  case class Db(host: String, port: String)
}

object HelloWorldSpec extends DefaultRunnableSpec {
  def spec = suite("HelloWorldSpec")(

    testM("respects property resolution order") {


      val desc = descriptor[AppProperties]
      for {
        layer  <- ZioProperties.createPropertiesLayer(List("-profile=xxx"), desc)
        props  <- config[AppProperties].provideLayer(layer)
        _      <- putStr(props.toString())
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector("Hello, World!\n")))
    }
  )
}
