package fr.delthas.skype;

import org.junit.Assert;

@SuppressWarnings({"javadoc", "static-method"})
public class TestConnect {

  // @Test (Disable it until a special test account is created)
  public void testConnect() {
    Skype skype = new Skype("username", "password");
    try {
      skype.connect();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    skype.setErrorListener(e -> {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    });
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e1) {
    }
    skype.disconnect();
  }

}
