package ptcgtool
package tabs

import api.Card
import objects.Deck

import javafx.animation.{KeyFrame, KeyValue, PauseTransition, Timeline}
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.input.{MouseEvent, ScrollEvent}
import javafx.util.Duration
import scalafx.Includes.jfxHBox2sfx
import scalafx.animation.Interpolator.EaseBoth
import scalafx.animation.Timeline.Indefinite
import scalafx.animation.{Animation, AnimationTimer, Interpolator}
import scalafx.application.Platform.runLater
import scalafx.geometry.Pos.Center
import scalafx.geometry.{Insets, Orientation}
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.control.ScrollPane.ScrollBarPolicy.Never
import scalafx.scene.effect.{BlurType, DropShadow}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.ScrollEvent.Scroll
import scalafx.scene.layout.*
import scalafx.scene.layout.BorderStrokeStyle.Solid
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.{Black, Transparent, White, color}
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.StageStyle.{TRANSPARENT, Undecorated}
import scalafx.stage.{Stage, StageStyle}
import scalafx.Includes.jfxTask2sfxTask

import java.awt.Dimension
import java.io.FileInputStream
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.language.postfixOps
import scala.util.Try

private val SCROLL_SPEED = 1.5

private val DARK_GRAY = "#2b2b2b"
private val LIGHTER_GRAY = "#3b3b3b"
private val TRANSPARENT = "transparent"

def backgroundIs(color: String): String = s"-fx-background-color: $color; -fx-background: $color"

private val EMPTY_CARD = Image(FileInputStream("C:\\Users\\usr\\IdeaProjects\\PTCGToolScalaEdition\\src\\resources\\emptyCard.png"))

val BOX_STYLE = s"-fx-background-color: $DARK_GRAY; -fx-control-inner-background: transparent; " +
  "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; " +
  "-fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 0; -fx-border-insets: 0; -fx-border-style: solid; "

def SimpleSeparator(o: Orientation): Separator =
  new Separator:
    orientation = o
    style = s"-fx-background: $DARK_GRAY; -fx-border-color: transparent; -fx-border-width: 0; "

// label with a certain text and size
def SimpleLabel(text: String, size: Double): Label =
  new Label(text):
    style = s"-fx-font-size: ${size}px; -fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\" "

def SimpleButton(text: String, eventHandler: EventHandler[_ >: MouseEvent]): Button =
  new Button(text):
    onMouseClicked = eventHandler
    style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;" +
    s"-fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-background-color: $DARK_GRAY; " +
      "-fx-text-fill: white; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 0;"
    // change color temporarily when mouse is over button
    onMouseEntered = _ => style = style.value + s"-fx-background-color: $LIGHTER_GRAY;"
    onMouseExited = _ => style = style.value.replace(s"-fx-background-color: $LIGHTER_GRAY;", "")

def SimpleToggleButton(text: String, eventHandler: EventHandler[_ >: MouseEvent]): ToggleButton =
  new ToggleButton(text):
    onMouseClicked = eventHandler
    style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;" +
    s"-fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-background-color: $DARK_GRAY; " +
      "-fx-text-fill: white; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 0;"
    // change color temporarily when mouse is over button
    onMouseEntered = _ => style = style.value + s"-fx-background-color: $LIGHTER_GRAY;"
    onMouseExited = _ => style = style.value.replace(s"-fx-background-color: $LIGHTER_GRAY;", "")


def SimpleSearchBar(search: String => Unit, size: Double): HBox =
  def SimpleSearchField(search: String => Unit): TextField =
    new TextField:
      style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; " +
        s"-fx-background-radius: 0; -fx-background-color: $LIGHTER_GRAY; -fx-text-fill: white; -fx-font-size: 20; " +
        "-fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-prompt-text-fill: white; -fx-prompt-text-font-size: 20; " +
        "-fx-prompt-text-font-weight: bold; -fx-prompt-text-font-family: \"Arial\";"
      promptText = "Search for a card..."
      onAction = _ => search(text.value)
      prefWidth = size

  new HBox:
    val textField: TextField = SimpleSearchField(search)
    val searchButton: Button = SimpleButton("Search", _ => search(textField.text.value))
    children = Seq(textField, searchButton)

def horizontalImageBoxScrollPane(verticalRatio: Double, windowSize: Dimension): ScrollPane =
  new ScrollPane:
    style = s"-fx-background-color: $DARK_GRAY; -fx-background: $DARK_GRAY; -fx-control-inner-background: $DARK_GRAY; "
    val imageList: HBox = new HBox:
      decorate(this, 10, 10)
      // remove top and bottom padding
      padding = Insets(0, 20, 0, 20)
      style = s"-fx-background-color: $DARK_GRAY "
    content = imageList
    prefViewportWidth = windowSize.width
    prefViewportHeight = windowSize.height * verticalRatio
    hbarPolicy = Never
    vbarPolicy = Never
    fitToWidth = true
    fitToHeight = true
    addEventFilter(Scroll, (event: ScrollEvent) => {
      val deltaY = -(event.getDeltaY * SCROLL_SPEED)
      val width = imageList.width.get - this.getViewportBounds.getWidth
      val hValue = this.getHvalue
      this.setHvalue(hValue - (deltaY / width))
    })

def toDeckCardView(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): VBox =
  new VBox:
    decorate(this, 10, 10)
    val image: ImageView = cardImageView(card, 0.25 * windowSize.height, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))
    zoomOnHover(image)
    val count: Label = Label(deck.countOf(card).toString)
    children = Seq(image, count)

def updateDeckViewPane(deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension, runnable: Runnable): Unit =
  runLater {
    getContent(deckViewPane).children = deck.all.distinct map (toDeckCardView(_, deck, deckViewPane, windowSize))
    runnable.run()
  }

def deckManagerCardWindow(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): Unit =
  def popup(content: Scene): Unit =
    new Stage:
      initModality(ApplicationModal)
      initStyle(StageStyle.Transparent)
      content.setOnMouseClicked(_ => close())
      scene = content
      centerOnScreen()
      show()

  val cardCount: Label = SimpleLabel(deck.countOf(card).toString, 50)
  cardCount.setStyle("-fx-font-size: 50; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-text-fill: white; " +
    "-fx-effect: dropshadow(gaussian, black, 7, 1, 0, 0);")
  val updateCardCount: Runnable = () => cardCount.text = deck.countOf(card).toString

  def updateDeckView(): Unit =
    runLater {
      getContent(deckViewPane).children = deck.all.distinct map (toDeckCardView(_, deck, deckViewPane, windowSize))
      updateCardCount
    }

  def deckAdd(card: Card): Unit =
    deck.add(card)
    updateDeckView()

  def deckRemove(card: Card): Unit =
    deck.remove(card)
    updateDeckView()

  popup(new Scene {
    fill = Transparent 
    content = new AnchorPane:
      style = "-fx-background-color: transparent"
      val cardImage: ImageView = cardImageView(card, 0.8 * windowSize.height, _ => ())
      cardImage.setEffect(new DropShadow(BlurType.Gaussian, Black, 20, 0.5, 0, 0))
      val cardAdd: Button = SimpleButton("+", _ => deckAdd(card))
      val cardRemove: Button = SimpleButton("-", _ => deckRemove(card))
      val cardCountBox: HBox = new HBox:
        decorate(this, 0, 10)
        style = "-fx-background-color: transparent"
        children = Seq(cardRemove, cardCount, cardAdd)
      children = Seq(cardImage, cardCountBox)
      AnchorPane.setTopAnchor(cardImage, 0.0)
      AnchorPane.setBottomAnchor(cardCountBox, -20.0)
      AnchorPane.setLeftAnchor(cardCountBox, 0.0)
      AnchorPane.setRightAnchor(cardCountBox, 0.0)
      runLater(updateCardCount)
  })

// have to specify type because of type erasure
def getContent(scrollPane: ScrollPane): HBox = scrollPane.content.value.asInstanceOf[javafx.scene.layout.HBox]

def cardImageView(card: Card, size: Double, eventHandler: EventHandler[_ >: MouseEvent]): ImageView =
  val smallCardImage = Try(card.getSmallImage).toOption match
    case Some(i) => i
    case None => EMPTY_CARD

  val imageView = new ImageView(smallCardImage):
    fitHeight = size
    preserveRatio = true
    onMouseClicked = eventHandler

  val loadLargeImageTask = new Task[Image]:
    override def call(): Image =
      Try(card.getLargeImage).toOption match
        case Some(i) => i
        case None => EMPTY_CARD

  loadLargeImageTask.valueProperty.addListener( _ => {
    if loadLargeImageTask.value != null && loadLargeImageTask.isDone then
      runLater(imageView.image = loadLargeImageTask.value.get)
  })

  loadLargeImageTask.run()

  imageView

// add padding, spacing and center alignment to a VBox or HBox
def decorate(box: HBox, padding: Double, spacing: Double): Unit =
  box.style = BOX_STYLE
  box.padding = Insets(padding)
  box.spacing = spacing
  box.alignment = Center

def decorate(box: VBox, padding: Double, spacing: Double): Unit =
  box.style = BOX_STYLE
  box.padding = Insets(padding)
  box.spacing = spacing
  box.alignment = Center


def zoomOnHover(imageView: ImageView): Unit = {
  val initialSize = imageView.getFitHeight
  val maxSize = initialSize * 1.1
  val zoomInTimeline = new Timeline(
    new KeyFrame(Duration.seconds(0.1), new KeyValue(imageView.fitHeightProperty(), maxSize, EaseBoth))
  )
  val zoomOutTransition = new PauseTransition(Duration.seconds(0.05))
  zoomOutTransition.setOnFinished(_ => new Timeline(
    new KeyFrame(Duration.seconds(0.1), new KeyValue(imageView.fitHeightProperty(), initialSize, EaseBoth))
  ).play())

  imageView.setOnMouseEntered(_ => {
    zoomInTimeline.stop()
    zoomInTimeline.play()
  })

  imageView.setOnMouseExited(_ => {
    zoomInTimeline.stop()
    zoomOutTransition.play()
  })
}



