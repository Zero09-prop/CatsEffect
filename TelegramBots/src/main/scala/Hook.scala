import canoe.api._
import canoe.models.messages.TextMessage
import canoe.syntax._
import cats.effect.{ExitCode, IO, IOApp, Sync, Timer}
import fs2.Stream
import io.circe.{HCursor, Json, ParsingFailure, parser}
import scalaj.http.Http

import scala.concurrent.duration.DurationInt
import scala.language.higherKinds
import scala.util.Try

object Hook extends IOApp {

  val botUrl: String = "https://7053-92-244-246-150.ngrok.io"
  val kelvinConst: Double = 273.15

  val botToken: String = "1665267577:AAEuGJXDqzTBRgXqep2u334lt1IKJEg82s4"
  val apiToken = "00f9e3657c5e27452ca64743fd445b89"

  val weatherUrl = "http://api.openweathermap.org/data/2.5/weather"
  val jokeUrl = "https://geek-jokes.sameerkumar.website/api"

  val toCelsius: Double => Double = (t: Double) => t - kelvinConst
  val toJson: String => Either[ParsingFailure, Json] = parser.parse

  def getRequestBodyJson[F[_]: Sync](url: String, p: (String, String)*): Scenario[F, Json] = {
    Scenario.eval(Sync[F].delay(toJson(Http(url).params(p).asString.body).getOrElse(Json.Null)))
  }

  def getTemperature[F[_]: Sync](cursor: HCursor): Scenario[F, Double] =
    Scenario.eval(
      Sync[F].delay(cursor.downField("main").downField("temp").as[Double].map(toCelsius).getOrElse(Double.NaN))
    )
  def getWindSpeed[F[_]: Sync](cursor: HCursor): Scenario[F, Double] =
    Scenario.eval(Sync[F].delay(cursor.downField("wind").downField("speed").as[Double].getOrElse(Double.NaN)))
  def run(args: List[String]): IO[ExitCode] =
    Stream
      .resource(TelegramClient.global[IO](botToken))
      .flatMap { implicit client =>
        Stream
          .resource(Bot.hook[IO](botUrl))
          .flatMap(_.follow(getWeather, alarm, joke))
      }
      .compile
      .drain
      .as(ExitCode.Success)
  def joke[F[_]: TelegramClient: Sync]: Scenario[F, Unit] = {
    for {
      chat <- Scenario.expect(command("joke").chat)
      json <- getRequestBodyJson(jokeUrl, ("format", "json"))
      cursor = json.hcursor
      jok <- Scenario.eval(Sync[F].delay(cursor.downField("joke").as[String].getOrElse(" ")))
      _ <- Scenario.eval(chat.send("Your joke, sir!\n" + jok))
    } yield ()
  }
  def getWeather[F[_]: TelegramClient: Sync]: Scenario[F, Unit] =
    for {
      chat <- Scenario.expect(command("weather").chat)
      city <- Scenario.eval(chat.send("Привет! Введите город: ")) >> Scenario.expect(text).within(10.second)
      json <- getRequestBodyJson(weatherUrl, ("q", city), ("appid", apiToken))
      cursor = json.hcursor
      temperature <- getTemperature(cursor)
      windSpeed <- getWindSpeed(cursor)
      _ <- Scenario.eval(chat.send(s"В $city температура:${temperature.round}°, скорость ветра $windSpeed м/с"))
    } yield ()
  def alarm[F[_]: TelegramClient: Timer]: Scenario[F, Unit] =
    for {
      chat <- Scenario.expect(command("alarm").chat)
      _ <- Scenario.eval(chat.send("Напиши сообщение о котором нужно напомнить"))
      mes <- Scenario.expect(textMessage)
      _ <- Scenario.eval(chat.send("Через сколько минут напомнить?"))
      time <- Scenario.expect(textMessage)
      minute = Try(time.text.toInt).toOption.filter(_ > 0)
      _ <- minute match {
        case Some(i) => setTimer(mes, i)
        case None    => Scenario.eval(time.reply("Время должно быть больше нуля"))
      }
    } yield ()
  def setTimer[F[_]: TelegramClient: Timer](m: TextMessage, i: Int): Scenario[F, Unit] =
    for {
      _ <- Scenario.eval(m.chat.send(s"Таймер установлен. Вы получите напоминание через $i минут"))
      _ <- Scenario.eval(Timer[F].sleep(i.minute))
      _ <- Scenario.eval(m.reply("Время вышло"))
    } yield ()
}
//ngrok http http://localhost:8443 -host-header=localhost:8443
