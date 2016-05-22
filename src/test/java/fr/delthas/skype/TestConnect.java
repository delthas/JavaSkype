package fr.delthas.skype;

import org.junit.Assert;

@SuppressWarnings("javadoc")
public class TestConnect {

  // @Test Disable it until a special test account is created
  public void testConnect() {
    try {
      Skype skype = new Skype("username", "password");
      skype.connect();
      Thread.sleep(10000);
      skype.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

}
