package model.Entities

import akka.actor._
import model.Util.util
import model.Entities.EntityType._
import model.Util.Position
import model._

abstract class MovableEntity(eType: EntityType, ePosition: Position, worldGrid: WorldGrid, worldActor: ActorRef) 
extends Entity (eType, ePosition, worldGrid) with BallPhysics {
  protected var _moveAngle = 0D;
  protected var _speed = 0D
  protected var _isMoving = if (_speed < 0.05) false else true;
  
  def speed = { _speed }
  def speed_=(newSpeed: Double) = { _speed = newSpeed } 
  def moveAngle = { _moveAngle }
  def moveAngle_=(newAngle: Double) = { _moveAngle = newAngle }
  
  def move();
  
  def updatePositionInGrid() = {
    /*
    var newGrids: List[(Int, Int)] = worldGrid.getHash(this)
    var oldGridsDiff = _gridsTakenByEntity diff newGrids
    var newGridsDiff = newGrids diff _gridsTakenByEntity
    worldGrid.remove(oldGridsDiff, this)
    worldGrid.insert(newGridsDiff, this)
    _gridsTakenByEntity = newGrids */
    
    var newGrids: List[(Int, Int)] = worldGrid.getHash(this)
    var gridsToRemove = _gridsTakenByEntity filterNot (x => newGrids contains x)
    //var gridsToAdd = newGrids diff (_gridsTakenByEntity intersect newGrids)
    //var intersection = _gridsTakenByEntity filter (x => newGrids contains x)
    //var gridsToAdd  = newGrids filterNot (y => intersection contains y)
    worldGrid.remove(gridsToRemove, this)
    worldGrid.insert(newGrids, this)
    _gridsTakenByEntity = newGrids
  }
  
  @Override
  override def onRemove() = {
    this.updatePositionInGrid()
    super.onRemove()
  }
}