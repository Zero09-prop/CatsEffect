import cats.effect._

object Hello extends IOApp{
  //  def rollDie(n: Int) = IO{
  //    println(s"Rolling $n-side die ...")
  //    Random.nextInt(n) + 1
  //  }
  //
  //  def rollDice = {
  //    List(rollDie(6),rollDie(8), rollDie(12), rollDie(20)).parSequence
  //  }
  //  val a = Semigroup[Int => Int].combine(_ + 1, _ * 10)
  //  println(a(6))
  //  rollDice.unsafeRunSync()
  //  def convertToIO[T](future: => Future[T]): IO[T] = {
  //    IO.async_{ callback =>
  //      future.onComplete{
  //        case Success(value) => callback(Right(value))
  //        case Failure(exception) => callback(Left(exception))
  //      }
  //    }
  //}

  def io(i:Int):IO[Unit]=
    IO{
      if(i==1){Thread.sleep(6000)}
      else
        Thread.sleep(3000)
      println(s"Hi from $i")
    }

  override def run(args:List[String]):IO[ExitCode]={
    //6117
    val time=System.currentTimeMillis()
    for{
      fiber<-io(1).start
      _<-io(2)
      _<-fiber.join
      a=println(System.currentTimeMillis()-time)
    }yield ExitCode.Success
  }
}
