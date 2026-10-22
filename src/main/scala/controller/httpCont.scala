package cont

import cats.effect.{IO, IOApp}
import com.comcast.ip4s._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import cats.effect.ExitCode
import io.circe.syntax._, io.circe.disjunctionCodecs._, io.circe.Encoder._, io.circe.Encoder
import cats.data.ValidatedNec
import org.http4s.circe._
import db.UserRepository
import skunk.Session
import cats.data.{Validated, ValidatedNec}
import cats.syntax.all._
import io.circe.Json
import dto.{UserDTO, LinksDTO}
import org.http4s.headers.Location

object objectHttpCont:

    given userDtoDecoder: EntityDecoder[IO, ValidatedNec[String, UserDTO]] = accumulatingJsonOf[IO, ValidatedNec[String, UserDTO]] 
    given linksDtoDecoder: EntityDecoder[IO, ValidatedNec[String, LinksDTO]] = accumulatingJsonOf[IO, ValidatedNec[String, LinksDTO]] 

    def service(s: Session[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {

        case request@POST -> Root / "api" / "addUser" => 
            request.as[ValidatedNec[String, UserDTO]]
                .flatMap({
                    case Validated.Valid(dto) => 
                            UserRepository.createInUsers(UserDTO.toUser(dto), s).attempt.flatMap(ans => {
                                ans match
                                    case Right(_) =>
                                        Created(Json.obj("message" -> Json.fromString("User Added")))

                                    case Left(e) => 
                                        Conflict(Json.obj(
                                            "error"   -> Json.fromString("USERNAME_TAKEN"),
                                            "message" -> Json.fromString(s"Username '${dto.username}' is already taken")
                                        ))
                            })
                    case Validated.Invalid(errors) =>  
                        BadRequest(Json.obj(
                            "error" -> Json.fromString("INVALID_FORMAT"),
                            "message" -> Json.fromString(errors.head) 
                        ))
                })
        
        case request@POST -> Root / "api" / "addUrl" => 
            request.as[ValidatedNec[String, LinksDTO]]
                .flatMap {
                    case Validated.Valid(dto) => 
                        LinksDTO.toLinks(dto, s).flatMap(optionLinks => { 
                            optionLinks match
                                case Some(link) => 
                                    UserRepository.createInLinks(link, s) *> Created(Json.obj("shortUrl" -> Json.fromString(link.shortUrl)))
                                case None =>
                                    NotFound(Json.obj(
                                        "error" -> Json.fromString("USER_NOT_FOUND"),
                                        "message" -> Json.fromString(s"Username '${dto.username}' doesn't exist")
                                    ))
                            })
                    case Validated.Invalid(errors) => 
                        BadRequest(Json.obj(
                            "error" -> Json.fromString("INVALID_FORMAT"),
                            "message" -> Json.fromString(errors.head) 
                        ))
                }

        case request @GET -> Root / "api" / "getPage" => 
            val username = request.params.get("username")
            val shortUrl = request.params.get("shortUrl")

            (username, shortUrl) match {
                case (Some(username), Some(short)) =>
                    UserDTO.validateUsername(username) match {
                        case Validated.Invalid(errors) =>
                            BadRequest(Json.obj( 
                                "error"   -> Json.fromString("INVALID_FORMAT"),
                                "message" -> Json.fromString(errors.head)
                            ))

                        case Validated.Valid(validUsername) =>
                            UserRepository.findIdByNameInUsers(validUsername, s).flatMap(optionId => {
                                optionId match {
                                    case None =>
                                        NotFound(Json.obj( 
                                            "error"   -> Json.fromString("USER_NOT_FOUND"),
                                            "message" -> Json.fromString(s"User '$validUsername' does not exist")
                                        ))
                                    case _ =>
                                        UserRepository.findByShortUrl(short, s).flatMap {
                                            case Some(originalUrl) => SeeOther(Location(Uri.unsafeFromString(originalUrl)))
                                            case None => NotFound(Json.obj("error" -> Json.fromString("short url not found")))
                                        }
                                }
                        })
                    }
                case _ => 
                    BadRequest(Json.obj(
                        "error"   -> Json.fromString("MISSING_PARAMS"),
                        "message" -> Json.fromString("Both 'username' and 'shortUrl' are required")
                    ))
            }

        case request @ GET -> Root / "api" / "getLinks" => 
            request.params.get("username") match {
                case Some(username) =>
                    UserDTO.validateUsername(username) match {
                        case Validated.Invalid(errors) =>
                            BadRequest(Json.obj( 
                                "error"   -> Json.fromString("INVALID_FORMAT"),
                                "message" -> Json.fromString(errors.head)
                            ))

                        case Validated.Valid(validUsername) =>
                            UserRepository.findIdByNameInUsers(validUsername, s).flatMap(optionId => {
                                optionId match {
                                    case None =>
                                        NotFound(Json.obj( 
                                            "error"   -> Json.fromString("USER_NOT_FOUND"),
                                            "message" -> Json.fromString(s"User '$validUsername' does not exist")
                                        ))
                                    case _ =>
                                        UserRepository.findLinksByUsername(validUsername, s).flatMap(listLinks => {
                                            val linkJsons = listLinks.map((shortUrl, url) =>
                                                Json.obj(
                                                    "shortUrl" -> Json.fromString(shortUrl),
                                                    "url"      -> Json.fromString(url)
                                                ))
                                            Ok(Json.arr(linkJsons*)) 
                                        })
                                }
                        })
                    }
                case _ =>
                BadRequest(Json.obj( 
                    "error"   -> Json.fromString("MISSING_PARAM"),
                    "message" -> Json.fromString("Query parameter 'username' is required")
                ))
            }
    }