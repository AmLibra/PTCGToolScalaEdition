package ptcgtool
package objects

import api.Card
import api.CardFetcher

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

// this is the implementation of a Pokemon TCG deck of Cards (a deck of 60 cards)
// it is a List of Cards
final class Deck(cards: Seq[Card]):
  // this is a method that returns the number of cards in the deck
  private def size: Int = cards.size

  def countOf(card: Card): Int = cards.count(_ == card)

  def countOf(cardName: String): Int = cards.count(_.name.get == cardName)

  def all: Seq[Card] = cards

  def pokemonCards: Seq[Card] = cards.filter(_.isPokemon)

  def trainerCards: Seq[Card] = cards.filter(_.isTrainer)

  def energyCards: Seq[Card] = cards.filter(_.isEnergy)

  // add a card to the deck
  def add(card: Card): Deck = Deck(cards :+ card)

  // remove a card from the deck
  def remove(card: Card): Deck = Deck(cards.filter(_ != card))

  // check if the deck contains a card
  def contains(card: Card): Boolean = cards.contains(card)

  // checks if the deck is legal:
  // a deck is legal if it contains exactly 60 cards, a maximum of 4 copies of each card name that is not a basic energy card
  // and at least 1 basic pokemon card, ruleboxes should be checked separately
  def isLegalStandard: Boolean =
    val cardNames = cards.map(_.getName)
    val uniqueNameCardCounts = cardNames.distinct map countOf
    val basicPokemonCount = cards.count(_.isBasicPokemon)
    val allCardsStandardLegal = cards.forall(_.isStandardLegal)
    size == 60 && uniqueNameCardCounts.forall(_ <= 4) && basicPokemonCount >= 1 && allCardsStandardLegal

  // override toString to print the deck in 3 sections: Pokemon, Trainers and Energy
  // header is the name of the deck and the header of each section should look like this:
  // Header (Count: 10)
  // each card with a different id should be printed on a new line
  // the format of a card line is "x count name id", every card should get a unique line
  override def toString: String =
    def print(cards: Seq[Card]): String =
      val uniqueCardIds = cards map (_.getId) distinct
      val cardCounts = cards.distinct map countOf
      val cardNames = uniqueCardIds map (id => cards.find(_.getId == id).get.getName)
      val cardLines =
         cardCounts zip cardNames zip uniqueCardIds map { case ((count, name), id) => s"$count $name $id" }
      cardLines.mkString("\n")

    val pokemonLines = print(pokemonCards)
    val trainerLines = print(trainerCards)
    val energyLines = print(energyCards)
    s"Pokemon ($pokemonCards.size):\n$pokemonLines\n" +
      s"Trainers ($trainerCards.size):\n$trainerLines\n" +
      s"Energy ($energyCards.size):\n$energyLines"

  def parseCardLine(line: String): Future[Card] =
    val Array(count, name, id) = line.split(" ")
    val cardFetcher = new CardFetcher()
    cardFetcher.fetchCard(id)

  def parseDeck(deckString: String): Deck =
    var deck: Deck = Deck(Seq())
    val lines = deckString.split("\n")
    // remove the first line, which is the deck name
    val cardLines = lines.drop(1)
    // remove the header lines, all valid lines should start with a number
    val validCardLines = cardLines.filter(_.head.isDigit)
    val cards = validCardLines.map(parseCardLine).toSeq
    cards.foreach(card => card.onComplete {
      case Success(value) => deck = deck.add(value)
      case Failure(exception) => println(exception)
    })
    deck





