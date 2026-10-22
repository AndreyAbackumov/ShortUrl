package service

import scala.util.Random

object Service {

    private val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val length = 6
    def generateShortUrl: String = (1 to length).map(_ => alphabet(Random.nextInt(alphabet.length))).mkString
    
}