package ptcgtool
package tabs

import api.CardFetcher.fetchCards
import api.IOTools.clearCache
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

private val TOP_SCROLLPANE_HEIGHT = 0.3
private val BOTTOM_SCROLLPANE_HEIGHT = 0.35

val deck = new Deck

def deckBuilderTabContent(windowSize: Dimension): VBox =
  val searchResultsPane: ScrollPane = horizontalImageBoxScrollPane(TOP_SCROLLPANE_HEIGHT, windowSize)
  val deckViewPane = horizontalImageBoxScrollPane(BOTTOM_SCROLLPANE_HEIGHT, windowSize)

  val searchView: VBox = new VBox:
    val searchBarBox: HBox = new HBox:
      def searchCards(searchBar: TextField): Unit =
        def toSearchResultsView(card: Card): ImageView =
          cardImageView(card, 0.28, windowSize, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))

        def updateSearchResultsPane(cards: Seq[Card]): Unit =
          runLater(getContent(searchResultsPane).children = cards.par map toSearchResultsView seq)

        fetchCards(searchBar.getText) andThen {
          case Success(cards) => updateSearchResultsPane(cards)
          case Failure(_) => println("Failed to fetch cards")
        }

      val searchBar: TextField = new TextField:
        promptText = "Search for a card..."
        prefWidth = windowSize.width * 0.4
        onAction = _ => searchCards(this)
      children = searchBar ::
        SimpleButton("Search", _ => searchCards(searchBar)) ::
        SimpleButton("Clear Cache", _ => clearCache) :: Nil
    children = Seq(searchBarBox, searchResultsPane)

  val deckView: VBox = new VBox:
    val deckViewHeader: HBox = new HBox:
      children = Label("Deck") :: new TextField :: Button("Save Deck") :: Nil
    children = Seq(deckViewHeader, deckViewPane)

  new VBox:
    children = List(searchView, separator(Horizontal), deckView)
