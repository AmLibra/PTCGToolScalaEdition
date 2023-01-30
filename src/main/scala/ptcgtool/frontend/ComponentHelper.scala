package ptcgtool
package frontend

import javafx.animation.{KeyFrame, KeyValue, PauseTransition, Timeline}
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.input.{MouseEvent, ScrollEvent}
import javafx.util.Duration
import ptcgtool.backend.{Card, Deck}
import scalafx.Includes.{jfxHBox2sfx, jfxTask2sfxTask}
import scalafx.animation.Interpolator.EaseBoth
import scalafx.animation.Timeline.Indefinite
import scalafx.animation.{Animation, AnimationTimer, Interpolator}
import scalafx.application.Platform.runLater
import scalafx.collections.ObservableBuffer
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
import scalafx.stage.StageStyle.Undecorated
import scalafx.stage.{Stage, StageStyle}

import java.awt.Dimension
import java.io.FileInputStream
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.collection.parallel.ParSeq
import scala.language.postfixOps
import scala.util.Try

private val SCROLL_SPEED = 1.5

private val EMPTY_CARD = Image(FileInputStream("C:\\Users\\usr\\IdeaProjects\\PTCGToolScalaEdition\\src\\resources\\emptyCard.png"))

private val WHITE = "#ffffff"
val DARK_GRAY = "#2b2b2b"
private val LIGHTER_GRAY = "#3b3b3b"
private val TRANSPARENT = "transparent"

val TAB_STYLE = s"-fx-background-color: $DARK_GRAY; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;"

def backgroundIs(color: String): String = s"-fx-background-color: $color; -fx-background: $color;"

val removeFocus: String = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;"

def textColor(color: String): String = s"-fx-text-fill: $color;"

def textSize(size: Double): String = s"-fx-font-size: $size; -fx-font-weight: bold; -fx-font-family: \"Arial\"; "

// create a pool of threads to run the search task


val BOX_STYLE = s"-fx-background-color: $DARK_GRAY; -fx-control-inner-background: transparent; " +
  "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; " +
  "-fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 0; -fx-border-insets: 0; -fx-border-style: solid; "


def TabLabel(text: String): Label = new Label(text):
  style = textColor(WHITE)

def SimpleSeparator(o: Orientation): Separator =
  new Separator:
    orientation = o
    style = s"-fx-background: $DARK_GRAY; -fx-border-color: transparent; -fx-border-width: 0; "

def SimpleLabel(text: String, size: Double): Label =
  new Label(text):
    style = textSize(size)

def SimpleButton(text: String, eventHandler: EventHandler[_ >: MouseEvent]): Button =
  new Button(text):
    onMouseClicked = eventHandler
    style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;" +
      s"-fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-background-color: $DARK_GRAY; " +
      "-fx-text-fill: white; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 0;"
    onMouseEntered = _ => style = style.value + s"-fx-background-color: $LIGHTER_GRAY;"
    onMouseExited = _ => style = style.value.replace(s"-fx-background-color: $LIGHTER_GRAY;", "")

def SimpleStatusLabel(text: String, size: Double): Label =
  new Label(text):
    style = textSize(size) + textColor(WHITE)


def SimpleToggleButton(text: String, eventHandler: EventHandler[_ >: MouseEvent]): ToggleButton =
  new ToggleButton(text):
    onMouseClicked = eventHandler
    style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;" +
      s"-fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-background-color: $DARK_GRAY; " +
      "-fx-text-fill: white; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 0;"
    onMouseEntered = _ => style = style.value + s"-fx-background-color: $LIGHTER_GRAY;"
    onMouseExited = _ => style = style.value.replace(s"-fx-background-color: $LIGHTER_GRAY;", "")

def SimpleCheckBox(label: String, eventHandler: EventHandler[_ >: MouseEvent]): CheckBox =
  new CheckBox(label):
    onMouseClicked = eventHandler
    style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;" +
      s"-fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-background-color: $DARK_GRAY; " +
      "-fx-text-fill: white; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-radius: 0;"


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
      padding = Insets(0, 20, 0, 20)
      style = backgroundIs(DARK_GRAY)
    content = imageList
    prefViewportWidth = windowSize.width
    prefViewportHeight = windowSize.height * verticalRatio
    hbarPolicy = Never
    vbarPolicy = Never
    fitToWidth = true
    fitToHeight = true
    addEventFilter(Scroll, (event: ScrollEvent) =>
      val deltaY = -(event.getDeltaY * SCROLL_SPEED)
      val width = imageList.width.get - this.getViewportBounds.getWidth
      val hValue = this.getHvalue
      this.setHvalue(hValue - (deltaY / width))
    )

def toSearchResultsView(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): ImageView =
  val imageView = cardImageView(card, 0.3 * windowSize.height, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))
  zoomOnHover(imageView)
  imageView

def toDeckCardView(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): VBox =
  new VBox:
    decorate(this, 10, 10)
    val image: ImageView = cardImageView(card, 0.25 * windowSize.height, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))
    zoomOnHover(image)
    val count: Label = SimpleLabel(deck.countOf(card).toString, 25)
    children = Seq(image, count)

def updateDeckViewPane(deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): Unit =
  runLater {
    getContent(deckViewPane).children = deck.all.par.distinct map (toDeckCardView(_, deck, deckViewPane, windowSize)) seq
  }


def popup(content: Scene): Unit =
  new Stage:
    initModality(ApplicationModal)
    initStyle(StageStyle.Transparent)
    content.setOnMouseClicked(_ => close())
    scene = content
    centerOnScreen()
    show()

def processed(cards: Seq[Card]): ParSeq[Card] =
    val start = System.currentTimeMillis()
    val out = cards.par.groupBy(_.getName) map (_._2.head) toSeq;
    println(s"Processed ${cards.size} in ${System.currentTimeMillis() - start} ms")
    out


def deckViewStatsBox(deck: Deck): VBox =
  new VBox:
    decorate(this, 0, 1)
    val size = 17
    val _padding = 1
    val _spacing = 5
    val pokemonCount: HBox = new HBox:
      decorate(this, _padding, _spacing)
      children = SimpleLabel("Pokemon: ", size) :: SimpleLabel(deck.pokemonCards.size.toString, size) :: Nil
    val trainerCount: HBox = new HBox:
      decorate(this, _padding, _spacing)
      children = SimpleLabel("Trainers: ", size) ::
        SimpleLabel(deck.trainerCards.size.toString, size) :: Nil
    val energyCount: HBox = new HBox:
      decorate(this, _padding, _spacing)
      children = SimpleLabel("Energy: ", size) ::
        SimpleLabel(deck.energyCards.size.toString, size) :: Nil

    children = pokemonCount :: trainerCount :: energyCount :: Nil

def deckManagerCardWindow(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): Unit =
  lazy val cardCount: Label = SimpleLabel(deck.countOf(card).toString, 50)
  cardCount.setStyle("-fx-font-size: 50; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-text-fill: white; " +
    "-fx-effect: dropshadow(gaussian, black, 7, 1, 0, 0);")
  lazy val updateCardCount: Runnable = () => cardCount.text = deck.countOf(card).toString

  def updateDeckViewOptimized(): Unit =
    runLater {
      updateCardCount.run()
      val cardView: VBox = toDeckCardView(card, deck, deckViewPane, windowSize)
      val cardViewIndex: Int = deck.all.par.distinct.indexOf(card)
      getContent(deckViewPane).children.set(cardViewIndex, cardView)
    }

  def deckAdd(card: Card): Unit =
    deck.add(card)
    updateDeckViewOptimized()

  def deckRemove(card: Card): Unit =
    deck.remove(card)
    updateDeckViewOptimized()

  lazy val scene = new Scene {
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
  }

  popup(scene)

def loadDecksSelector(): Unit =
  popup(
    new Scene {
      content = new VBox:
        style = "-fx-background-color: transparent"
        children = Seq(
          SimpleLabel("Select a deck to load", 50),
          new HBox:
            style = "-fx-background-color: transparent"
            children = Seq(
              SimpleButton("Load", _ => ()),
              SimpleButton("Cancel", _ => ())
            )
        )
    }
  )


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

  loadLargeImageTask.valueProperty addListener (_ =>
    if loadLargeImageTask isDone then runLater(imageView.image = loadLargeImageTask.value get)
    )
  loadLargeImageTask.run()
  imageView

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

def zoomOnHover(imageView: ImageView): Unit =
  val initialSize = imageView.getFitHeight
  val maxSize = initialSize * 1.1
  val zoomInTimeline = Timeline(KeyFrame(Duration.seconds(0.05), KeyValue(imageView.fitHeightProperty(), maxSize, EaseBoth)))
  val pauseThenZoomOut = PauseTransition(Duration.seconds(0.1))
  pauseThenZoomOut.setOnFinished(_ =>
    Timeline(KeyFrame(Duration.seconds(0.3), KeyValue(imageView.fitHeightProperty(), initialSize, EaseBoth))).play()
  )
  imageView.setOnMouseEntered(_ => zoomInTimeline.play())
  imageView.setOnMouseExited(_ => pauseThenZoomOut.play())



