package ptcgtool
package frontend

import scalafx.scene.control.Label
import scalafx.scene.layout.VBox

import java.awt.Dimension


def statsTabContent(windowSize: Dimension): VBox =
  new VBox:
    decorate(this, 20, 30)
    children = List(SimpleLabel("Stats Tab Content - WIP", 600))