import cats.effect._

import scala.concurrent.duration.DurationInt

object FizzBuzz extends IOApp.Simple{
  val run=
    for{
      ctr<-IO.ref(0)
      wait=IO.sleep(1.seconds)
      poll=wait*>ctr.get
      _<-poll.flatMap(IO.println(_)).foreverM.start
      _<-poll.map(_%3==0).ifM(IO.println("fizz"),IO.unit).foreverM.start
      _<-poll.map(_%5==0).ifM(IO.println("buzz"),IO.unit).foreverM.start
      _<-(wait*>ctr.update(_+1)).foreverM.start
    }yield()
}
