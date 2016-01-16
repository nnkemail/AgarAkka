package model.Entities

import model.Util.Position
import model.Entities.EntityType._
import akka.actor._
import model.Util.util.settings
import model._
import util.control.Breaks._

case class Mass(ePosition: Position, worldGrid: WorldGrid, worldActor: ActorRef) 
extends MovableEntity (EntityType.MASS, ePosition, worldGrid, worldActor) {
  this.mass = settings.ejectedMass
  
  @Override
  def shouldUpdate() = { true /*_isMoving */ }
  def move() = { bounce(settings.ejectedMassSpeedDecay); updatePositionInGrid(); checkCollisionWithViruses(); }
  
  def checkCollisionWithViruses() = {
    var virusesNearBy = worldGrid.getListOfViruses(this._gridsTakenByEntity)
    var r = 100; // Checking radius
    var topY = this.position.y - r;
    var bottomY = this.position.y + r;
    var leftX = this.position.x - r;
    var rightX = this.position.x + r;
    
    for (virus <- virusesNearBy) {
      breakable {
        //if (virus.collisionCheck(bottomY,topY,rightX,leftX)) {
        if (collisionCheck(virus)) {
          worldActor ! FeedVirus(virus, this)
          break
        }
      }
    }
  }
  
  @Override
  override def equals(other: Any): Boolean =
    other match {
     case that: Mass =>
       super.equals(that) &&
       (that canEqual this) &&
       id == that.id &&
       _typeofEntity == that._typeofEntity
  
       case _ => false
  }
  
  @Override
  override def canEqual(other: Any): Boolean =
    other.isInstanceOf[Mass]
}