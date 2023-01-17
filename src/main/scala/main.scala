package ptcgtool

import api.CardFetcher

import ptcgtool.api.IOTools.clearCache

import java.awt.Toolkit
import scala.swing.Dimension
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.parallel.CollectionConverters.*

//get current screen size
val screenSize = Toolkit.getDefaultToolkit.getScreenSize
// 80% of screen size
val windowSize = new Dimension((screenSize.width * 0.8).toInt, (screenSize.height * 0.8).toInt)

val cardFetcher = new CardFetcher

@main
def main(): Unit =
  val cards = cardFetcher.fetchCards("charizard")
  cards.onComplete {
    case scala.util.Success(cards) =>
      cards.par.foreach(println)
      cards.par.foreach(_.getImg)
      clearCache
    case scala.util.Failure(exception) =>
      println(exception)
  }
  Thread.sleep(10000)
