package db

import cats.effect.{IO, Resource}
import skunk.Session
import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import org.typelevel.otel4s.metrics.Meter.Implicits.noop

object Database {
  
  val host = sys.env.getOrElse("DB_HOST", "localhost")
  val port = sys.env.getOrElse("DB_PORT", "5432").toInt
  val user = sys.env.getOrElse("DB_USER", "admin")
  val db   = sys.env.getOrElse("DB_NAME", "myapp")
  val pass = sys.env.getOrElse("DB_PASSWORD", "root")

  val session: Resource[IO, Session[IO]] =
    Session.single[IO](
      host = host,
      port = port,
      user = user,
      database = db,
      password = Some(pass)
    )
}