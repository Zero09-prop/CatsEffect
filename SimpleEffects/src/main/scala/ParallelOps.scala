import cats.effect._

object ParallelOps extends IOApp{

  override def run(args:List[String]):IO[ExitCode]={
    val list= 1 to 10000
    val time=System.currentTimeMillis()
    for{
      fiber<-IO(list.filter(_%2==0).sum).start
      odd<-IO(list.filter(_%2!=0).sum)
      even<-fiber.joinWithNever
      a=println(s"Sum is ${odd + even}. Time is ${System.currentTimeMillis() - time}")
    }yield ExitCode.Success

    for{
      b<-IO(list.sum)
      _=println(s"Sum is ${b}. Time is ${System.currentTimeMillis() - time}\n")
    }yield ExitCode.Success
  }
}
