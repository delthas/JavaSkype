package fr.delthas.skype;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;

class LiveConnector {
  private static final Logger logger = Logger.getLogger("fr.delthas.skype.live");
  private static final String SERVER_HOSTNAME = "https://login.live.com";
  private final String username, password;
  private String loginToken;
  private String liveToken;
  private String skypeToken;
  
  public LiveConnector(String username, String password) {
    this.username = username;
    this.password = password;
  }
  
  public synchronized long refreshTokens() throws IOException {
    logger.finer("Refreshing tokens");
    
    Response authorize = Jsoup.connect(SERVER_HOSTNAME + "/oauth20_authorize.srf?client_id=00000000480BC46C&scope=service%3A%3Askype.com%3A%3AMBI_SSL&response_type=token&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf&state=999&locale=en").maxBodySize(100 * 1024 * 1024).timeout(10000).method(Method.GET).ignoreContentType(true).ignoreHttpErrors(true).execute();
    
    String MSPOK = authorize.cookie("MSPOK");
    if (MSPOK == null) {
      IOException e = new IOException("Error while connecting to Live: MSPOK not set.");
      logger.log(Level.SEVERE, "", e);
      throw e;
    }
    
    Elements PPFTs = null;
    for (int i = 0; i < authorize.body().length(); i++) {
      i = authorize.body().indexOf("<input", i);
      if (i == -1) {
        break;
      }
      int j = authorize.body().indexOf(">", i);
      if (j == -1) {
        break;
      }
      PPFTs = Jsoup.parseBodyFragment(authorize.body().substring(i, j + ">".length())).select("input[name=PPFT][value]");
      if (!PPFTs.isEmpty()) {
        break;
      }
    }
    if (PPFTs == null || PPFTs.isEmpty()) {
      IOException e = new IOException("Error while connecting to Live: PPFT not found.");
      logger.log(Level.SEVERE, "", e);
      throw e;
    }
    String PPFT = PPFTs.first().attr("value");
    
    String postUrl = SERVER_HOSTNAME + "/ppsecure/post.srf?client_id=00000000480BC46C&scope=service%3A%3Askype.com%3A%3AMBI_SSL&response_type=token&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf&state=999&locale=en";
    int urlStart = authorize.body().indexOf("urlPost:'");
    if (urlStart != -1) {
      int urlEnd = authorize.body().indexOf('\'', urlStart + "urlPost:'".length());
      if (urlEnd != 1) {
        postUrl = authorize.body().substring(urlStart + "urlPost:'".length(), urlEnd);
      }
    }
    
    Response post = Jsoup.connect(postUrl).data("PPFT", PPFT, "login", username, "passwd", password).cookie("MSPOK", MSPOK).maxBodySize(100 * 1024 * 1024).timeout(10000).method(Method.POST).followRedirects(false).ignoreContentType(true).ignoreHttpErrors(true).execute();
    if (post.statusCode() != 302) {
      int index = post.body().indexOf("sErrTxt:'");
      int end;
      if (index == -1 || (end = post.body().indexOf('\'', index + "sErrTxt:'".length())) == -1) {
        IOException e = new IOException("Error while connecting to Live: not redirected, no reason given.");
        logger.log(Level.SEVERE, "", e);
        throw e;
      }
      IOException e = new IOException("Error while connecting to Live: " + post.body().substring(index + "sErrTxt:'".length(), end));
      logger.log(Level.SEVERE, "", e);
      throw e;
    }
    
    String url = post.header("Location");
    
    int refreshTokenStart = url.indexOf("refresh_token=");
    if (refreshTokenStart == -1) {
      IOException e = new IOException("Error while connecting to Live: refresh token not found.");
      logger.log(Level.SEVERE, "", e);
      throw e;
    }
    int refreshTokenEnd = url.indexOf('&', refreshTokenStart + "refresh_token=".length());
    if (refreshTokenEnd == -1) {
      refreshTokenEnd = url.length();
    }
    String refreshToken = url.substring(refreshTokenStart + "refresh_token=".length(), refreshTokenEnd);
    refreshToken = URLDecoder.decode(refreshToken, StandardCharsets.UTF_8.name());
    
    int accessTokenStart = url.indexOf("access_token=");
    if (accessTokenStart == -1) {
      IOException e = new IOException("Error while connecting to Live: access token not found.");
      logger.log(Level.SEVERE, "", e);
      throw e;
    }
    int accessTokenEnd = url.indexOf('&', accessTokenStart + "access_token=".length());
    if (accessTokenEnd == -1) {
      accessTokenEnd = url.length();
    }
    String accessToken = url.substring(accessTokenStart + "access_token=".length(), accessTokenEnd);
    accessToken = URLDecoder.decode(accessToken, StandardCharsets.UTF_8.name());
    
    int expires = 86400;
    int expiresStart = url.indexOf("expires_in=");
    if (expiresStart != -1) {
      int expiresEnd = url.indexOf('&', expiresStart + "expires_in=".length());
      if (expiresEnd == -1) {
        expiresEnd = url.length();
      }
      try {
        expires = Integer.parseInt(url.substring(expiresStart, expiresEnd));
      } catch (NumberFormatException ignore) {
      }
    }
    
    skypeToken = accessToken;
    loginToken = getToken(refreshToken, "service::login.skype.com::MBI_SSL");
    liveToken = getToken(refreshToken, "service::ssl.live.com::MBI_SSL");
    
    logger.finer("Refreshed live tokens successfully");
    
    return System.nanoTime() + expires * 1000000000L;
  }
  
  private String getToken(String token, String scope) throws IOException {
    Response response = Jsoup.connect(SERVER_HOSTNAME + "/oauth20_token.srf").data("client_id", "00000000480BC46C", "scope", scope, "grant_type", "refresh_token", "refresh_token", token).maxBodySize(100 * 1024 * 1024).timeout(10000).method(Method.POST).ignoreContentType(true).ignoreHttpErrors(true).execute();
    if (response.statusCode() != 200) {
      try {
        JSONObject json = new JSONObject(response.body());
        String errorDescription = json.getString("error_description");
        IOException ex = new IOException("Error while connecting to Live: token request error: " + errorDescription);
        logger.log(Level.SEVERE, "", ex);
        throw ex;
      } catch (JSONException | IllegalFormatException e) {
        IOException ex = new IOException("Error while connecting to Live: unknown token request error: " + response.body());
        logger.log(Level.SEVERE, "", ex);
        throw ex;
      }
    }
    try {
      JSONObject json = new JSONObject(response.body());
      String accessToken = json.getString("access_token");
      return accessToken;
    } catch (JSONException | IllegalFormatException e) {
      IOException ex = new IOException("Error while connecting to Live: failed reading token response: " + response.body());
      logger.log(Level.SEVERE, "", ex);
      throw ex;
    }
  }
  
  public String getLiveToken() {
    return liveToken;
  }
  
  public String getLoginToken() {
    return loginToken;
  }
  
  public String getSkypeToken() {
    return skypeToken;
  }
}
