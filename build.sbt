import Base._
import LinkerdBuild._

// Unified documentation via the sbt-unidoc plugin
val all = Base.project("all", file("."))
    .settings(aggregateSettings ++ unidocSettings)
    .aggregate(aggregates:_*)
