package ptcgtool
package objects

import api.{Card, CardFetcher}
import api.CardFetcher.fetchCard

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

// this is the implementation of a Pokemon TCG deck of Cards (a deck of 60 cards)
// it is a List of Cards
final class Deck:

  private var cards: List[Card] = List.empty[Card]
  private def size: Int = cards.size

  def countOf(card: Card): Int = cards.count(_ == card)

  def countOf(cardName: String): Int = cards.count(_.name.get == cardName)

  def all: Seq[Card] =
    sort
    cards

  def pokemonCards: Seq[Card] = cards.filter(_.isPokemon).sortBy(_.name.get)

  def trainerCards: Seq[Card] = cards.filter(_.isTrainer).sortBy(_.name.get)

  def energyCards: Seq[Card] = cards.filter(_.isEnergy).sortBy(_.name.get)

  // add a card to the deck
  def add(card: Card): Unit = cards = card :: cards

  // remove a card from the deck
  def remove(card: Card): Unit = cards = cards.diff(List(card))

  // sort the deck: first by card type (Pokemon > Trainer > Energy), then by card name
  def sort: Unit =
    val (pokemon, trainersAndEnergies) = cards.partition(_.isPokemon)
    val (trainers, energies) = trainersAndEnergies.partition(_.isTrainer)
    cards = pokemon.sortBy(_.name.get) ++ trainers.sortBy(_.name.get) ++ energies.sortBy(_.name.get)

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

    val pokemonHeader = s"Pokemon (${pokemonCards.size}):\n"
    val trainerHeader = s"Trainers (${trainerCards.size}):\n"
    val energyHeader = s"Energy (${energyCards.size}):\n"

    pokemonHeader + s"$pokemonLines\n" +
      trainerHeader + s"$trainerLines\n" +
      energyHeader + s"$energyLines"

  private def parseCardLine(line: String): Future[Card] =
    val Array(_, _, id) = line.split(" ")
    fetchCard(id)

  def parseDeck(deckString: String): Deck =
    var deck: Deck = new Deck
    val lines = deckString.split("\n")
    // remove the first line, which is the deck name
    val cardLines = lines.drop(1)
    // remove the header lines, all valid lines should start with a number
    val validCardLines = cardLines.filter(_.head.isDigit)
    val cards = validCardLines.map(parseCardLine).toSeq
    Future.sequence(cards).onComplete {
      case Success(cards) => cards.foreach(deck.add)
      case Failure(exception) => println(exception)
    }
    deck





