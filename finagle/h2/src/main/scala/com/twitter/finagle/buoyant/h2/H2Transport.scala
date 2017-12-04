package com.twitter.finagle.buoyant.h2

import com.twitter.io.Buf
import com.twitter.util.{Closable, Future, Time}
import java.net.SocketAddress

import io.netty.handler.codec.http2.Http2FrameStream

object H2Transport {

  /**
   * A codec-agnostic interface supporting writes of H2 messages to a transport.
   */
  trait Writer {

    def localAddress: SocketAddress
    def remoteAddress: SocketAddress

    def write(stream: Http2FrameStream, orig: Headers, eos: Boolean): Future[Unit]
    def write(stream: Http2FrameStream, buf: Buf, eos: Boolean): Future[Unit]
    def write(stream: Http2FrameStream, frame: Frame): Future[Unit]

    /**
     * Update the flow control window by `incr` bytes.
     */
    def updateWindow(stream: Http2FrameStream, incr: Int): Future[Unit]

    /**
     * Write a stream reset.
     */
    def reset(stream: Http2FrameStream, err: Reset): Future[Unit]

    /**
     * Write a GO_AWAY frame and close the connection..
     */
    def goAway(err: GoAway, deadline: Time): Future[Unit]

    final def goAway(err: GoAway): Future[Unit] = goAway(err, Time.Top)
  }
}
