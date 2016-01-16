package model.Entities

import model.Util.Position;
import akka.actor._
import model.Util.util
import model._

case class Food(ePosition: Position, worldGrid: WorldGrid) 
extends Entity(EntityType.FOOD, ePosition, worldGrid) {
    this.mass = 1;     
    
    @Override
    def shouldUpdate = false
}