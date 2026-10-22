package test

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.testcontainers.utility.DockerImageName
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.dsl.io._
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import skunk.Session
import skunk.implicits._
import skunk.codec.all._
import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import org.typelevel.otel4s.metrics.Meter.Implicits.noop
import cont.objectHttpCont
import db.UserRepository

class MySuite extends AnyFlatSpec with Matchers with TestContainerForAll {

  override val containerDef: PostgreSQLContainer.Def =
    PostgreSQLContainer.Def(
      dockerImageName = DockerImageName.parse("postgres:16-alpine")
  )

  def withSession[A](f: Session[IO] => IO[A]): A =
    withContainers { container =>
      Session.single[IO](
        host     = container.host,
        port     = container.mappedPort(5432),
        user     = container.username,
        database = container.databaseName,
        password = Some(container.password)
      ).use { session =>
        initSchema(session) *> f(session)
      }
    }.unsafeRunSync()

  def initSchema(s: Session[IO]): IO[Unit] =
    s.execute(UserRepository.dropTableLinks).void *>
      s.execute(UserRepository.dropTableUsers).void *>
      s.execute(UserRepository.createTableUsers).void *>
      s.execute(UserRepository.createTableLinks).void

  def postAddUser(session: Session[IO], username: String): IO[Response[IO]] =
    val routes = objectHttpCont.service(session)
    val request = Request[IO](Method.POST, uri"/api/addUser")
      .withEntity(Json.obj("username" -> Json.fromString(username)))
    routes.orNotFound.run(request)

  "POST /api/addUser" should "return 201 Created for a new user" in {
    withSession(session =>
      postAddUser(session, "alice").flatMap(resp =>
        IO(resp.status shouldBe Status.Created).flatMap(_ => resp.as[Json].map(_ shouldBe Json.obj("message" -> Json.fromString("User Added"))))
      )
    )
  }

  it should "return 409 Conflict for a duplicate username" in {
    withSession(session =>
      for {
        _         <- postAddUser(session, "alice")
        response  <- postAddUser(session, "alice")
        body      <- response.as[Json]
      } yield {
        response.status shouldBe Status.Conflict
        body shouldBe Json.obj(
          "error"   -> Json.fromString("USERNAME_TAKEN"),
          "message" -> Json.fromString("Username 'alice' is already taken")
        )
      }
    )
  }

  it should "return 400 Bad Request for an invalid username" in {
    withSession(session =>
      postAddUser(session, "alice!!!").flatMap(resp =>
        IO(resp.status shouldBe Status.BadRequest).flatMap(_ => resp.as[Json].map(_ shouldBe Json.obj(
          "error" -> Json.fromString("INVALID_FORMAT"),
          "message" -> Json.fromString("alice!!! should contain only letters a..z or A..Z")
        )))
      )
    )
  }
}