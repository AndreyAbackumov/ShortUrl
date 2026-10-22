package model

case class User(username: String)

case class Links(user_id: Int, url: String, shortUrl: String)