// package com.adrianfilip.zio

// import zio.config._, ConfigDescriptor._
// import zio.config.ConfigSource
// import zio.config.magnolia.DeriveConfigDescriptor.descriptor
// import zio.config.magnolia.DeriveConfigDescriptor

// object MultipleSourcesExample extends App {

//   final case class MyConfig(ldap: String, port: Int, dburl: Option[String])

//   // Only used to fetch the source - the pattern is only for explanation purpose
//   val runtime = zio.Runtime.default

//   // Assume they are different sources (env, property file, HOCON / database (in future))
//   private val source1 = ConfigSource.fromMap(Map("LDAP" -> "jolap"), "constant")
//   private val source2 = runtime.unsafeRun(ConfigSource.fromSystemProperties)
//   private val source3 = runtime.unsafeRun(ConfigSource.fromSystemEnv)
//   private val source4 = ConfigSource.fromMap(Map("PORT" -> "1999"), "constant")
//   private val source5 = ConfigSource.fromMap(Map("DB_URL" -> "newyork.com"), "constant")

//   private val oneValidSource =
//     ConfigSource.fromMap(
//       Map(
//         "LDAP"   -> "jolap",
//         "PORT"   -> "1999",
//         "DB_URL" -> "newyork.com"
//       ),
//       "constant"
//     )

// //   val myConfig =
// //     ((string("LDAP").from(source1.orElse(source3)) |@| int("PORT").from(source4)) |@|
// //       string("DB_URL").optional.from(source1.orElse(source5)))(MyConfig.apply, MyConfig.unapply)

//   case class NonEmptyList[T](head: T, tail: List[T])

//   def configSource(sources: List[ConfigSource[String, String]]): ConfigSource[String, String] =
//     sources.reduce((s1, s2) => s1.orElse(s2))

//   def descriptor[T](derivedDescriptor: DeriveConfigDescriptor.Descriptor[T], source: ConfigSource[String, String]) =
//     derivedDescriptor.from(source)

//   val myConfig
//     : ConfigDescriptor[String, String, MyConfig] = ??? //descriptor[MyConfig].from(source1.orElse(source2).orElse(source3).orElse(source4).orElse(source5))

//   // Let's reset the whole source details in the original description
//   val myConfigWithReset = myConfig.unsourced.from(oneValidSource) // Equivalent to myConfig.fromNothing

//   // Have got a few more sources to be tried, on top of what's there already ?
//   val myConfigChangedSource = myConfig.updateSource(_.orElse(source2))

//   //

//   assert(
//     read(myConfig) == Right(MyConfig("jolap", 1999, Some("newyork.com")))
//   )

//   assert(
//     read(myConfigWithReset) == Right(MyConfig("jolap", 1999, Some("newyork.com")))
//   )

//   assert(
//     read(myConfigChangedSource) == Right(MyConfig("jolap", 1999, Some("newyork.com")))
//   )
// }
