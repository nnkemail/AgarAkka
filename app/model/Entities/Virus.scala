package model.Entities

import model.Util.Position
import model.Entities.EntityType._
import akka.actor._
import model.Util.util.settings
import model._
import java.awt.Color

case class Virus(ePosition: Position, worldGrid: WorldGrid, worldActor: ActorRef) 
extends MovableEntity (EntityType.VIRUS, ePosition, worldGrid, worldActor) {
  var fed: Int = 0
  this.mass = settings.virusStartMass
  this.isSpiked = true
  this.color = new Color(0, 255, 0)
  println("Virus" + this.entityType.id);

 def feed(feeder: Mass) = {
    this.moveAngle = feeder.moveAngle; // Set direction if the virus explodes
    worldActor.!(EatEntity(feeder, this))(worldActor)
  }
  
  @Override 
  override def addMass(newMass: Int) = {
    super.addMass(newMass)
    this.fed = this.fed + 1;
    println("virus add mass");
    if (this.fed >= settings.virusFeedAmount) {
        this.mass = settings.virusStartMass; // Reset mass
        this.fed = 0;
        this.color = new Color(0, 255, 0)
        worldActor.!(ShootVirus(this))(worldActor);
    } else {
      this.color = new Color (0, this.color.getGreen() - 20, 0)
    }
  }
  
  @Override
  def shouldUpdate() = { true }
  def move() = { 
    bounce(settings.ejectedVirusSpeedDecay); 
    updatePositionInGrid() }
  
  @Override
  override def equals(other: Any): Boolean =
    other match {
     case that: Virus =>
       super.equals(that) &&
       (that canEqual this) &&
       id == that.id &&
       _typeofEntity == that._typeofEntity
  
       case _ => false
  }
  
  @Override
  override def canEqual(other: Any): Boolean =
    other.isInstanceOf[Virus]
}