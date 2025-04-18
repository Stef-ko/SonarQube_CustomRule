package org.sonar.samples.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.semantic.Symbol;
import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidLoggingPasswordsRule")
public class AvoidLoggingPasswordsRule extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.emptyList();
  }

  @Override
  public void visitNode(Tree tree) {
//    MethodTree method = (MethodTree) tree;
//
//    Symbol.MethodSymbol symbol = method.symbol();
//    if (symbol == null || !symbol.isMethodSymbol()) return;
//
//    String methodName = symbol.name();
//    String className = symbol.owner().name();
//
//    if (!methodName.equals("info")) return; // Only flag log.info
//
//    for (ExpressionTree arg : method.arguments()) {
//      String argText = arg.toString();
//
//      // Quick-n-dirty string matching. Can be improved with tree analysis.
//      if (argText.contains("Base64.getEncoder()") &&
//        argText.contains("encodeToString") &&
//        argText.toLowerCase().contains("password")) {
//
//        reportIssue(arg, "Avoid logging passwords, even if encoded.");
//        break;
//      }
//    }
  }
}
