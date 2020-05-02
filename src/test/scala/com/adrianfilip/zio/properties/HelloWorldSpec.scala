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
  def sayHello: ZIO[Console, Nothing, Unit] =
    console.putStrLn("Hello, World!")

  case class AppProperties(username: String, db: Db, aliases: List[String])
  case class Db(host: String, port: String)
}

object HelloWorldSpec extends DefaultRunnableSpec {
  def spec = suite("HelloWorldSpec")(
    testM("sayHello correctly displays output") {
      val desc = descriptor[AppProperties]
      for {
        layer  <- ZioProperties.createPropertiesLayer(List.empty, desc)
        props  <- config[AppProperties].provideLayer(layer)
        _      <- putStr(props.toString())
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector("Hello, World!\n")))
    }
  )
}
