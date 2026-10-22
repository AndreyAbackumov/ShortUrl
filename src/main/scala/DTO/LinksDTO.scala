package dto 

import cats.data._, cats.implicits._, cats.syntax.apply._
import io.circe.syntax._, io.circe.disjunctionCodecs._, io.circe.Encoder._, io.circe.Encoder
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import model.{User, Links}
import skunk.Session
import cats.effect.IO
import db.UserRepository
import service.Service

case class LinksDTO(username: String, url: String)

object LinksDTO {

  def validateUsername(username: String): ValidatedNec[String, String] =
    Validated.condNec(
        username.matches("""^[a-zA-Z]+"""), 
        username, 
        s"$username should contain only letters a..z or A..Z")

  def createLinksDTO(username: String, url: String): ValidatedNec[String, LinksDTO] = (validateUsername(username)).map(name => LinksDTO(name, url))

  inline given Decoder[ValidatedNec[String, LinksDTO]] = Decoder.forProduct2("username", "url")(createLinksDTO)

  inline given Encoder[LinksDTO] = deriveEncoder
  
  def toLinks(u: LinksDTO, session: Session[IO]): IO[Option[Links]] = 
    UserRepository.findIdByNameInUsers(u.username, session).map(optionId => {
        optionId match
            case Some(userId) => Some(Links(userId, u.url, Service.generateShortUrl))
            case None         => None
    })
}