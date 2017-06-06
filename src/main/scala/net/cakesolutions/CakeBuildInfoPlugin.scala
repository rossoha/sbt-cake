// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZonedDateTime}

import scala.util._

import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

// kicks in if the Project uses the BuildInfoPlugin
object CakeBuildInfoPlugin extends AutoPlugin {
  /**
    * When this plugin is enabled, {{autoImport}} defines a wildcard import for
    * set, eval, and .sbt files.
    */
  val autoImport = CakeBuildInfoKeys
  import autoImport._

  override def requires = BuildInfoPlugin
  override def trigger = allRequirements

  override val projectSettings = Seq(
    buildInfoKeys := Seq[BuildInfoKey](
      name in ThisBuild,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("lastCommitSha")(gitCommitHash)
    ),
    buildInfoPackage :=
      s"${organization.value}.${(name in ThisBuild).value}.build",
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,
    externalBuildTools := Seq(),
    generalInfo := {
      externalBuildTools ++= Seq(
        "hostname" ->
          "`hostname` command should be installed and PATH accessible",
        "whoami" -> "`whoami` command should be installed and PATH accessible",
        "git --version" ->
          "`git` command should be installed and PATH accessible"
      )
      Map(
        "buildDatetime" -> buildDatetime,
        "buildHost" -> buildHost,
        "buildUser" -> buildUser,
        "gitBranch" -> gitBranch,
        "gitCommitHash" -> gitCommitHash,
        "gitRepository" -> gitRepository
      )
    },
    dockerInfo := {
      externalBuildTools ++= Seq(
        "docker --version" ->
          "`docker` command should be installed and PATH accessible"
      )
      Map(
        "buildDockerVersion" -> buildDockerVersion
      )
    },
    checkExternalBuildTools := {
      externalBuildTools.value.foreach {
        case (checkCmd, errorMsg) =>
          require(checkCmd.! == 0, errorMsg)
      }
    },
    packageOptions in (Compile, packageBin) +=
      Package.ManifestAttributes(generalInfo.value.toSeq: _*)
  )

  private def buildDatetime =
    ZonedDateTime.now(Clock.systemUTC()).format(DateTimeFormatter.ISO_INSTANT)

  private def buildDockerVersion =
    Try("docker --version".!!.trim).getOrElse("n/a")

  private def buildHost = Try("hostname".!!.trim).getOrElse("n/a")

  private def buildUser = Try("whoami".!!.trim).getOrElse("n/a")

  private def gitBranch =
    Try("git symbolic-ref --short -q HEAD".!!.trim).getOrElse("n/a")

  private def gitCommitHash =
    Try("git rev-parse --verify HEAD".!!.trim).getOrElse("n/a")

  private def gitRepository =
    Try("git ls-remote --get-url".!!.trim).getOrElse("n/a")
}

/**
  * Build keys that will be auto-imported when this plugin is enabled.
  */
object CakeBuildInfoKeys {
  /**
    * Map holding generic set of labelled values. These are use to label jar
    * manifests and Docker containers.
    */
  val generalInfo: SettingKey[Map[String, String]] =
    settingKey[Map[String, String]](
      "Generic set of labelled values used, for example, to label jar " +
        "manifests and Docker containers"
    )

  /**
    * Map holding Docker specific labelled values. These are used to label
    * Docker containers.
    */
  val dockerInfo: SettingKey[Map[String, String]] =
    settingKey[Map[String, String]](
      "Docker specific labelled values used, for example, to label Docker " +
        "containers"
    )

  /**
    * Configures a sequence of command/error message pairs that the task
    * `checkExternalBuildTools` will check.
    */
  val externalBuildTools: SettingKey[Seq[(String, String)]] =
    settingKey[Seq[(String, String)]](
      "List of command/error message pairs that will be checked by the task " +
        "`checkExternalBuildTools`"
    )

  /**
    * Task that checks each command in the settings key `externalBuildTools`.
    * If the checked command returns a non-zero exit code, then an
    * `IllegalArgumentException` is raised with the supplied error message.
    */
  val checkExternalBuildTools: TaskKey[Unit] =
    taskKey[Unit](
      "Checks that all commands in `externalBuildTools` run correctly"
    )
}