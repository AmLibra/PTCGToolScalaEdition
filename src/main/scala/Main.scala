package ptcgtool

import api.CardFetcher.fetchCards
import api.{Card, CardFetcher}
import objects.Deck

import javafx.scene.input.ScrollEvent
import ptcgtool.api.IOTools.clearCache
import scalafx.Includes.jfxHBox2sfx
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
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.White
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.{Modality, Stage, StageStyle, Window}

import java.awt.Toolkit.getDefaultToolkit
import java.awt.{Dimension, Toolkit}
import java.io.{FileInputStream, InputStream}
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Main extends JFXApp3:
  private final val screenSize = getDefaultToolkit getScreenSize
  private final val windowSize = Dimension((screenSize.width * 0.8) toInt, (screenSize.height * 0.8) toInt)

  private final val SCROLL_SPEED = 1.5
  private final val TOP_SCROLLPANE_HEIGHT = 0.3
  private final val BOTTOM_SCROLLPANE_HEIGHT = 0.35

  private def deckBuilderTabContent: VBox =
    def horizontalImageBoxScrollPane(verticalRatio: Double): ScrollPane =
      new ScrollPane:
        val imageList: HBox = new HBox:
          spacing = 10
          padding = Insets(10)
          style = "-fx-background-color: #3a3a3a"

        content = imageList
        prefViewportWidth = windowSize.width
        prefViewportHeight = windowSize.height * verticalRatio
        vbarPolicy = ScrollPane.ScrollBarPolicy.Never
        style = "-fx-background-color: transparent; -fx-border-color: transparent;"
        fitToWidth = true
        fitToHeight = true

        addEventFilter(Scroll, (event: ScrollEvent) => {
          val deltaY = -event.getDeltaY * SCROLL_SPEED
          val width = imageList.width.get() - this.getViewportBounds.getWidth
          val hValue = this.getHvalue
          this.setHvalue(hValue - deltaY / width)
        })

    val deck = new Deck
    val searchResultsPane: ScrollPane = horizontalImageBoxScrollPane(TOP_SCROLLPANE_HEIGHT)
    val deckViewPane = horizontalImageBoxScrollPane(BOTTOM_SCROLLPANE_HEIGHT)

    def deckAdd(card: Card): Unit =
      deck.add(card)
      updateDeckView()

    def deckRemove(card: Card): Unit =
      deck.remove(card)
      updateDeckView()

    def getContent(scrollPane: ScrollPane): HBox = // have to specify type because of type erasure
      scrollPane.content.value.asInstanceOf[javafx.scene.layout.HBox]

    def updateDeckView(): Unit =
      runLater(getContent(deckViewPane).children = deck.all map toDeckCardView)

    def cardImageView(card: Card, scale: Double): ImageView =
      new ImageView(card.getImg):
        fitHeight = windowSize.height * scale
        preserveRatio = true

    def popup(content: Scene): Unit =
      new Stage:
        initModality(ApplicationModal)
        initStyle(StageStyle.Undecorated)
        content.setOnMouseClicked(_ => close())
        scene = content
        centerOnScreen()
        show()

    def toDeckCardView(card: Card): ImageView =
      def simpleCardPopUp(card: Card): Unit =
        popup(new Scene {
          content = cardImageView(card, 0.8)
        })

      val cardView = cardImageView(card, 0.25)
      cardView.setOnMouseClicked(_ => simpleCardPopUp(card))
      cardView


    def searchCards(searchBar: TextField): Unit =
      val searchQuery = searchBar.getText
      searchBar.clear()
      runLater {
        fetchCards(searchQuery).andThen({
          case Success(cards) => runLater {
            getContent(searchResultsPane).children = cards.par map (card => foundCardView(card)) seq
          }
          case Failure(exception) => println(exception)
        })
      }

    def foundCardView(card: Card): ImageView =
      val imageView = cardImageView(card, 0.28)
      imageView.setOnMouseClicked(_ => interactableCardPopUp(card))
      imageView

    def interactableCardPopUp(card: Card): Unit =
      val scene = new Scene:
        content = new VBox:
          val image: ImageView = cardImageView(card, 0.8)
          val buttons: HBox = new HBox:
            val addButton: Button = new Button("+"):
              onMouseClicked = _ => deckAdd(card)
            val removeButton: Button = new Button("-"):
              onMouseClicked = _ => deckRemove(card)
            children = Seq(addButton, removeButton)
            alignment = Pos.Center
            spacing = 10
          children = Seq(image, buttons)
      popup(scene)

    val searchBar: TextField = new TextField:
      promptText = "Search for a card..."
      prefWidth = windowSize.width * 0.4
      style = "-fx-border-color: transparent;"
      onAction = _ => searchCards(this)

    val searchButton: Button = new Button:
      text = "Search"
      style = "-fx-border-color: transparent;"
      onAction = _ => searchCards(searchBar)

    val clearCacheButton: Button = new Button:
      text = "Clear Cache"
      style = "-fx-border-color: transparent;"
      onAction = _ => clearCache

    val searchBarBox = new HBox:
      spacing = 10
      padding = Insets(10)
      alignment = Pos.Center
      children = Seq(searchBar, searchButton, clearCacheButton)

    val deckLabel = new Label("Deck"):
      textFill = White
    val deckNameField = TextField()
    val saveDeck = Button("Save Deck")

    //deckLabel, deckNameField and saveDeck should be in a HBox
    val deckViewHeader = new HBox:
      children = Seq(deckLabel, deckNameField, saveDeck)
      spacing = 10
      alignment = Pos.CenterLeft
      padding = Insets(10)

    val deckView = new VBox:
      children = Seq(deckViewHeader, deckViewPane)
      spacing = 10
      padding = Insets(10)

    new VBox:
      children = List(searchBarBox, searchResultsPane, new Separator, deckView)
      prefWidth = windowSize.width * 0.8
      prefHeight = windowSize.height * 0.8
      style = "-fx-background-color: #3a3a3a"
      focusTraversable = false


  private def statsTabContent: VBox =
    new VBox:
      children = List(new Label("Stats - WIP"))
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