package ptcgtool

import api.CardFetcher

import javafx.scene.input.ScrollEvent
import ptcgtool.objects.Deck
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
import scalafx.Includes.jfxHBox2sfx
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.{Modality, Stage, Window}

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
          image.fitHeight = windowSize.height * 0.28
          image.preserveRatio = true
          image.onMouseClicked = _ =>
            new Stage:
              initModality(ApplicationModal)
              title = card.getName
              scene = new Scene(new VBox(new Label(card.getName), new ImageView(card.getImg)), 200, 200)
              show()
          image
        )
    })

  private def getContent(scrollPane: ScrollPane): HBox =
    scrollPane.content.value.asInstanceOf[javafx.scene.layout.HBox]

  private def horizontalImageBoxScrollPane: ScrollPane =
    new ScrollPane:
      val imageList: HBox = new HBox:
        spacing = 10
        padding = Insets(10)
        style = "-fx-background-color: #1a1a1a"

      content = imageList
      prefViewportWidth = windowSize.width
      prefViewportHeight = windowSize.height * 0.3
      vbarPolicy = ScrollPane.ScrollBarPolicy.Never
      style = "-fx-background-color: transparent; -fx-border-color: transparent;"
      fitToWidth = true
      fitToHeight = true

      addEventFilter(Scroll, (event: ScrollEvent) => {
        val deltaY = -event.getDeltaY * 1.5
        val width = imageList.width.get() - this.getViewportBounds.getWidth
        val hValue = this.getHvalue
        this.setHvalue(hValue - deltaY / width)
      })


  private def deckBuilderTabContent: VBox =
    val deck = new Deck(Nil)

    val searchResultsPane: ScrollPane = horizontalImageBoxScrollPane
    val searchBar: TextField = new TextField:
      promptText = "Search for a card..."
      prefWidth = windowSize.width * 0.4
      //border is transparent
      style = "-fx-border-color: transparent;"

    val searchButton: Button = new Button:
      text = "Search"
      style = "-fx-border-color: transparent;"
      onAction = _ => searchCards(searchBar, getContent(searchResultsPane))

    //searchBar should be on the left, searchButton on the right
    val searchBarBox = new HBox:
      spacing = 10
      padding = Insets(10)
      alignment = Pos.Center
      children =
        searchBar ::
        searchButton ::
        Nil

    val deckLabel = Label("Deck")
    val deckNameField = TextField()
    val saveDeck = Button("Save Deck")
    val deckViewPane = horizontalImageBoxScrollPane

    //deckLabel, deckNameField and saveDeck should be in a HBox
    val deckViewHeader = new HBox:
      children = Seq(deckLabel, deckNameField, saveDeck)
      spacing = 10
      alignment = Pos.CenterLeft
      padding = Insets(10)

    val deckView = new VBox:
      children = Seq(
        deckViewHeader,
        deckViewPane
      )
      spacing = 10
      padding = Insets(10)

    new VBox:
      children = List(searchBarBox, searchResultsPane, new Separator, deckView)
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