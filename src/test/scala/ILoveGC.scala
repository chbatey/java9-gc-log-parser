object ILoveGC {
  def main(args: Array[String]): Unit = {

    var map = Map[Int, Char]()
    var more = Map[Int, Array[Byte]]()

    for (i <- 1 to Int.MaxValue) {
      map += (i -> i.toChar)
      println(i)
      more += (i -> new Array[Byte](i * 10))
      Thread.sleep(10)
      if (i % 5000 == 0) {
        println("Getting rid of some garbage")
        more = more.filter {
          case (k, v) => k < 2000
        }
      }
    }
  }

}
