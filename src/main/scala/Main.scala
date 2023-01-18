package ptcgtool

import api.CardFetcher

import javafx.scene.input.ScrollEvent
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.Platform.runLater
import scalafx.application.{JFXApp3, Platform}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.ScrollEvent.Scroll
import scalafx.scene.layout.{HBox, VBox}

import java.awt.Toolkit.getDefaultToolkit
import java.awt.{Dimension, Toolkit}
import java.io.{FileInputStream, InputStream}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object Main extends JFXApp3:
  //get current screen size
  private val screenSize = getDefaultToolkit.getScreenSize
  private val windowSize = Dimension((screenSize.width * 0.8).toInt, (screenSize.height * 0.8).toInt)
  val cardFetcher = new CardFetcher


  private def searchCards(searchBar: TextField, imageList: HBox): Unit =
    val futureCards = cardFetcher.fetchCards(searchBar.text.value)
    futureCards onComplete (cards => runLater {
      imageList.children = cards.get map (card =>
         val image = ImageView(card.getImg)
         // images should preserve their aspect ratio but be scaled to fit the window
          image.fitHeight = windowSize.height / 2.6
          image.preserveRatio = true
          image
        )
    })


  private def deckBuilderTabContent: VBox =
    val imageList = new HBox:
      spacing = 10
      padding = Insets(10)
      // color should be some very dark gray
      style = "-fx-background-color: #1a1a1a"

    val horizontalScrollPane: ScrollPane = new ScrollPane:
      content = imageList
      prefViewportWidth = windowSize.width / 2.4
      prefViewportHeight = windowSize.height / 2.4
      vbarPolicy = ScrollPane.ScrollBarPolicy.Never
      //border is transparent
      style = "-fx-background-color: transparent; -fx-border-color: transparent;"
      fitToWidth = true
      fitToHeight = true

      addEventFilter(Scroll, (event: ScrollEvent) => {
        val deltaY = - event.getDeltaY * 1.5
        val width = imageList.width.get() - this.getViewportBounds.getWidth
        val hValue = this.getHvalue
        this.setHvalue(hValue - deltaY / width)
      })

    val searchBar: TextField = new TextField:
      promptText = "Search for a card..."
      prefWidth = windowSize.width * 0.4
      //border is transparent
      style = "-fx-border-color: transparent;"

    val searchButton: Button = new Button:
      text = "Search"
      style = "-fx-border-color: transparent;"
      onAction = _ => searchCards(searchBar, imageList)

    //searchBar should be on the left, searchButton on the right
    val searchBarBox = new HBox:
      spacing = 10
      padding = Insets(10)
      //center the search bar
      alignment = Pos.Center
      children = searchBar :: searchButton :: Nil

    val deckLabel = Label("Deck")
    val deckNameField = TextField()
    val saveDeck = Button("Save Deck")
    val deckList = new HBox
    val deckScrollPane = new ScrollPane:
      content = deckList

    //deckLabel, deckNameField and saveDeck should be in a HBox
    val deckViewHeader = new HBox:
      children = Seq(deckLabel, deckNameField, saveDeck)
      spacing = 10
      alignment = Pos.CenterLeft
      padding = Insets(10)

    val deckView = new VBox:
      children = Seq(deckViewHeader, deckScrollPane)
      spacing = 10
      padding = Insets(10)

    new VBox:
      children = List(searchBarBox, horizontalScrollPane, new Separator, deckView)
      prefWidth = windowSize.width * 0.8
      prefHeight = windowSize.height * 0.8
      style = "-fx-background-color: #1a1a1a"
      focusTraversable = false


  private def statsTabContent: VBox =
    new VBox:
      children = List(new Label("Stats"))
      prefWidth = windowSize.width * 0.8
      prefHeight = windowSize.height * 0.8


  override def start(): Unit =
    println("Starting GUI...")
    new PrimaryStage:
      title = "PTCG Tool"
      scene = new Scene(windowSize.width, windowSize.height):
        val deckBuilderTab: Tab = new Tab:
          text = "Deck Builder"
          content = deckBuilderTabContent
        val statsTab: Tab = new Tab:
          text = "Stats"
          content = statsTabContent

        root = new TabPane:
          tabs = List(deckBuilderTab, statsTab)