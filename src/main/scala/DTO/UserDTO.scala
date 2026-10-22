package dto 

import cats.data._, cats.implicits._, cats.syntax.apply._
import io.circe.syntax._, io.circe.disjunctionCodecs._, io.circe.Encoder._, io.circe.Encoder
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import model.User

case class UserDTO(username: String)

object UserDTO {

  def validateUsername(username: String): ValidatedNec[String, String] =
    Validated.condNec(
        username.matches("""^[a-zA-Z]+"""), 
        username, 
        s"$username should contain only letters a..z or A..Z")

  def createUserDTO(username: String): ValidatedNec[String, UserDTO] = (validateUsername(username)).map(UserDTO.apply)

  inline given Decoder[ValidatedNec[String, UserDTO]] = Decoder.forProduct1("username")(createUserDTO)
  
  inline given Encoder[UserDTO] = deriveEncoder
  
  def toUser(u: UserDTO): User = User(u.username)

}