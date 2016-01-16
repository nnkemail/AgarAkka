package model

import akka.actor.ActorRef
import model.Util.Position
import model.Entities._
import java.awt.Color
import model.Entities.EntityType._
import com.mohiva.play.silhouette.api.LoginInfo

sealed abstract class ServerMessage
case class GetUniqueId ()  extends ServerMessage
case class Join ()  extends ServerMessage
//case class TickMove ()  extends ServerMessage
case class GameTick ()  extends ServerMessage
//case class SubscribedID (uID: Int) extends ServerMessage
//case class Goodbye(uID: Int) extends ServerMessage
//case class PlayerData(id: Int, x: Double, y: Double) extends ServerMessage
//case class EntitiesInView(ent: List[PlayerData]) extends ServerMessage
case class SpawnData(uID: Int, initialPosition: Position, worldGrid: WorldGrid, worldActor: ActorRef) extends ServerMessage
case class GiveMeUniqueId() extends ServerMessage
//case class UpdateData(id: Int, x: Double, y: Double, size: Double, R: Int, G: Int, B: Int, isSpiked: Boolean) extends ServerMessage
case class UpdateData(id: Int, x: Double, y: Double, size: Double, R: Int, G: Int, B: Int, eType: Int, name: String) extends ServerMessage
case class RemoveData(id: Int)
case class EntitiesInView(ent: List[Entity]) extends ServerMessage
//case class Eat(id: Int) extends ServerMessage
case class EatCell(c: Cell, eatingEntity: Entity) extends ServerMessage
case class EatFood(f: Food) extends ServerMessage
case class PlayerCellsData(l: List[Cell]) extends ServerMessage
//case class EjectedMass(m: Mass) extends ServerMessage
case class EjectedMass(startPosition: Position, angle: Double, color: Color) extends ServerMessage
case class EatEntity(m: Entity, eatingEntity: Entity) extends ServerMessage
case class PlayerData(id: Int, x: Double, y: Double, size: Double, R: Int, G: Int, B: Int) extends ServerMessage
case class AddMass(mass: Int, eatingEntity: Entity) extends ServerMessage
case class FeedVirus(v: Virus, m: Mass) extends ServerMessage
case class RemoveNode(e: Entity) extends ServerMessage
case class ShootVirus(sourceVirus: Virus) extends ServerMessage
case class SplitCellByVirusCollision(sourceCell: Cell, angle: Double, mass: Int, speed: Double) extends ServerMessage
case class LeaderBoardUpdateTick(players: List[ActorRef]) extends ServerMessage
case class RemoveFromLeaderBoard(entry: LeaderBoardEntry) extends ServerMessage
case class NewLeaderBoard(leaderBoard: List[LeaderBoardEntry]) extends ServerMessage
case class LeaderBoardEntryPacket(id: Int, name: String) extends ServerMessage
case class JoinRoom(idRoom: Int) extends ServerMessage
case class GiveServer(idRoom: Int) extends ServerMessage
case class GiveRooms() extends ServerMessage
case class RoomPacket(id: Int, title: String, lat: Double, lng: Double) extends ServerMessage
case class FriendPacket(name: String, avatar: String) extends ServerMessage
case class JoinChatMap() extends ServerMessage
case class AddNewRoom(title: String, lat: Double, lng: Double) extends ServerMessage
case class AddFacebookFriend(myFacebookID: String, friendFacebookID: String) extends ServerMessage
case class GetFriends(userLoginInfo: LoginInfo) extends ServerMessage
case class AddedFriend(friend: User) extends ServerMessage


