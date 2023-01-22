package ptcgtool
package tabs

import api.CardFetcher.fetchCards
import api.IOTools.{clearCache, readDeck, readDeckFolder, saveDeck}
import api.{Card, CardFetcher}
import objects.Deck
import tabs.deckBuilderTabContent

import javafx.event.EventHandler
import javafx.scene.input.{MouseEvent, ScrollEvent}
import scalafx.Includes.jfxHBox2sfx
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.Platform.runLater
import scalafx.application.{JFXApp3, Platform}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Orientation.Horizontal
import scalafx.geometry.Pos.Center
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.control.*
import scalafx.scene.control.ScrollPane.ScrollBarPolicy.Never
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.ScrollEvent.Scroll
import scalafx.scene.layout.*
import scalafx.scene.layout.BorderStrokeStyle.Solid
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.{Black, White}
import scalafx.scene.{Node, Scene}
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.StageStyle.Undecorated
import scalafx.stage.{Modality, Stage, StageStyle, Window}

import java.awt.Toolkit.getDefaultToolkit
import java.awt.{Dimension, Toolkit}
import java.io.{FileInputStream, InputStream}
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}

private val TOP_SCROLLPANE_HEIGHT = 0.45
private val BOTTOM_SCROLLPANE_HEIGHT = 0.50

val deck = new Deck

def deckBuilderTabContent(windowSize: Dimension): VBox =
  val searchResultsPane: ScrollPane = horizontalImageBoxScrollPane(TOP_SCROLLPANE_HEIGHT, windowSize)
  val deckViewPane = horizontalImageBoxScrollPane(BOTTOM_SCROLLPANE_HEIGHT, windowSize)

  val searchView: VBox = new VBox:
    val searchBarBox: HBox = new HBox:
      decorate(this, 20, 30)
      def fetchCardsAndUpdate(query: String): Unit =
        def toSearchResultsView(card: Card): ImageView =
          cardImageView(card, 0.35 * windowSize.height, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))

        def updateSearchResultsPane(cards: Seq[Card]): Unit =
          runLater(getContent(searchResultsPane).children = cards.par map toSearchResultsView seq)

        fetchCards(query) andThen {
          case Success(cards) => updateSearchResultsPane(cards)
          case Failure(_) => println("Failed to fetch cards")
        }

      children = SimpleSearchBar(fetchCardsAndUpdate, windowSize.getWidth * 0.5) :: SimpleButton("Clear Cache", _ => clearCache) :: Nil
    children = Seq(searchBarBox, searchResultsPane)

  val deckView: VBox = new VBox:
    val deckName: String = readDeckFolder.head
    readDeck(deckName).all foreach deck.add
    runLater {
      getContent(deckViewPane).children = deck.all.distinct map(toDeckCardView(_, deck, deckViewPane, windowSize))
    }

    val deckViewHeader: HBox = new HBox:
      decorate(this, 20, 30)
      children = SimpleLabel("Deck: ", 50) ::
        SimpleLabel(deckName.dropRight(".deck".length), 50) ::
        SimpleButton("Save Deck", _ => saveDeck("test", deck)) :: Nil
    children = Seq(deckViewHeader, deckViewPane)

  new VBox:
    style = s"-fx-background-color: $DARK_GRAY"
    children = List(searchView, SimpleSeparator(Horizontal), deckView)
