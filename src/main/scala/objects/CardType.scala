package ptcgtool
package objects

object CardType extends Enumeration {
  type CardType = Value
  val TRAINER, POKEMON, ENERGY = Value

  def of(cardSuperType: String): CardType = cardSuperType match {
    case "Pokémon" => POKEMON
    case "Trainer" => TRAINER
    case "Energy" => ENERGY
    case _ => null
  }
}