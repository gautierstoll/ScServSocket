import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.{ServerSocket, SocketTimeoutException}
import java.util.Scanner


class ScQueueServer(val port: Int, val socketHostName: String, val socketPort: Int) {
  val server = new ServerSocket(port)
  val queueSim = scala.collection.mutable.ListBuffer[(String, Thread)]()
  val logStr = new scala.collection.mutable.StringBuilder
  var isRunning = true

  private def bosWrite(bs: BufferedOutputStream, str: String): Unit = {
    bs.write(str.getBytes())
    bs.write(0.toChar)
    try bs.flush() catch {
      case e: Throwable => {
        System.err.print("IOerror by flushing buffer, socket may be closed ")
      }
    }
  }
  val mainTh:Thread = new Thread() {
    override def run(): Unit = {
      while (isRunning) {
        val commSocket = server.accept()
        val bos: BufferedOutputStream = new BufferedOutputStream(commSocket.getOutputStream)
        val scannerBis: Scanner = new Scanner(new BufferedInputStream(commSocket.getInputStream)).useDelimiter(0.toChar.toString)
        try {
          val receivedStr = scannerBis.next()
          "[^\n]*".r.findFirstIn(receivedStr) match {
            case Some("@queue") => {
              queueSim.map(_._1).toString
              bosWrite(bos, queueSim.map(_._1).mkString("\n"))
              commSocket.close()
            }
            case Some("@log") => {
              bosWrite(bos, logStr.toString)
              commSocket.close()
            }
            case Some(jobName) => {
              val mbssInput = "[^\n]*\n".r.replaceFirstIn(receivedStr, "")
              this.synchronized {
                val oPrecThread: Option[(String, Thread)] = queueSim.lastOption
                val thSc: Thread = new Thread() {
                  override def run(): Unit = {
                    oPrecThread match {
                      case Some((pJobName, pThread)) => {
                        println(jobName + " wait until " + pJobName + " is done\n")
                        logStr.++=(jobName + " wait until " + pJobName + " is done\n")
                        pThread.join()
                        queueSim.remove(0)
                      }
                      case None => {}
                    }
                    println("Send " + jobName + " to MaBoSS Server\n")
                    logStr ++= ("Send " + jobName + " to MaBoSS Server\n")
                    ServClient(socketHostName, socketPort) match {
                      case Some(sCli) => {
                        sCli.send(mbssInput) match {
                          case Some(str) => {
                            println(jobName + " is done\n")
                            logStr.++=(jobName + " is done\n")
                            bosWrite(bos, str)
                          }
                          case None => {
                            bosWrite(bos, "no server MaBoSS")
                          }
                        }
                      }
                      case _ => None
                    }
                    commSocket.close()
                  }
                }
                thSc.start()
                queueSim.+=((jobName, thSc))
              }
            }
            case None => {}
          }
        } catch {
          case e: SocketTimeoutException => println("Timeout server reached");
            None
          case e: Throwable => System.err.print(e.toString);
            None
        }
      }
    }
  }
  mainTh.start()
  def close():Unit = { isRunning = false
  server.close()
  }
  }

