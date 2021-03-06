package org.telecomsudparis.smap

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.logging.Logger

import org.apache.zookeeper._
import io.grpc.{ManagedChannelBuilder, Status}
import org.imdea.vcd.pb.Proto
import org.imdea.vcd._
import java.net.InetAddress

import com.codahale.metrics.{ConsoleReporter, MetricAttribute}
import nl.grons.metrics4.scala.DefaultInstrumented

import scala.collection.JavaConverters._


class SMapServiceClient(cfg: ClientConfig) extends DefaultInstrumented {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceClient].getName)

  private[this] val rpcTime = metrics.timer("rpcTime")

  val host: String = cfg.host
  val channel =
    ManagedChannelBuilder
      .forAddress(host, cfg.serverPort)
      .usePlaintext(true)
      .build()

  val blockingStub = smapGrpc.blockingStub(channel)

  def shutdown(): Unit = channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)

  /*
  var reporterSettings = scala.collection.JavaConverters.setAsJavaSet(
    Set(MetricAttribute.MAX, MetricAttribute.STDDEV,
      MetricAttribute.M1_RATE, MetricAttribute.M5_RATE,
      MetricAttribute.M15_RATE, MetricAttribute.MIN,
      MetricAttribute.P99, MetricAttribute.P50,
      MetricAttribute.P75, MetricAttribute.P95,
      MetricAttribute.P98, MetricAttribute.P999)
  )

  ConsoleReporter.forRegistry(this.metricRegistry)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MICROSECONDS)
    .disabledMetricAttributes(reporterSettings)
    .build()
    .start(10, TimeUnit.SECONDS) //changed the interval to seconds instead of minutes
  */

  import io.grpc.StatusRuntimeException

  /**
    * Blocking unary call. Calls executeCmd.
    */
  def sendCmd(request: MapCommand): Either[Exception, ResultsCollection] = {
    try {
      val result = rpcTime.time(blockingStub.executeCmd(request))
      Right(result)
    } catch {
      case e: StatusRuntimeException =>
        logger.warning(s"RPC failed:${e.getStatus}")
        Left(e)
    }
  }
}

object SMapServiceClient {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceClient].getName)


  /**
    * Contacts Zookeeper and outputs closest host
    */
  /*
  def zkGetClosest(config: ClientConfig): String = {
    val zkConfig: String = config.zkHost + ":" + config.zkPort
    val timeStamp: String = config.timeStamp
    val root = "/" + timeStamp
    var nodes: List[Proto.NodeSpec] = List()

    try {
      println("SMap - Connecting to: " + zkConfig)

      val zk = new ZooKeeper(zkConfig + "/", 5000, new ZkWatcher())

      for (child <- zk.getChildren(root, null).asScala) {
        val path: String = root + "/" + child
        val data = zk.getData(path, null, null)
        nodes :+= Proto.NodeSpec.parseFrom(data)
      }

      println("SMap: Fetched the Config")
      zk.close()
    } catch {
      case e: KeeperException =>
        e.printStackTrace()
        throw new RuntimeException("SMap: Fatal error, cannot connect to Zk.")
      case e: InterruptedException =>
        e.printStackTrace()
        throw new RuntimeException("SMap: Fatal error, cannot connect to Zk.")
    }

    var min: Long = Long.MaxValue
    var closest: Option[Proto.NodeSpec] = None
    for(n <- nodes) {
      var inet = InetAddress.getByName(n.getIp())
      var start = System.currentTimeMillis()
      for (_ <- 1 to 10) {
        inet.isReachable(5000)
      }
      var delay = System.currentTimeMillis() - start
      if(delay < min){
        min = delay
        closest = Some(n)
      }
    }
    println("SMap: Closest is " + closest.get.toString)

    closest.get.getIp
  }
  */

  /**
    *  Using VCD-java-client to contact Zookeeper and output closest MGB host
    */
  def javaClientGetClosestNode(zkHost: String, zkPort: String): String = {
    val javaConfig = Array("-zk=" + zkHost + ":" + zkPort)
    val hostNode: Proto.NodeSpec = Socket.getClosestNode(Config.parseArgs(javaConfig))
    hostNode.getIp
  }

  /*
  private class ZkWatcher() extends Watcher {
    @Override
    def process(watchedEvent: WatchedEvent): Unit = {
    }
  }
  */
}
