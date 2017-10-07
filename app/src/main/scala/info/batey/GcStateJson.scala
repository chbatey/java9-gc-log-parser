package info.batey

import info.batey.GCLogFileModel.TimeOffset
import info.batey.actors.GcStateActor.{GcState, GenerationSizes, HeapSize}
import info.batey.actors.PauseActor.TotalPause
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait GcStateJson extends DefaultJsonProtocol {
  implicit val timeOffset: RootJsonFormat[TimeOffset] = jsonFormat1(TimeOffset.apply)
  implicit val totalPause: RootJsonFormat[TotalPause] = jsonFormat1(TotalPause)
  implicit val size: RootJsonFormat[HeapSize] = jsonFormat2(HeapSize)
  implicit val genSizes: RootJsonFormat[GenerationSizes] = jsonFormat4(GenerationSizes)
  implicit val gc: RootJsonFormat[GcState] = jsonFormat9(GcState)
}
