import cats.effect._
import cats.implicits._

import java.io.{File, FileInputStream, FileOutputStream, InputStream, OutputStream}
import scala.io.StdIn

object CopyFile extends IOApp {
  def copy[F[_]: Sync](origin: File, destination: File, bufferSize: Int): F[Long] =
    inputOutputStream(origin, destination).use {
      case (in, out) =>
        transfer(in, out, bufferSize)
    }
  def inputStream[F[_]: Sync](f: File): Resource[F, FileInputStream] =
    Resource.make {
      Sync[F].blocking(new FileInputStream(f))
    } { inStream =>
      Sync[F].blocking(inStream.close()).handleErrorWith(_ => Sync[F].unit)
    }
  def outputStream[F[_]: Sync](f: File): Resource[F, FileOutputStream] =
    Resource.make {
      Sync[F].blocking(new FileOutputStream(f))
    } { outStream =>
      Sync[F].blocking(outStream.close()).handleErrorWith(_ => Sync[F].unit)
    }
  def inputOutputStream[F[_]: Sync](in: File, out: File): Resource[F, (InputStream, OutputStream)] = {
    for {
      inStream <- inputStream(in)
      outStream <- outputStream(out)
    } yield (inStream, outStream)
  }
  def transmit[F[_]: Sync](origin: InputStream, destination: OutputStream, buffer: Array[Byte], acc: Long): F[Long] =
    for {
      amount <- Sync[F].blocking(origin.read(buffer, 0, buffer.length))
      count <-
        if (amount > -1)
          Sync[F].blocking(destination.write(buffer, 0, buffer.length)) >> transmit(
            origin,
            destination,
            buffer,
            acc + amount
          )
        else Sync[F].pure(acc)
    } yield count

  def transfer[F[_]: Sync](origin: InputStream, destination: OutputStream, bufferSize: Int): F[Long] =
    transmit(origin, destination, new Array[Byte](bufferSize), 0L)
  def checkFiles(f1: File, f2: File): IO[Boolean] =
    IO {
      if (f1 == f2)
        false
      else {
        if (f2.exists()) {
          val input = StdIn.readLine(s"File ${f2.getPath} exist. Do you wanna really overwrite this file? y/n\n")
          if (input == "y") true
          else false
        } else true
      }
    }
  def readBufferSize(): IO[Int] = IO(StdIn.readLine("Enter the buffer size\n").toInt)
  def work(check: Boolean,origin: File,destination: File): IO[Unit] = {
    if(check){
      readBufferSize()
        .flatMap(copy[IO](origin, destination, _))
        .flatMap(count => IO.println(s"$count bytes copied from ${origin.getPath} to ${destination.getPath}"))
    }
    else
      IO.println("Copy denied")
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO.unit
      orig = new File("build.sbt")
      dest = new File("text.txt")
      correct <- checkFiles(orig, dest) //IO[Boolean]
      _ <- work(correct,orig,dest)
    } yield ExitCode.Success
}
