import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyKeys._
import sbtdocker.DockerKeys._
import sbtprotobuf.ProtobufPlugin._
import scala.language.implicitConversions
import scoverage.ScoverageKeys._

object Grpc {
  import Base._

  val execScript =
    """|#!/bin/sh
       |exec "${JAVA_HOME:-/usr}/bin/java" ${JVM_OPTIONS:-$DEFAULT_JVM_OPTIONS} \
       |     -cp "$0" -server io.buoyant.grpc.gen.Main "$@"
       |""".stripMargin

  val gen = projectDir("grpc/gen")
    .settings(
      mainClass := Some("io.buoyant.grpc.gen.Main"),
      mainClass in assembly := Some("io.buoyant.grpc.gen.Main"),
      assemblyJarName in assembly := s"protoc-gen-io.buoyant.grpc",
      assemblyOption in assembly := (assemblyOption in assembly).value.
        copy(prependShellScript = Some(execScript.split("\n").toSeq)),
      coverageEnabled := false)
    .withLibs(Deps.protobuf)
    .withTwitterLib(Deps.twitterUtil("app"))


  val runtime = projectDir("grpc/runtime")
    .dependsOn(Finagle.h2)
    .withLibs(Deps.protobuf)
    .withTests

  /*
   * Settings for generated modules.
   */

  private[this] def EnvPath = sys.env.getOrElse("PATH", "/bin:/usr/bin:/usr/local/bin")

  val grpcGenExec =
    taskKey[Option[File]]("Location of the protoc-io.buoyant.grpc plugin binary")

  // By default, assemble the plugin from the gen project. The
  // plugin is then linked into a temporary directory so that it
  // may be safely added to the PATH without exposing unintended
  // executables.
  private[this] lazy val grpcGenExec0 = Def.task {
      val link = IO.createUniqueDirectory(target.value) / "protoc-gen-io.buoyant.grpc"
      IO.copyFile((assembly in gen).value, link)
      link.setExecutable(true)
      Some(link)
    }

  // Ensure that the protoc-gen-io.buoyant.grpc plugin has been
  // assembled and is on the PATH when protoc runs.
  private[this] lazy val runProtoc0 = Def.task {
      val log = streams.value.log
      val env = grpcGenExec.value match {
        case None => Map.empty[String, String]
        case Some(ge) => Map("PATH" -> s"${ge.getCanonicalFile.getParent}:${EnvPath}")
      }
      (args: Seq[String]) => {
        val cmd = protoc.value +: args
        env.foreach { case (k, v) => log.debug(s":; export ${k}=${v}") }
        log.debug(":; " + cmd.mkString(" "))
        Process(cmd, None, env.toSeq:_*) ! log
      }
    }

  /** sbt-protobuf, without protobuf-java */
  val grpcGenSettings =
    protobufSettings ++ inConfig(protobufConfig)(Seq(
      javaSource := (sourceManaged in Compile).value,
      scalaSource := (sourceManaged in Compile).value,
      generatedTargets := Seq(scalaSource.value -> "*.pb.scala"),
      protoc := "./protoc",
      grpcGenExec := grpcGenExec0.value,
      runProtoc := runProtoc0.value,
      protocOptions :=
        Seq(s"--io.buoyant.grpc_out=plugins=grpc:${scalaSource.value.getCanonicalPath}")
    )) ++ Seq(
      // Sbt has trouble if scalariform rewrites the generated code
      excludeFilter in ScalariformKeys.format := "*.pb.scala",
      coverageExcludedFiles := """.*\.pb\.scala""",

      // We don't need protobuf-java.
      libraryDependencies := libraryDependencies.value.filterNot(_.name == "protobuf-java")
    )

  case class GrpcProject(project: Project) {
    def withGrpc: Project = project.settings(grpcGenSettings).dependsOn(runtime)
  }
  implicit def withGrpcProject(p: Project): GrpcProject = GrpcProject(p)

  /** Example */
  val eg = projectDir("grpc/eg")
    .withGrpc
    .withTests()
    .settings(publishArtifact := false)

  val interop = projectDir("grpc/interop")
    .withGrpc
    .withTests()
    .withTwitterLib(Deps.twitterServer)
    .settings(appAssemblySettings)
    .settings(publishArtifact := false)

  val all = aggregateDir("grpc", eg, gen, interop, runtime)
}
