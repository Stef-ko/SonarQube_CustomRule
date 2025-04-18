package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class AvoidLoggingPasswordsRuleTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidLoggingPasswordsRule.java")
      .withCheck(new AvoidLoggingPasswordsRule())
      .verifyIssues();
  }

}
