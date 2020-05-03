package com.adrianfilip.zio.properties.examples

import zio.config._
import zio.console._
import zio.{ ZEnv, ZIO }
import com.adrianfilip.zio.properties.ZioProperties
import zio.config.magnolia.DeriveConfigDescriptor.descriptor

/**
  * This example will create, load (based on resolution order) and print the AppProperties if you provide the properties in 
  * 
  */
object ZioPropertiesExample extends zio.App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    //create the descriptor for your AppProperties
    val desc = descriptor[AppProperties]

    val p = for {
      //provide the arguments from main method and the descriptor to create the Layer
      layer <- ZioProperties.createPropertiesLayer(args, desc)
      props <- myProgram.provideCustomLayer(layer)
    } yield props

    p.tapError(err => putStrLn(err.toString()))
      .map(_ => 0) orElse ZIO.succeed(1)
  }

  /**
   * Effects that require AppProperties can retrieve it from the Layer
   */
  val myProgram: ZIO[Config[AppProperties] with Console, Nothing, AppProperties] =
    for {
      myProps <- config[AppProperties]
      _       <- putStrLn(myProps.toString())
    } yield myProps

  case class AppProperties(username: String, db: Db, aliases: List[String])
  case class Db(host: String, port: Int)

}
