package db

import cats.effect.IO
import cats.syntax.all._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import model.{User, Links}

object UserRepository {

  val dropTableUsers: Command[Void] =
    sql"DROP TABLE IF EXISTS users".command

  val createTableUsers: Command[Void] =
    sql"""
      CREATE TABLE users (
        id    SERIAL PRIMARY KEY,
        username  VARCHAR(50) NOT NULL UNIQUE
      )
    """.command

  val dropTableLinks: Command[Void] =
    sql"DROP TABLE IF EXISTS links".command

  val createTableLinks: Command[Void] =
    sql"""
      CREATE TABLE links (
        id    SERIAL PRIMARY KEY,
        user_id INTEGER NOT NULL,
        url TEXT NOT NULL,
        shorturl VARCHAR(50) NOT NULL,
        CONSTRAINT fk_urls 
          FOREIGN KEY (user_id) 
          REFERENCES users(id)
      )
    """.command

  val insertInLinks: Command[Links] =
    sql"""INSERT INTO links (user_id, url, shorturl) VALUES ($int4, $text, $varchar)"""
      .command
      .contramap[Links] { l => l.user_id *: l.url *: l.shortUrl *: EmptyTuple }

  def createInLinks(user: Links, s: Session[IO]): IO[Unit] =
    s.prepare(insertInLinks).flatMap(_.execute(user)).void
  
  val selectUrlByShortUrl: Query[String, String] =
    sql"SELECT url FROM links WHERE shorturl = $varchar"
      .query(text)
  
  def findByShortUrl(shortUrl: String, s: Session[IO]): IO[Option[String]] =
    s.prepare(selectUrlByShortUrl).flatMap(_.option(shortUrl))

  val insertUserInUsers: Command[User] =
    sql"""INSERT INTO users (username) VALUES ($varchar)"""
      .command
      .contramap[User] { u => u.username }

  def createInUsers(user: User, s: Session[IO]): IO[Unit] =
    s.prepareR(insertUserInUsers).use(_.execute(user)).void
  
  val selectByNameInUsers: Query[String, Int] =
    sql"SELECT id FROM users WHERE username = $varchar"
      .query(int4)
  
  def findIdByNameInUsers(username: String, s: Session[IO]): IO[Option[Int]] =
    s.prepare(selectByNameInUsers).flatMap(_.option(username))
  
  val selectUrlAndShortUrlByName: Query[String, (String, String)] = 
    sql"""SELECT l.shorturl, l.url FROM links l JOIN users u ON u.id = l.user_id WHERE u.username = $varchar""".query(varchar(50) ~ text)
  
  def findLinksByUsername(username: String, s: Session[IO]): IO[List[(String, String)]] =
    s.prepare(selectUrlAndShortUrlByName).flatMap(_.stream(username, 64).compile.toList)
}