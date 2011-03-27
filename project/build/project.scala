import sbt._

trait Defaults {
  def androidPlatformName = "android-8"
}
class Parent(info: ProjectInfo) extends ParentProject(info) {
  override def shouldCheckOutputDirectories = false
  override def updateAction = task { None }

  lazy val main  = project(".", "Tick", new MainProject(_))
  lazy val tests = project("tests",  "tests", new TestProject(_), main)
  lazy val pics = project("pics", "Tick Pics", new MainProject(_))

  class MainProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish with TypedResources {
    val keyalias  = "change-me"
    val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"
  }

  class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults
}
