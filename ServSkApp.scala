object ServSkApp extends App {
  override def main(args: Array[String]): Unit = {
    if (args.length < 3) println("not enough arguments") else {
      val quServ = new ScQueueServer(args(0).toInt, args(1), args(2).toInt)
      println("Start server")
      println("Type q to quit")
      var loop = true
      while (loop) {
        scala.io.StdIn.readLine("") match {
          case "q" => {
            quServ.close()
            loop = false
          }
          case _ => {println("Working")}
        }
      }
    }
  }
}