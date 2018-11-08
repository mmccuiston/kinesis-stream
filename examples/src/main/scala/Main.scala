import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import px.kinesis.stream.consumer.Consumer

object Main extends App {

  implicit val system = ActorSystem("kinesis-source")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()
  implicit val logging: LoggingAdapter = Logging(system, "Example")

  // A simple consumer that will print to the console for now
  val consumer = Sink.foreach[String](s => logging.info(s))

  val streamName = "activity-test"
  val appName = "test-kcl-3"

  // Attach a MergeHub Source to the consumer. This will materialize to a
  // corresponding Sink.
  val runnableGraph =
    Consumer
      .source(streamName, appName)
      .take(10)
      .mapAsyncUnordered(1)(r => r.markProcessed().map(_ => r))
      .map(r =>
        s"${r.sequenceNumber.takeRight(10)} /${r.shardId} - ${r.data.utf8String}")
      .toMat(consumer)(Keep.left)

  val done = runnableGraph.run()
  done.onComplete(_ => {
    logging.info("Shutdown completed")
    system.terminate()
  })
}