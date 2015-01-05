package Bitcoins_Proj

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


/*to handle the startmining message from the master*/
case class startMining(no_of_zeros: Integer)

/*to store the bitcoins received from the workers after each worker unit(1000000) is processed*/
case class minedCoins(mined_bitcoins:ArrayBuffer[String])

/*to display the total bitcoins found*/
case class finishedMining(inputsprocessed:Integer)

/*to start 12 workers within the same host*/
case class assignWork(no_of_zeros:Integer)

class Worker extends Actor {

  def receive = {
	/* Concatenation of two random seeds with the gatorlink id 
	   is given as input to SHA256 of MessageDigest library.
	   The found hash is compared for number of zeros and saved in an ArrayBuffer.
	   The mined bitcoins are passed to the master after a work unit of 1000000 is processed.
	   The worker stops computation when the time exceeds 270s
	*/
          
      case startMining(no_of_zeros:Integer) => {
      var mined_bitcoins:ArrayBuffer[String]=  ArrayBuffer[String]()
      var inputsprocessed:Integer=0
      val startingTime=System.currentTimeMillis()
      var seed1:String=Random.alphanumeric.take(6).mkString
      var seed2:String=Random.alphanumeric.take(6).mkString
      while(System.currentTimeMillis()-startingTime<270000){
         var s:String = "keshav92"+seed1+seed2+inputsprocessed
         val sha = MessageDigest.getInstance("SHA-256")
         var bitcoin:String=sha.digest(s.getBytes).foldLeft("")((s:String, b: Byte) => s + Character.forDigit((b & 0xf0) >> 4, 16) +Character.forDigit(b & 0x0f, 16))
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
      /* to connect with the remote master using the received ip address */
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
     /*to start 12 workers within the same host */
     case assignWork(no_of_zeros:Integer) => {
         noofzeros=no_of_zeros
         numberofworkers+=12
         println("invoke the worker")
         val worker =context.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances= 12)))
         for (n <- 1 to 12)
            worker ! startMining(noofzeros)
         }
    /* to store the bitcoins received from the workers 
       after each worker unit(1000000) is processed*/
     case minedCoins(mined_bitcoins:ArrayBuffer[String]) => {

         total_mined_bitcoins++=mined_bitcoins
         println("Worker reported progress about mining")

       }
    /* to start 8 workers in the remote host*/ 
      case "remote" => {
         println("remote worker active")
         numberofworkers+=8
         sender ! startMining(noofzeros)
       }
    /*  the total bitcoins found is displayed if all the 
	workers have finished processing or if time exceeds 
	300s(5 mins).System is shutdown after displaying
        if either condition holds true*/ 
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



/*main object to start the master when the number of 
zeros is given as cmdline argument or to start the 
remote worker if ip address is given as cmdline argument */
object project1 extends App {


  var command_line_args:String=args(0)
  if(command_line_args.contains('.'))
  {
    val worker =ActorSystem("workersystem").actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances= 8)))
    for (n <- 1 to 8)
        worker ! command_line_args
  }
  else
  {
  var no_of_zeros=args(0).toInt
  val system = ActorSystem("master")
  val master = system.actorOf(Props[Master],name="Master")
  master ! assignWork(no_of_zeros)
  println("invoke the master")
  }
}
