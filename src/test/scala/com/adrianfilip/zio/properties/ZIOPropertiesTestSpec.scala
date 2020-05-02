package com.adrianfilip.zio.properties

import zio._
import zio.config.config
import zio.test._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import ZIOPropertiesTest._
import zio.test.Assertion._
import zio.test.TestAspect._

object ZIOPropertiesTest {
  case class AppProperties(
    profile: Option[String],
    propertiesFile: Option[String],
    username: String,
    db: Db,
    aliases: List[String]
  )
  case class Db(host: String, port: Int)

  val appPropertiesDescriptor = descriptor[AppProperties]

}

object ZIOPropertiesTestSpec extends DefaultRunnableSpec {
  def spec =
    suite("ZIOProperties spec")(
      testM("if profile and propertiesFile missing it defaults to application.properties") {
        val expected =
          AppProperties(
            profile = None,
            propertiesFile = None,
            username = "diana",
            db = Db(
              host = "localhost-diana",
              port = 8083
            ),
            aliases = List("alias-d1", "alias-d2", "alias-d3")
          )

        for {
          layer <- ZioProperties.createPropertiesLayer(List.empty, appPropertiesDescriptor)
          props <- config[AppProperties].provideLayer(layer)
        } yield assert(props)(equalTo(expected))
      },
      testM("if profile present in command line arguments it uses given profile properties") {
        val expected =
          AppProperties(
            profile = Some("dev"),
            propertiesFile = None,
            username = "hal",
            db = Db(
              host = "localhost-hal",
              port = 8084
            ),
            aliases = List("alias-h1", "alias-h2", "alias-h3")
          )

        for {
          layer <- ZioProperties.createPropertiesLayer(List("-profile=dev"), appPropertiesDescriptor)
          props <- config[AppProperties].provideLayer(layer)
        } yield assert(props)(equalTo(expected))
      },
      testM("if profile present in system properties it uses given profile properties") {

        val expected =
          AppProperties(
            profile = Some("dev"),
            propertiesFile = None,
            username = "hal",
            db = Db(
              host = "localhost-hal",
              port = 8084
            ),
            aliases = List("alias-h1", "alias-h2", "alias-h3")
          )

        Task(java.lang.System.setProperty("profile", "dev"))
          .bracket(_ => UIO(java.lang.System.clearProperty("profile"))) { _ =>
            for {
              layer <- ZioProperties.createPropertiesLayer(List.empty, appPropertiesDescriptor)
              props <- config[AppProperties].provideLayer(layer)
            } yield assert(props)(equalTo(expected))
          }

      },
      // testM("if profile present in environment properties it uses given profile properties") {
// TODO
      // },
      testM("if propertyFile present in system properties it uses given propertyFile regardless of profile value") {
        val expected =
          AppProperties(
            profile = Some("dev"),
            propertiesFile = Some("custom.properties"),
            username = "wally",
            db = Db(
              host = "localhost-wally",
              port = 8085
            ),
            aliases = List("alias-w1", "alias-w2", "alias-w3")
          )

        Task {
          java.lang.System.setProperty("profile", "dev")
          java.lang.System.setProperty("propertiesFile", "custom.properties")
        }.bracket(_ =>
          UIO {
            java.lang.System.clearProperty("profile")
            java.lang.System.clearProperty("propertiesFile")
          }
        ) { _ =>
          for {
            layer <- ZioProperties.createPropertiesLayer(List.empty, appPropertiesDescriptor)
            props <- config[AppProperties].provideLayer(layer)
          } yield assert(props)(equalTo(expected))
        }
      },
      testM("Property resolution order") {


        val args = List("-db.port=9000")

        val expected =
          AppProperties(
            profile = Some("dev"),  //from system properties - prio 2
            propertiesFile = None,   //not provided anywhere
            username = "hal",  //from props file - prio 4
            db = Db(
              host = "127.0.0.1",  // because system properties is prio 2
              port = 9000    //because args is prio 1
            ),
            aliases = List("alias-h1", "alias-h2", "alias-h3")   //from props file - prio 4
          )

        Task {
          java.lang.System.setProperty("profile", "dev")
          java.lang.System.setProperty("db_host", "127.0.0.1")
        }.bracket(_ =>
          UIO {
            java.lang.System.clearProperty("profile")
            java.lang.System.clearProperty("db.host")
           
          }
        ) { _ =>
          for {
            layer <- ZioProperties.createPropertiesLayer(args, appPropertiesDescriptor)
            props <- config[AppProperties].provideLayer(layer)
          } yield assert(props)(equalTo(expected))
        }
      }
    ) @@ sequential
}
