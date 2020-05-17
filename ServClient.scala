
import java.net.Socket
import java.io._
import java.util._
import java.net._
import java.io.InterruptedIOException

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.BufferedSource
//import scala.collection.JavaConversions._


import scala.concurrent.{Await, ExecutionContext, Future, Promise, duration}
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import scala.sys.process

class ServClient (val socket : java.net.Socket) {
  protected val bos : BufferedOutputStream = new BufferedOutputStream(socket.getOutputStream)
  protected val scannerBis : Scanner = new Scanner(new BufferedInputStream(socket.getInputStream)).useDelimiter(0.toChar.toString)
  def send(inputData: String,tOut : Option[Int] = None):Option[String] =  {
    tOut match {
      case None => {}
      case Some(i) => socket.setSoTimeout(i)}
    bos.write(inputData.getBytes())
    bos.write(0.toChar)
    try bos.flush() catch {
      case e: Throwable => {
        System.err.print("IOerror by flushing buffer to MaBoSS server, socket may be closed ")
        None
      }
    }
    try Some(scannerBis.next()) catch {
      case e: SocketTimeoutException => println("Timeout server reached"); None
      case e: Throwable => System.err.print(e.toString);None
    }
  }
  def close(): Unit = {socket.close()}
}

/** Factory object
  *
  */
object ServClient {
  /** Construct socket from server host and port
    *
    * @param host
    * @param port
    * @return
    */
  def apply(host: String = "localhost", port: Int): Option[ServClient] = {
      try {
        Some(new ServClient(new java.net.Socket(host, port)))
      } catch {
        //case e: Throwable => {println(e);System.err.print("error trying to connect to port " + port + " and host "+ host);sys.exit(1)}
        case e: Exception => {
          println(e)
          System.err.println("error trying to connect to port " + port + " and host "+ host)
          None
        }
      }
  }
}
