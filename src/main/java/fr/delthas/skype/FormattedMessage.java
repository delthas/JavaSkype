package fr.delthas.skype;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class FormattedMessage {
  private static final String PARSING_ERROR_MESSAGE = "Error while parsing formatted message";
  public final String sender;
  public final String receiver;
  public final String type;
  public final Map<String, String> headers;
  public final String body;
  
  private FormattedMessage(String sender, String receiver, String type, Map<String, String> headers, String body) {
    this.sender = sender;
    this.receiver = receiver;
    this.type = type;
    this.headers = headers;
    this.body = body;
  }
  
  public static FormattedMessage parseMessage(String formattedMessage) {
    int firstBlockEnd = formattedMessage.indexOf("\r\n\r\n");
    if (firstBlockEnd == -1) {
      throw new IllegalArgumentException(PARSING_ERROR_MESSAGE);
    }
    int thirdBlockStart = formattedMessage.indexOf("\r\n\r\n", firstBlockEnd + "\r\n\r\n".length()) + "\r\n\r\n".length();
    if (thirdBlockStart == -1) {
      throw new IllegalArgumentException(PARSING_ERROR_MESSAGE);
    }
    int thirdBlockEnd = formattedMessage.indexOf("\r\n\r\n", thirdBlockStart);
    if (thirdBlockEnd == -1) {
      throw new IllegalArgumentException(PARSING_ERROR_MESSAGE);
    }
    String to = extractValue(formattedMessage, "\r\nTo: ", "\r\n", -1, firstBlockEnd);
    if (to == null) {
      throw new IllegalArgumentException(PARSING_ERROR_MESSAGE);
    }
    String from = extractValue(formattedMessage, "\r\nFrom: ", "\r\n", -1, firstBlockEnd);
    if (from == null) {
      throw new IllegalArgumentException(PARSING_ERROR_MESSAGE);
    }
    
    Map<String, String> headers = new HashMap<>();
    int headerStart = thirdBlockStart;
    while (headerStart < thirdBlockEnd) {
      int headerEnd = formattedMessage.indexOf("\r\n", headerStart);
      int middle = formattedMessage.indexOf(": ", headerStart);
      if (middle == -1 || middle >= headerEnd - ": ".length()) {
        throw new IllegalArgumentException(PARSING_ERROR_MESSAGE);
      }
      String key = formattedMessage.substring(headerStart, middle);
      String value = formattedMessage.substring(middle + ": ".length(), headerEnd);
      headers.put(key, value);
      headerStart = headerEnd + 2;
    }
    String body = formattedMessage.substring(thirdBlockEnd + "\r\n\r\n".length());
    return new FormattedMessage(from, to, headers.get("Message-Type"), headers, body);
  }
  
  private static String extractValue(String string, String pre, String post, int min, int max) {
    int preIndex = string.indexOf(pre, min);
    if (preIndex == -1) {
      return null;
    }
    int postIndex = string.indexOf(post, preIndex + pre.length());
    if (postIndex == -1 || postIndex > max) {
      return null;
    }
    return string.substring(preIndex + pre.length(), postIndex);
  }
  
  public static String format(String sender, String receiver, String type, String body, String... headers) {
    String routing = String.format("Routing: 1.0\r\nTo: %s\r\nFrom: %s\r\n", receiver, sender);
    String reliability = "Reliability: 1.0\r\n";
    StringBuilder userHeaders = new StringBuilder();
    for (String header : headers) {
      userHeaders.append(header).append("\r\n");
    }
    String contentLength = String.format("Content-Length: %d\r\n", body.getBytes(StandardCharsets.UTF_8).length);
    String formattedMessage = String.format("%s\r\n%s\r\n%s\r\n%s%s\r\n%s", routing, reliability, type, userHeaders, contentLength, body);
    return formattedMessage;
  }
}
