package com.github.service.base;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is a sample request handler where we want to write business logic.
 * 
 * @author gaurav
 */
public final class ServiceHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final Logger logger = LogManager.getLogger(ServiceHandler.class.getSimpleName());

  @Override
  public void channelRead0(final ChannelHandlerContext context, final FullHttpRequest request)
      throws Exception {
    logger.info(String.format("Received %s message", request.getClass().getSimpleName()));

    if (HttpUtil.is100ContinueExpected(request)) {
      send100Continue(context);
      return;
    }

    BaseServiceUtils.logRequestDetails(logger, context, request);

    // if (need to read) {
    // context.fireChannelRead(request.retain());
    // }
    final HttpMethod method = request.method();
    final String body = request.content() != null ? request.content().toString() : "";
    final String uri = request.uri();
    BaseServiceUtils.logRequestDetails(logger, context, request);

    final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK, Unpooled.copiedBuffer("Echo" + body, CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    BaseServiceUtils.channelResponseWrite(context, request, response, context.voidPromise());
  }

  private static void send100Continue(final ChannelHandlerContext context) {
    final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
    context.write(response);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) {
    logger.error(cause);
    context.close();
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext context) {
    context.flush();
  }

}
