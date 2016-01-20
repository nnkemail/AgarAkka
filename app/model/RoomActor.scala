package model

import play.libs.Akka
import akka.actor._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import model.Util.util
import model.Util.Position
import model.Entities._
import model.Util.Settings
import model.Util.util.settings

class RoomActor(id: Int) extends Actor {
  val system = ActorSystem("mySystem")
  val worldGrid = WorldGrid()
  val worldActor = context.actorOf(WorldActor.props(this.context.self, worldGrid))
  val leaderBoardActor = context.actorOf(LeaderBoardActor.props(this.context.self))
  var players = HashSet.empty[ActorRef]
  var id: Int = 0
  var workingPlayers: Int = 0;
  var playersData = ListBuffer.empty[Entity]
  var recievedMsg = 0;
  
  system.scheduler.schedule(Duration.Zero, Duration(60, MILLISECONDS))(sendMoveTicks)
  system.scheduler.schedule(Duration.Zero, Duration(800, MILLISECONDS))(sendUpdateLeaderBoardTick)
  println ("Po schedulerze")
  
  def sendMoveTicks() = {
    players.foreach(_ ! GameTick)
    worldActor ! GameTick
  }
  
  def sendUpdateLeaderBoardTick() = {
    leaderBoardActor ! LeaderBoardUpdateTick(players.toList)
  }
  
  def receive = {
    case Join =>
      workingPlayers = workingPlayers + 1;
      players += sender;
      sender ! SpawnData(util.nextSysId(), util.getRandomPosition(), self, worldGrid, worldActor)
      context.watch(sender)
      println("Przyszlo Join");
      
    case Terminated(terminatedActorRef) =>
      players = players - terminatedActorRef;
      workingPlayers = workingPlayers - 1;
      println(players);
  }
}

object RoomActor {
  //val serv = Akka.system.actorOf(Props[Server])    
  //def getActorRef() = { serv }
  def props(id: Int) = Props(new RoomActor(id))
}

