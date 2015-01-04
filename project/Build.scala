import sbt._
import Keys._

import scala.scalajs.sbtplugin.env.nodejs.NodeJSEnv
import scala.scalajs.sbtplugin.ScalaJSPlugin._

import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

object Build extends sbt.Build{
  val scalacFlags = Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature",
    "-language:postfixOps", "-language:implicitConversions", "-language:higherKinds", "-language:existentials")

  val cross = new utest.jsrunner.JsCrossBuild(
    organization := "com.github.japgolly.fork.upickle",

    version := "golly-1",
    scalaVersion := "2.11.4",
    name := "upickle",

    scalacOptions := scalacFlags,
    scalacOptions in Test ++= scalacFlags,

    // Sonatype
    publishArtifact in Test := false,
    publishTo <<= version { (v: String) =>
      Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    ) /*++ (
      if (scalaVersion.value startsWith "2.11.") Nil
      else Seq(
        "org.scalamacros" %% s"quasiquotes" % "2.0.0" % "provided",
        compilerPlugin("org.scalamacros" % s"paradise" % "2.0.0" cross CrossVersion.full)
      )
    )*/,

    sourceGenerators in Compile <+= sourceManaged in Compile map { dir =>
      val file = dir / "upickle" / "Generated.scala"
      val tuples = (1 to 22).map{ i =>
        def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")
        val writerTypes = commaSeparated(j => s"T$j: Writer")
        val readerTypes = commaSeparated(j => s"T$j: Reader")
        val typeTuple = commaSeparated(j => s"T$j")
        val written = commaSeparated(j => s"writeJs(x._$j)")
        val pattern = commaSeparated(j => s"x$j")
        val read = commaSeparated(j => s"readJs[T$j](x$j)")

        s"""
        implicit final def Tuple${i}W[$writerTypes] = W[Tuple${i}[$typeTuple]](
          x => Js.Arr($written)
        )
        implicit final def Tuple${i}R[$readerTypes] = R[Tuple${i}[$typeTuple]](
          validate("Array(${i})"){case Js.Arr($pattern) => Tuple${i}($read)}
        )
        """
      }

      IO.write(file, s"""
        package upickle
        import acyclic.file

        /**
         * Auto-generated picklers and unpicklers, used for creating the 22
         * versions of tuple-picklers and case-class picklers
         */
        object TupleCodecs {
          import Aliases._
          import Fns._

          ${tuples.mkString("\n")}
        }
      """)
      Seq(file)
    },
    autoCompilerPlugins := true,
//    scalacOptions += "-Xlog-implicits",
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
    pomExtra :=
      <url>https://github.com/lihaoyi/upickle</url>
      <licenses>
        <license>
          <name>MIT license</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/lihaoyi/upickle.git</url>
        <connection>scm:git://github.com/lihaoyi/upickle.git</connection>
      </scm>
      <developers>
        <developer>
          <id>lihaoyi</id>
          <name>Li Haoyi</name>
          <url>https://github.com/lihaoyi</url>
        </developer>
      </developers>

  )

  def sourceMapsToGithub: Project => Project =
    p => p.settings(
      scalacOptions ++= (if (isSnapshot.value) Seq.empty else Seq({
        val a = p.base.toURI.toString.replaceFirst("[^/]+/?$", "")
        val g = "https://raw.githubusercontent.com/japgolly/upickle"
        s"-P:scalajs:mapSourceURI:$a->$g/v${version.value}/"
      }))
    )

  lazy val root = cross.root

  lazy val js = cross.js.settings(
    (jsEnv in Test) := new NodeJSEnv
  ).configure(sourceMapsToGithub)

  lazy val jvm = cross.jvm.settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.7.2"
  )
}
