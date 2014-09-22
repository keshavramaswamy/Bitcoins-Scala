import java.security.MessageDigest

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import akka.actor._
import akka.actor.Actor
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.RoundRobinRouter



case class startMining(no_of_zeros: Integer)
case class finishedMining(inputsprocessed:Integer)
case class minedCoins(mined_bitcoins:ArrayBuffer[String])
case class assignWork(no_of_zeros:Integer)
class Worker extends Actor {

  def receive = {
      case startMining(no_of_zeros:Integer) => {
      var mined_bitcoins:ArrayBuffer[String]=  ArrayBuffer[String]()
      var inputsprocessed:Integer=0
      val startingTime=System.currentTimeMillis()
      var seed1:String=Random.alphanumeric.take(6).mkString
      var seed2:String=Random.alphanumeric.take(6).mkString
      while(System.currentTimeMillis()-startingTime<270000){

 
         var s:String = "keshav92"+seed1+seed2+inputsprocessed
         val sha = MessageDigest.getInstance("SHA-256")
         var bitcoin:String=sha.digest(s.getBytes).foldLeft("")((s:
String, b: Byte) => s + Character.forDigit((b & 0xf0) >> 4, 16) +
Character.forDigit(b & 0x0f, 16))
         var extracted_val:String=bitcoin.substring(0,no_of_zeros)
         var comparison_val="0"*no_of_zeros
         if(extracted_val.equals(comparison_val)){
            mined_bitcoins+="keshav92"+seed1+seed2+inputsprocessed+" "+bitcoin
            }
         inputsprocessed+=1
         if(inputsprocessed%1000000==0)

           sender ! minedCoins(mined_bitcoins)
        }
           sender ! finishedMining(inputsprocessed)
      }
      case ipaddress:String => {
        println("inside remote worker")
        val master=context.actorSelection("akka.tcp://master@"+ipaddress+":5152/user/Master")
        master! "remote"
      }

  }
}



class Master extends Actor {

  private var noofzeros:Integer=0
  private var noofbitcoins:Integer = 0
  private var workernumber:Integer=0
  private var total_inputsprocessed:Integer=0
  private var numberofworkers:Integer=0
  private var total_mined_bitcoins:ArrayBuffer[String]=  ArrayBuffer[String]()
   def receive = {
     case assignWork(no_of_zeros:Integer) => {
         noofzeros=no_of_zeros
         numberofworkers+=12
         println("invoke the worker")
         val worker =
context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances
= 12)))
         for (n <- 1 to 12)
            worker ! startMining(noofzeros)
         }
     case minedCoins(mined_bitcoins:ArrayBuffer[String]) => {

         total_mined_bitcoins++=mined_bitcoins
         println("Worker reported progress about mining")

       }
      case "remote" => {
         println("remote worker active")
         numberofworkers+=8
         sender ! startMining(noofzeros)
       }
      case finishedMining(inputsprocessed:Integer) =>
        {
          workernumber+=1
          total_inputsprocessed+=inputsprocessed
          if(workernumber == numberofworkers)
         {
           println("Number of workers : "+numberofworkers)
           println("Number of inputs processed : "+total_inputsprocessed)
           total_mined_bitcoins=total_mined_bitcoins.distinct
           for(i<- 0 until total_mined_bitcoins.length )
          println((i+1)+" " + total_mined_bitcoins(i))
           println("Number of bitcoins found : "+total_mined_bitcoins.length )
           context.system.shutdown()
         }
        }

    }
   }




object project1 extends App {


  var command_line_args:String=args(0)
  if(command_line_args.contains('.'))
  {
    val worker =
ActorSystem("workersystem").actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances
= 10)))
    for (n <- 1 to 12)
        worker ! command_line_args
  }
  else
  {
  var no_of_zeros=args(0).toInt
  //val master = system.actorOf(Props[Master],name="Master")
  val system = ActorSystem("master")
  val master = system.actorOf(Props[Master],name="Master")
  master ! assignWork(no_of_zeros)
  println("invoke the master")
  }
}
