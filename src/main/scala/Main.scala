package ptcgtool

import api.CardFetcher.fetchCards
import api.IOTools.clearCache
import api.{Card, CardFetcher}
import objects.Deck
import tabs.{deckBuilderTabContent, statsTabContent}

import javafx.scene.input.ScrollEvent
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
import scalafx.scene.layout.*
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

  private final val SCREEN_SIZE = getDefaultToolkit getScreenSize
  private final val WINDOW_SIZE = Dimension((SCREEN_SIZE.width * 0.8) toInt, (SCREEN_SIZE.height * 0.8) toInt)

  private final val APP_NAME = "PTCG Tool"
  private final val DECK_BUILDING_TAB_NAME = "Deck Builder"
  private final val STATS_TAB_NAME = "Stats"

  private val LIGHT_GRAY = "#000000"
  private val DARK_GRAY = "#2b2b2b"
  private val WHITE = "#ffffff"

  private val TAB_STYLE = s"-fx-background-color: $DARK_GRAY; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;"

  override def start(): Unit =
    println("Starting GUI...")
    new PrimaryStage:
      title = APP_NAME
      scene = new Scene(WINDOW_SIZE.width, WINDOW_SIZE.height):
        val deckBuilderTab: Tab = new Tab:
          style = TAB_STYLE
          val label = new Label(DECK_BUILDING_TAB_NAME):
            style = s"-fx-text-fill: $WHITE; -fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\" "
          graphic = label
          text = "\t"
          content = deckBuilderTabContent(WINDOW_SIZE)
        val statsTab: Tab = new Tab:
          style = TAB_STYLE
          val label = new Label(STATS_TAB_NAME):
            style = s"-fx-text-fill: $WHITE; -fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\" "
          graphic = label
          text = "\t"
          content = statsTabContent(WINDOW_SIZE)
        root = new TabPane:
          tabClosingPolicy = TabPane.TabClosingPolicy.Unavailable
          style = s"-fx-background: $DARK_GRAY"
          tabs = List(deckBuilderTab, statsTab)