// package com.adrianfilip.zio.properties

// import zio.config.Config
// import zio.config._
// import zio.console._
// import zio.config.magnolia.DeriveConfigDescriptor.descriptor
// import zio.ZLayer._
// import zio.console.Console
// import zio.{ App, ZEnv, ZIO, ZLayer }

// object Hello3 extends App {

//   override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
//     val prg = for {
//       _ <- finalExecution.provideLayer(configLayer ++ ZLayer.requires[Console])
//     } yield ()
//     prg.fold(e => {
//         println(e)
//         1
//     }, _ => 0)
//   }

//   final case class ApiConfig(endpoint: String, port: Int)
  
//   final case class WsConfig(api: ApiConfig)

//   val wsConfigAutomatic = descriptor[WsConfig]

//   val configLayer = Config.fromMap(Map("api_endpoint" -> "localhost", "api_port" -> "8080"), wsConfigAutomatic, "", Some('_'), Some(','))

//   val finalExecution: ZIO[Console with Config[WsConfig], Nothing, Unit] =
//     for {
//       wsCommConfig <- config[WsConfig]
//       _            <- putStrLn(wsCommConfig.toString())
//     } yield ()

//   val runtime = zio.Runtime.default
// }
