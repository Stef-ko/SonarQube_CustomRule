import java.util.Base64;
import java.nio.charset.StandardCharsets;

class MyClass {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyClass.class);

  MyClass(MyClass mc) { }

  int     foo1() {
    log.error("This is a valid test");
  }

  void    foo2(int value) {
    log.warn("This is another valid test");
  }

  void    foo2(int value) { log.info("This is another valid test"); }

  int     foo3(int value) {
    String password = "PASSWORD";
    log.info("This is not a valid test, because it contains a password: {}", password); // Noncompliant
    return 0;
  }

  Object  foo4(int value) { return null; }

  MyClass foo5(MyClass value) {
    String password = "PASSWORD";
    log.info("This is also not a valid test, because it contains the password {}", password); // Noncompliant
  }

  int     foo6(int value, String name) { return 0; }

  int     foo7(int ... values) { return 0;}

  int     foo8(int value) {
    String password = "PASSWORD";
    log.info( // Noncompliant
            "This is also not a valid test, because it contains the password {}", password);
  }

  int     foo9(String value) {
    log.info( "This is a password {}", Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8))); // Noncompliant
  }

  int     foo10() {
    String password = "PASSWORD";
    log.warn("Password {}", password); // Noncompliant
  }
}
