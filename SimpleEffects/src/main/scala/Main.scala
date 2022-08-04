import cats.effect._

object Main extends IOApp{

  val ioInt:IO[Int]=IO(59).map{value=>println(Thread.currentThread().getName,value);value}
  val ioStr:IO[String]=IO("Success")
  val fiber:IO[FiberIO[String]]=ioStr.map{value=>println(Thread.currentThread().getName,value);value}.start
  override def run(args:List[String]):IO[ExitCode]={
    for{
      _<-fiber
      _<-ioInt
    }yield ExitCode.Success
  }
}
