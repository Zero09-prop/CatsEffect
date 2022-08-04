import canoe.api._
import canoe.models.Chat
import canoe.models.messages._
import canoe.syntax._
import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.{catsSyntaxOptionId, toTraverseOps}
import fs2.Stream

import scala.language.higherKinds
object Echo extends IOApp {
  val token: String = "1665267577:AAEuGJXDqzTBRgXqep2u334lt1IKJEg82s4"

  def run(args: List[String]): IO[ExitCode] =
    Stream
      .resource(TelegramClient.global[IO](token))
      .flatMap { implicit client =>
        Bot.polling[IO].follow(echos)
      }
      .compile
      .drain
      .as(ExitCode.Success)

  def echos[F[_]: TelegramClient: Applicative]: Scenario[F, Unit] =
    for {
      msg <- Scenario.expect(any)
      _ <- Scenario.eval(catcher(msg))
    } yield ()
  def catcher[F[_]: TelegramClient: Applicative](msg: TelegramMessage): F[_] =
    msg match {
      case photoMessage: PhotoMessage => photoMessage.photo.headOption.map(msg.chat.send(_)).sequence
      case _                          => msg.chat.send("Wrong data type").some.sequence
    }
  def echoBack[F[_]: TelegramClient: Applicative](msg: TelegramMessage): F[_] =
    msg match {
      case textMessage: TextMessage           => msg.chat.send(textMessage.text)
      case animationMessage: AnimationMessage => msg.chat.send(animationMessage.animation)
      case stickerMessage: StickerMessage     => msg.chat.send(stickerMessage.sticker)
      case photoMessage: PhotoMessage =>
        println(photoMessage.photo.head)
        photoMessage.photo.headOption.map(msg.chat.send(_)).sequence
      //msg.chat.send(photoMessage.photo.head)
      case voiceMessage: VoiceMessage => {
        val chat: Chat = msg.chat
        val user: Option[String] = voiceMessage.from.flatMap(_.username)
        println(user)
        msg.chat.send("hjkjhkjh")
        //user.map((usr: String) => msg.chat.send(usr)).sequence
      }
      case _ => msg.chat.send("Sorry! I can't echos that back.")
    }
}
