package com.adrianfilip

import _root_.zio.config._
import _root_.zio.config.ConfigSource
import _root_.zio.config.magnolia.DeriveConfigDescriptor
import _root_.zio.config.magnolia.DeriveConfigDescriptor.descriptor
import _root_.zio.ZLayer
import _root_.zio.ZIO
import izumi.reflect.Tags.Tag
import _root_.zio.Has


object myapi {

  def createSources(): List[ConfigSource[String, String]] = ???

  def configSource(sources: List[ConfigSource[String, String]]): ConfigSource[String, String] =
    sources.reduce((s1, s2) => s1.orElse(s2))

  def descriptor1[T](descriptor: DeriveConfigDescriptor.Descriptor[T], source: ConfigSource[String, String]) =
    descriptor.from(source)

     def descriptor2[T](descriptor: ConfigDescriptor[String, String, T], source: ConfigSource[String, String]) =
    descriptor.from(source)



    def createPropertiesLayer[T: Tag](descriptor: ConfigDescriptor[String, String, T]): ZLayer[Any, ReadError[String], Has[T]] = {
        val sources = createSources()
        val desc = descriptor.from(configSource(sources))
        ZLayer.fromEffect(ZIO.fromEither(read(desc)))
    }
    

  def main() = {

    val sources = createSources()
    val desc    = descriptor[MyConfig].from(configSource(sources))
    val layer = ZLayer.fromEffect(ZIO.fromEither(read(desc)))


    // this is it right here
    createPropertiesLayer(desc)

    println(sources)
    println(desc)
    println(layer)
    ???
  }

  case class MyConfig(host: String)

}
