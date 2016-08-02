package fr.delthas.skype;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

class WebConnector {

  private static final String SERVER_HOSTNAME = "http://api.skype.com";
  private final Skype skype;
  private final String username, password;
  private String skypeToken;
  private String session;
  private String sessionToken;

  public WebConnector(Skype skype, String username, String password) {
    this.skype = skype;
    this.username = username;
    this.password = password;
  }

  public void start() throws IOException {
    generateTokens();
    updateContacts();
  }

  public void block(User user) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/" + user.getUsername() + "/block", "reporterIp", "127.0.0.1");
  }

  public void unblock(User user) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/" + user.getUsername() + "/unblock");
  }

  public void sendContactRequest(User user, String greeting) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/auth-request/" + user.getUsername(), "greeting", greeting);
  }

  public void acceptContactRequest(ContactRequest contactRequest) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/auth-request/" + contactRequest.getUser().getUsername() + "/accept");
  }

  public void declineContactRequest(ContactRequest contactRequest) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/auth-request/" + contactRequest.getUser().getUsername() + "/decline");
  }

  public void removeFromContacts(User user) throws IOException {
    sendRequest(Method.DELETE, "/users/self/contacts/" + user.getUsername());
  }

  public byte[] getAvatar(User user) throws IOException {
    return sendRequest(Method.GET, "/users/" + user.getUsername() + "/profile/avatar").bodyAsBytes();
  }

  public void updateUser(User user) throws IOException {
    String reponse = sendRequest(Method.GET, "/users/" + user.getUsername() + "/profile/public", "clientVersion", "0/7.12.0.101/").body();
    JSONObject userJSON = new JSONObject(reponse);
    userJSON.put("username", user.getUsername());
    updateUser(userJSON, false);
  }

  private void updateContacts() throws IOException {
    String selfDisplayNameResponse = sendRequest(Method.GET, "/users/self/displayname").body();
    JSONObject selfDisplayNameJSON = new JSONObject(selfDisplayNameResponse);
    skype.getUser(username).setDisplayName(selfDisplayNameJSON.optString("displayname", ""));

    String selfResponse = sendRequest(Method.GET, "/users/self/profile").body();
    JSONObject selfJSON = new JSONObject(selfResponse);
    updateUser(selfJSON, false);

    String response = sendRequest(Method.GET, "/users/self/contacts", "hideDetails", "true").body();
    JSONArray contactsArray = new JSONArray(response);

    List<String> contacts = new ArrayList<>(contactsArray.length());
    for (int i = 0; i < contactsArray.length(); i++) {
      try {
        JSONObject contactObject = contactsArray.getJSONObject(i);
        String contactSkypename = contactObject.getString("skypename");
        if (contactObject.getBoolean("blocked") || !contactObject.getBoolean("authorized") || contactSkypename.equalsIgnoreCase("echo123")) {
          continue;
        }
        contacts.add(contactSkypename);
        skype.addContact(contactSkypename);
      } catch (JSONException e) {
        throw new ParseException(e);
      }
    }

    JSONObject data = new JSONObject();
    JSONArray usernamesArray = new JSONArray();
    for (String username : contacts) {
      usernamesArray.put(username);
    }
    data.put("usernames", usernamesArray);
    String profilesResponse = sendRequestJson(Method.POST, "/users/batch/profiles", data.toString());
    try {
      JSONArray profilesJSON = new JSONArray(profilesResponse);
      for (int i = 0; i < profilesJSON.length(); i++) {
        updateUser(profilesJSON.getJSONObject(i), false);
      }
    } catch (JSONException e) {
      throw new ParseException(e);
    }

    String filterString = contacts.stream().map(u -> "id eq '" + u + "'").collect(Collectors.joining(" or "));
    profilesResponse =
        sendRequest(Method.GET, "https://contacts.skype.com/contacts/v1/users/" + username + "/contacts?$filter=" + filterString, true).body();
    try {
      JSONArray profilesJSON = new JSONObject(profilesResponse).getJSONArray("contacts");
      for (int i = 0; i < profilesJSON.length(); i++) {
        updateUser(profilesJSON.getJSONObject(i), true);
      }
    } catch (JSONException e) {
      throw new ParseException(e);
    }

  }

  private void updateUser(JSONObject userJSON, boolean newContactType) throws ParseException {
    String userUsername;
    String userFirstName = null;
    String userLastName = null;
    String userMood = null;
    String userCountry = null;
    String userCity = null;
    String userDisplayName = null;
    try {
      if (!newContactType) {
        userUsername = userJSON.getString("username");
        userFirstName = userJSON.optString("firstname", null);
        userLastName = userJSON.optString("lastname", null);
        userMood = userJSON.optString("mood", null);
        userCountry = userJSON.optString("country", null);
        userCity = userJSON.optString("city", null);
        userDisplayName = userJSON.optString("displayname", null);
      } else {
        userUsername = userJSON.getString("id");
        userDisplayName = userJSON.optString("display_name", null);
        JSONObject nameJSON = userJSON.getJSONObject("name");
        userFirstName = nameJSON.optString("first", null);
        userLastName = nameJSON.optString("surname", null);
        userMood = userJSON.optString("mood", null);
        if (userJSON.has("locations")) {
          JSONObject locationJSON = userJSON.optJSONArray("locations").optJSONObject(0);
          if (locationJSON != null) {
            userCountry = locationJSON.optString("country", null);
            userCity = locationJSON.optString("city", null);
          }
        }
      }
    } catch (JSONException e) {
      throw new ParseException(e);
    }
    User user = skype.getUser(userUsername);
    user.setCity(getPlaintext(userCity));
    user.setCountry(getPlaintext(userCountry));
    user.setDisplayName(getPlaintext(userDisplayName));
    user.setFirstName(getPlaintext(userFirstName));
    user.setLastName(getPlaintext(userLastName));
    user.setMood(getPlaintext(userMood));
  }

  private void generateTokens() throws IOException {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      // md5 will always be available according to javadoc
      throw new RuntimeException(e);
    }
    byte[] md5hash = md.digest(String.format("%s\nskyper\n%s", username, password).getBytes(StandardCharsets.UTF_8));
    String base64hash = Base64.getEncoder().encodeToString(md5hash);
    String response = sendRequest(Method.POST, "/login/skypetoken", "scopes", "client", "clientVersion", "0/7.12.0.101/", "username", username,
        "passwordHash", base64hash).body();
    try {
      JSONObject jsonResponse = new JSONObject(response);
      if (!jsonResponse.has("skypetoken")) {
        if (jsonResponse.has("status") && jsonResponse.getJSONObject("status").has("text")) {
          throw new IOException("Error while connecting to Skype: " + jsonResponse.getJSONObject("status").getString("text"));
        } else {
          throw new IOException("Unknown error while connecting to Skype: " + jsonResponse.toString());
        }
      }
      skypeToken = jsonResponse.getString("skypetoken");
    } catch (JSONException e) {
      throw new ParseException(e);
    }

    Document doc = Jsoup.connect("https://login.skype.com/login?client_id=578134&redirect_uri=https%3A%2F%2Fweb.skype.com%2F").get();

    Elements pieElements = doc.select("#pie");
    Elements etmElements = doc.select("#etm");
    if (pieElements.isEmpty() || etmElements.isEmpty()) {
      throw new ParseException();
    }

    String pie = pieElements.get(0).val();
    String etm = etmElements.get(0).val();

    Response r = Jsoup.connect("https://login.skype.com/login").ignoreContentType(true).method(Method.POST).data("username", username)
        .data("password", password).data("pie", pie).data("etm", etm).execute();
    session = r.cookie("skype-session");
    sessionToken = r.cookie("skype-session-token");
    if (session == null || sessionToken == null) {
      throw new ParseException("Error while getting skype token: " + r.body());
    }
  }

  private Response sendRequest(Method method, String apiPath, boolean absoluteApiPath, String... keyval) throws IOException {
    Connection conn =
        Jsoup.connect(absoluteApiPath ? apiPath : (SERVER_HOSTNAME + apiPath)).method(method).ignoreContentType(true).ignoreHttpErrors(true);
    if (skypeToken != null && session != null && sessionToken != null) {
      conn.header("X-Skypetoken", skypeToken);
      conn.cookie("skype-session", session);
      conn.cookie("skype-session-token", sessionToken);
    }
    conn.data(keyval);
    return conn.execute();
  }

  private Response sendRequest(Method method, String apiPath, String... keyval) throws IOException {
    return sendRequest(method, apiPath, false, keyval);
  }

  private String sendRequestJson(Method method, String apiPath, String content) throws IOException {
    // can't use jsoup to post simple body because of issue https://github.com/jhy/jsoup/issues/627
    // when https://github.com/jhy/jsoup/pull/734 is merged, replace this with jsoup code
    HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_HOSTNAME + apiPath).openConnection();
    conn.setRequestMethod(method.toString());
    if (skypeToken != null && session != null && sessionToken != null) {
      conn.addRequestProperty("X-Skypetoken", skypeToken);
      conn.addRequestProperty("Cookie", "skype-session=" + session + ";skype-session-token=" + sessionToken);
    }
    conn.addRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);
    try (BufferedOutputStream bos = new BufferedOutputStream(conn.getOutputStream())) {
      bos.write(content.getBytes(StandardCharsets.UTF_8));
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream())) {
      byte[] sink = new byte[1024];
      int n;
      while ((n = bis.read(sink)) != -1) {
        baos.write(sink, 0, n);
      }
    }
    return baos.toString(StandardCharsets.UTF_8.name());
  }

  private static String getPlaintext(String string) {
    if (string == null)
      return null;
    return Jsoup.parseBodyFragment(string).text();
  }

}
