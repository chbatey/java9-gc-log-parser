package info.batey

import com.typesafe.config.{Config, ConfigFactory}

sealed trait OutputMode
case object HttpMode extends OutputMode
case object ConsoleMode extends OutputMode

sealed trait StreamMode
case object OneShot extends StreamMode
case object Stream extends StreamMode

class Conf(config: Config) {
  val outputMode: OutputMode = Option(config.getString("gc.output"))
    .map(_.toLowerCase)
    .map(str => if (str.eq("http")) HttpMode else ConsoleMode)
    .getOrElse(ConsoleMode)

  val streamMode: StreamMode = Option(config.getString("gc.stream"))
    .map(_.toLowerCase)
    .map(str => if (str.eq("stream")) Stream else OneShot)
    .getOrElse(OneShot)

  val filePath: String = config.getString("gc.file")
  val httpHost: String = config.getString("gc.http.host")
  val httpPort: Int = config.getInt("gc.http.port")
}

object Conf {
  def apply(config: Config = ConfigFactory.load()): Conf = new Conf(config)
}
