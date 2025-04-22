package org.sonar.samples.java.checks;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.*;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidLoggingPasswordsRule")
public class AvoidLoggingPasswordsRule extends IssuableSubscriptionVisitor {

  private static final Logger log = LoggerFactory.getLogger(AvoidLoggingPasswordsRule.class);

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION); // returns list with all method invocation
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
    ExpressionTree methodSelect = methodInvocation.methodSelect();

    if (methodSelect instanceof MemberSelectExpressionTree) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
      String methodName = memberSelect.identifier().name();
      String expression = memberSelect.expression().toString();

      // Checks, if invoked method is log.*
      if (expression.matches(".*log$")) {
        List<ExpressionTree> arguments = methodInvocation.arguments();

        // Iterate over method arguments of log.
        for (ExpressionTree arg : arguments) {
          if (containsEncodedPassword(arg)) {
            reportIssue(methodInvocation, "Don't log passwords");
          }
        }
      }
    }
  }

  private boolean containsEncodedPassword(ExpressionTree tree) {
    if (tree == null) return false;

    return containsPasswordIdentifier(tree);
  }

  private boolean containsPasswordIdentifier(Tree tree) {
    // check if tree contains password
    if (tree instanceof IdentifierTree) {
      String name = ((IdentifierTree) tree).name().toLowerCase();
      return name.contains("password") || name.contains("pwd") || name.contains("pass");
    }

    // Recurse into method invocations like Base64.getEncoder().encodeToString(...)
    if (tree instanceof MethodInvocationTree) {
      MethodInvocationTree method = (MethodInvocationTree) tree;
      for (ExpressionTree arg : method.arguments()) {
        if (containsPasswordIdentifier(arg)) {
          return true;
        }
      }
      return containsPasswordIdentifier(method.methodSelect());
    }

    // Recurse into member selections like password.getBytes()
    if (tree instanceof MemberSelectExpressionTree) {
      MemberSelectExpressionTree member = (MemberSelectExpressionTree) tree;
      return containsPasswordIdentifier(member.expression());
    }

    // Handle type casts, parentheses, etc
    if (tree instanceof UnaryExpressionTree) {
      return containsPasswordIdentifier(((UnaryExpressionTree) tree).expression());
    }

    if (tree instanceof ParenthesizedTree) {
      return containsPasswordIdentifier(((ParenthesizedTree) tree).expression());
    }

    return false;
  }
}


//          String code = arg.toString().toLowerCase();
////          log.info("INFO===================");
////          log.info("Code: {}", code);
////          log.info("code.contains(\"password\") || code.contains(\"pwd\") || code.contains(\"pass\"): {}", code.contains("password") || code.contains("pwd") || code.contains("pass"));
//
//
//          if (code.contains("password") || code.contains("pwd") || code.contains("pass")) {
//            res = true;
//          }
//        }
//        if (res == true){
//          reportIssue(methodInvocation, "Don't log passwords, CUNT!");
//        }
//      }
//    }
//  }
//}
