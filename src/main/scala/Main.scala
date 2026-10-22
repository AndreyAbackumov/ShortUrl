
import cats.effect.{IO, IOApp}
import com.comcast.ip4s._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import cats.effect.ExitCode
import cont.objectHttpCont
import db.{Database, UserRepository}

object Main extends IOApp.Simple {

    val run: IO[Unit] =
    Database.session.use { s =>
      for {
        _ <- s.execute(UserRepository.dropTableLinks)
        _ <- s.execute(UserRepository.dropTableUsers)
        _ <- s.execute(UserRepository.createTableUsers)
        _ <- s.execute(UserRepository.createTableLinks)
        _ <- EmberServerBuilder
               .default[IO]
               .withHost(ipv4"0.0.0.0")
               .withPort(port"8080")
               .withHttpApp(objectHttpCont.service(s).orNotFound)
               .build
               .useForever
      } yield ()
    }
}