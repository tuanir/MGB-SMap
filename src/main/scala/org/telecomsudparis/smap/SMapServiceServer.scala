package org.telecomsudparis.smap

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}

class SMapServiceServer(server: Server) {
  val logger: Logger = Logger.getLogger(classOf[SMapServiceServer].getName)

  def start(): Unit = {
    server.start()
    logger.info(s"MGB-SMap Server started, listening on ${server.getPort}")
    sys.addShutdownHook {
      // Use stderr here since the logger may has been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      stop()
      System.err.println("*** server shut down")
    }
    ()
  }
  def stop(): Unit = {
    server.shutdown()
  }

  /**
    * Await termination on the main thread since the grpc library uses daemon threads.
    */
  def blockUntilShutdown(): Unit = {
    server.awaitTermination()
  }

}

object SMapServiceServer extends App {
  //TODO: get server parameters from properties
  val server = new SMapServiceServer(
    ServerBuilder
      .forPort(8980)
      .addService(
        smapGrpc.bindService(
          new SMapService[String](Array("")),
          scala.concurrent.ExecutionContext.global
        )
      )
      .build()
  )
  server.start()
  server.blockUntilShutdown()
}
