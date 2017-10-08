package info.batey

import info.batey.GCLogFileModel.TimeOffset
import info.batey.GcStateModel._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait GcStateJson extends DefaultJsonProtocol {
  implicit val timeOffset: RootJsonFormat[TimeOffset] = jsonFormat1(TimeOffset.apply)
  implicit val size: RootJsonFormat[HeapSize] = jsonFormat2(HeapSize)
  implicit val genSizes: RootJsonFormat[GenerationSizes] = jsonFormat4(GenerationSizes)
  implicit val gc: RootJsonFormat[GcState] = jsonFormat9(GcState)
}
