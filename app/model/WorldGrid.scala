package model

import scala.Array._
import scala.collection.mutable.HashSet
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.agent.Agent
import scala.collection.mutable.ListBuffer
import model.Util.util

import model.Entities._
import model.Util.Position
import model.Util.util.settings

object WorldGrid {
  def apply() = new WorldGrid
}

class WorldGrid {
  val worldWidth: Int = settings.border.right - settings.border.left
  var worldHeight: Int = settings.border.bottom - settings.border.top
  val gridHeightCount: Int = worldHeight/settings.gridSquareSize
  val gridWidthCount: Int = worldWidth/settings.gridSquareSize
  var _grid = ofDim[Agent[HashSet[Entity]]](gridHeightCount + 1, gridWidthCount + 1);
  var _virusesGrid = ofDim[Agent[HashSet[Virus]]](gridHeightCount + 1, gridWidthCount + 1);
  
  initGrid()
  initVirusesGrid();
  
  def initGrid() = {
    for (y <- 0 to gridHeightCount)
      for (x <- 0 to gridWidthCount)
        _grid(y)(x) = Agent(new HashSet[Entity])
        
  println("initGrid: gridHeightCount" + gridHeightCount + ", " + gridWidthCount)    
  }
  
   def initVirusesGrid() = {
    for (y <- 0 to gridHeightCount)
      for (x <- 0 to gridWidthCount)
        _virusesGrid(y)(x) = Agent(new HashSet[Virus])
        
  println("initVirusesGrid: gridHeightCount" + gridHeightCount + ", " + gridWidthCount)    
  }
  
  def grid_=(newGrid: Array[Array[Agent[HashSet[Entity]]]]) = {_grid = newGrid}
  def grid = _grid
  
  def insert(e: Entity) = {
    e match {
      case e: Food => 
        val gridY: Int = (e.position.y/settings.gridSquareSize).toInt
        val gridX: Int = (e.position.x/settings.gridSquareSize).toInt
        println (gridY + ":" + gridX)
        _grid(gridY)(gridX).send (_ += e)
        
      case e: Cell => 
        var begY: Int = ((e.position.y.toInt - e.getPhysicalSize())/settings.gridSquareSize).toInt
        var endY: Int = ((e.position.y.toInt + e.getPhysicalSize())/settings.gridSquareSize).toInt
        var begX: Int = ((e.position.x.toInt - e.getPhysicalSize())/settings.gridSquareSize).toInt
        var endX: Int = ((e.position.x.toInt + e.getPhysicalSize())/settings.gridSquareSize).toInt
        
        begY = cutYtoBounds(begY)
        endY = cutYtoBounds(endY)
        begX = cutXtoBounds(begX)
        endX = cutXtoBounds(endX)
      
        for (y <- begY to endY)
          for (x <- begX to endX)
           _grid(y)(x).send (_ += e)   
    }
  }
  
  def remove(e: Entity) = {
    e match {
      case e: Food =>
        val gridY: Int = (e.position.y/settings.gridSquareSize).toInt
        val gridX: Int = (e.position.x/settings.gridSquareSize).toInt
        _grid(gridY)(gridX).send (_ -= e)
        
      case e: Cell => 
        var begY: Int = ((e.position.y.toInt - e.getPhysicalSize())/settings.gridSquareSize).toInt
        var endY: Int = ((e.position.y.toInt + e.getPhysicalSize())/settings.gridSquareSize).toInt
        var begX: Int = ((e.position.x.toInt - e.getPhysicalSize())/settings.gridSquareSize).toInt
        var endX: Int = ((e.position.x.toInt + e.getPhysicalSize())/settings.gridSquareSize).toInt
        
        begY = cutYtoBounds(begY)
        endY = cutYtoBounds(endY)
        begX = cutXtoBounds(begX)
        endX = cutXtoBounds(endX)
      
        for (y <- begY to endY)
          for (x <- begX to endX)
            _grid(y)(x).send (_ -= e)
    }   
  }
  
  def getGridCells(topLeft: Position, bottomRight: Position): List[HashSet[Entity]] = {
    var resultGridCells = List.empty
    var begY: Int = (topLeft.y.toInt/settings.gridSquareSize).toInt
    var endY: Int = (topLeft.y.toInt/settings.gridSquareSize).toInt
    var begX: Int = (bottomRight.x.toInt/settings.gridSquareSize).toInt
    var endX: Int = (bottomRight.x.toInt/settings.gridSquareSize).toInt
    
    begY = cutYtoBounds(begY)
    endY = cutYtoBounds(endY)
    begX = cutXtoBounds(begX)
    endX = cutXtoBounds(endX)
    
    for (y <- begY to endY)
      for (x <- begX to endX)
        grid(y)(x)::resultGridCells
    
    resultGridCells
  }
  
  def getListOfEntities(topLeft: Position, bottomRight: Position): List[Entity] = {
    var resultGridCells = HashSet.empty[Entity]
    var begY: Int = (topLeft.y.toInt/settings.gridSquareSize).toInt
    var endY: Int = (bottomRight.y.toInt/settings.gridSquareSize).toInt
    var begX: Int = (topLeft.x.toInt/settings.gridSquareSize).toInt
    var endX: Int = (bottomRight.x.toInt/settings.gridSquareSize).toInt
    
    begY = cutYtoBounds(begY)
    endY = cutYtoBounds(endY)
    begX = cutXtoBounds(begX)
    endX = cutXtoBounds(endX)
    
    for (y <- begY to endY) {
      for (x <- begX to endX) {
        resultGridCells ++= grid(y)(x).get.toList
        resultGridCells ++= _virusesGrid(y)(x).get.toList
      }
    }
    
    resultGridCells.toList
  }
  
  def getListOfViruses(gridCells: List[(Int, Int)]) = {
    var resultGridCells = HashSet.empty[Virus]
    for (coord <- gridCells) {
       resultGridCells ++= _virusesGrid(coord._1)(coord._2).get.toList
    }
       
    resultGridCells.toList.distinct    
  }
  
  def getListOfEntities(gridCells: List[(Int, Int)]) = {
    var resultGridCells = HashSet.empty[Entity]
    for (coord <- gridCells) {
       resultGridCells ++= grid(coord._1)(coord._2).get.toList
       resultGridCells ++= _virusesGrid(coord._1)(coord._2).get.toList
    }
       
    resultGridCells.toList.distinct    
  }
  
  def getHash(e: Entity): List[(Int, Int)] = {
    e match {
      case e: Food => 
        val gridY: Int = (e.position.y/settings.gridSquareSize).toInt
        val gridX: Int = (e.position.x/settings.gridSquareSize).toInt
        List((gridY, gridX))
        
      case e: Mass => 
        val gridY: Int = (e.position.y/settings.gridSquareSize).toInt
        val gridX: Int = (e.position.x/settings.gridSquareSize).toInt
        List((gridY, gridX))  
        
      case e: Virus =>   
        val gridY: Int = (e.position.y/settings.gridSquareSize).toInt
        val gridX: Int = (e.position.x/settings.gridSquareSize).toInt
        println("virus hash :" + List((gridY, gridX)));
        List((gridY, gridX)) 
        
      case e: Cell => 
        var gridCellsNumbers = new ListBuffer[(Int, Int)]()
        var begY: Int = ((e.position.y.toInt - e.getPhysicalSize())/settings.gridSquareSize).toInt
        var endY: Int = ((e.position.y.toInt + e.getPhysicalSize())/settings.gridSquareSize).toInt
        var begX: Int = ((e.position.x.toInt - e.getPhysicalSize())/settings.gridSquareSize).toInt
        var endX: Int = ((e.position.x.toInt + e.getPhysicalSize())/settings.gridSquareSize).toInt
        
        begY = cutYtoBounds(begY)
        endY = cutYtoBounds(endY)
        begX = cutXtoBounds(begX)
        endX = cutXtoBounds(endX)
      
        for (y <- begY to endY)
          for (x <- begX to endX)
            gridCellsNumbers += ((y, x)) 
        
        gridCellsNumbers.toList.distinct
    }    
  }
  
  def getHash(topLeft: Position, bottomRight: Position): List[(Int, Int)] = {
    var resultGridCells = new ListBuffer[(Int, Int)]()
    var begY: Int = (topLeft.y.toInt/settings.gridSquareSize).toInt
    var endY: Int = (topLeft.y.toInt/settings.gridSquareSize).toInt
    var begX: Int = (bottomRight.x.toInt/settings.gridSquareSize).toInt
    var endX: Int = (bottomRight.x.toInt/settings.gridSquareSize).toInt
    
    begY = cutYtoBounds(begY)
    endY = cutYtoBounds(endY)
    begX = cutXtoBounds(begX)
    endX = cutXtoBounds(endX)
    
    for (y <- begY to endY)
      for (x <- begX to endX)
        resultGridCells += ((y, x))
    
    resultGridCells.toList.distinct 
  }
  
  def insert(gridPosition: (Int, Int), e: Entity): Unit = {
    if (gridPosition._1 > gridHeightCount || gridPosition._1 < 0 ||
        gridPosition._2 > gridWidthCount || gridPosition._2 < 0) {
      println("Coordinates out of bounds")
      return
    }
    _grid(gridPosition._1)(gridPosition._2).send (_ += e);
  }
  
  def insertVirus(gridPosition: (Int, Int), e: Virus): Unit = {
    if (gridPosition._1 > gridHeightCount || gridPosition._1 < 0 ||
        gridPosition._2 > gridWidthCount || gridPosition._2 < 0) {
      println("Coordinates out of bounds")
      return
    }
    _virusesGrid(gridPosition._1)(gridPosition._2).send (_ += e);
  }
  
  def insert(gridList: List[(Int, Int)], e: Entity): Unit = {     
    e match {
      case v: Virus => 
        println("insert virus");
        for (gridCell <- gridList)
          insertVirus(gridCell, v)  
          
      case ent: Entity => 
         for (gridCell1 <- gridList)
          insert(gridCell1, ent)    
    }  
  }
  
  def remove(gridPosition: (Int, Int), e: Entity): Unit = {
    if (gridPosition._1 > gridHeightCount || gridPosition._1 < 0 ||
        gridPosition._2 > gridWidthCount || gridPosition._2 < 0) {
      println("Coordinates out of bounds")
      return
    }
    
    _grid(gridPosition._1)(gridPosition._2).send (_ -= e);
  }
  
  def removeVirus(gridPosition: (Int, Int), e: Virus): Unit = {
    if (gridPosition._1 > gridHeightCount || gridPosition._1 < 0 ||
        gridPosition._2 > gridWidthCount || gridPosition._2 < 0) {
      println("Coordinates out of bounds")
      return
    }
    
    _virusesGrid(gridPosition._1)(gridPosition._2).send (_ -= e);
  }
  
  def remove(gridList: List[(Int, Int)], e: Entity): Unit = {
    e match {
      case v: Virus => 
        for (gridCell <- gridList)
          removeVirus(gridCell, v) 
          
      case e: Entity => 
        for (gridCell <- gridList)
          remove(gridCell, e) 
    }
  }
  
  private def cutYtoBounds(y: Int): Int = {
	  if (y < 0) 0
	  else if (y > gridHeightCount) gridHeightCount
	  else y  
	}
	
	private def cutXtoBounds(x: Int): Int = {
	  if (x < 0) 0
	  else if (x > gridWidthCount) gridWidthCount
	  else x  
	}
}