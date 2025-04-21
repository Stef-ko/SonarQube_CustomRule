package org.sonar.samples.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.semantic.Symbol;
import java.util.Collections;
import java.util.List;

@Rule(key = "AvoidLoggingPasswordsRule")
public class AvoidLoggingPasswordsRule extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
    ExpressionTree methodSelect = methodInvocation.methodSelect();

    if(methodSelect instanceof MemberSelectExpressionTree){
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodSelect;
      String methodName = memberSelect.identifier().name();
      String expression = memberSelect.expression().toString();

      if(methodName.equals("info") && expression.matches(".*log$")){
        for (ExpressionTree arg : methodInvocation.arguments()){
          String argText = arg.toString();

          if (argText.toLowerCase().contains("password")){
            reportIssue(methodInvocation, "Don't log passwords, CUNT!");
          }
        }
      }
    }
  }
}
