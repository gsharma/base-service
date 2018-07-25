package com.github.service.base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is a sample request handler where we want to write business logic.
 * 
 * @author gaurav
 */
public final class ServiceHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final Logger logger = LogManager.getLogger(ServiceHandler.class.getSimpleName());
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void channelRead0(final ChannelHandlerContext context, final FullHttpRequest request)
      throws Exception {
    logger.info(String.format("Received %s message", request.getClass().getSimpleName()));

    if (HttpUtil.is100ContinueExpected(request)) {
      send100Continue(context);
      return;
    }

    BaseServiceUtils.logRequestDetails(logger, context, request);

    // TODO: better handling of Supported methods, uri, content-types
    final HttpMethod method = request.method();
    final String uri = request.uri().trim();
    final String contentType = request.headers().get("Content-Type");

    final ByteBuf content = request.content();
    final String body = content != null ? content.toString(StandardCharsets.UTF_8) : "";

    FullHttpResponse response = null;

    if (method == HttpMethod.POST) {
      if (uri.endsWith("service/base") || uri.endsWith("service/base/")) {
        BaseRequest baseRequest = objectMapper.readValue(body, BaseRequest.class);
        if (baseRequest != null) {
          double requestId = baseRequest.getRequestId();
          long clientTstamp = baseRequest.getClientTstampMillis();
          logger.info("Received " + baseRequest);
        }
      }
      response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }

    if (method == HttpMethod.GET) {
      if (uri.endsWith("service/base") || uri.endsWith("service/base/")) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setServerTstampMillis(System.currentTimeMillis());
        String responseJson = objectMapper.writeValueAsString(baseResponse);
        logger.info("Streaming back: " + responseJson);
        ByteBuf statusBytes = context.alloc().buffer();
        statusBytes.writeBytes(responseJson.getBytes());
        response =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, statusBytes);
      }
    }

    if (response == null) {
      response =
          new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
    }

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

    // CORS Headers, if needed - tweak here or use CorsHandler
    // response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    // response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST");

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
