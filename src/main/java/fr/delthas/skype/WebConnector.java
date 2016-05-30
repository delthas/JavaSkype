package fr.delthas.skype;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

  private static final String SERVER_HOSTNAME = "https://api.skype.com";
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
    updateUser(userJSON);
  }

  private void updateContacts() throws IOException {
    String selfDisplayNameResponse = sendRequest(Method.GET, "/users/self/displayname").body();
    JSONObject selfDisplayNameJSON = new JSONObject(selfDisplayNameResponse);
    skype.getUser(username).setDisplayName(selfDisplayNameJSON.optString("displayname", ""));

    String selfResponse = sendRequest(Method.GET, "/users/self/profile").body();
    JSONObject selfJSON = new JSONObject(selfResponse);
    updateUser(selfJSON);

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
      } catch (JSONException e) {
        throw new ParseException(e);
      }
    }

    // we need to know the actual array size
    // so we can't do everything in one loop
    // (some contacts may be blocked and their details won't be fetched)

    String[] data = new String[contacts.size() * 2];
    for (int i = 0; i < contacts.size(); i++) {
      data[2 * i] = "contacts[]";
      data[2 * i + 1] = contacts.get(i); // calling get repeteadly is okay because it's an arraylist
    }
    String profilesResponse = sendRequest(Method.POST, "/users/self/contacts/profiles", data).body();
    try {
      JSONArray profilesJSON = new JSONArray(profilesResponse);
      for (int i = 0; i < profilesJSON.length(); i++) {
        User user = updateUser(profilesJSON.getJSONObject(i));
        skype.addContact(user.getUsername());
      }
    } catch (JSONException e) {
      throw new ParseException(e);
    }
  }

  private User updateUser(JSONObject userJSON) throws ParseException {
    String userUsername = userJSON.optString("username", null);
    if (userUsername == null) {
      throw new ParseException();
    }
    String userFirstName = userJSON.optString("firstname", null);
    String userLastName = userJSON.optString("lastname", null);
    String userMood = userJSON.optString("mood", null);
    String userCountry = userJSON.optString("country", null);
    String userCity = userJSON.optString("city", null);
    String userDisplayName = userJSON.optString("displayname", null);
    User user = skype.getUser(userUsername);
    user.setCity(userCity);
    user.setCountry(userCountry);
    user.setDisplayName(userDisplayName);
    user.setFirstName(userFirstName);
    user.setLastName(userLastName);
    user.setMood(userMood);
    return user;
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
      skypeToken = new JSONObject(response).getString("skypetoken");
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

  private Response sendRequest(Method method, String apiPath, String... keyval) throws IOException {
    Connection conn = Jsoup.connect(SERVER_HOSTNAME + apiPath).method(method).ignoreContentType(true).ignoreHttpErrors(true);
    if (skypeToken != null && session != null && sessionToken != null) {
      conn.header("X-Skypetoken", skypeToken);
      conn.cookie("skype-session", session);
      conn.cookie("skype-session-token", sessionToken);
    }
    conn.data(keyval);
    return conn.execute();
  }
}
