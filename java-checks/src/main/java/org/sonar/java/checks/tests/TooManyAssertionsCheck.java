/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.ASSERTION_INVOCATION_MATCHERS;
import static org.sonar.java.checks.helpers.UnitTestUtils.isUnitTest;
import static org.sonar.java.checks.helpers.UnitTestUtils.methodNameMatchesAssertionMethodPattern;
import static org.sonar.java.model.ExpressionUtils.methodName;

@Rule(key = "S5961")
public class TooManyAssertionsCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 25;

  @RuleProperty(key = "MaximumAssertionNumber", description = "The maximum authorized number of assertions in a test method", defaultValue = "" + DEFAULT_MAX)
  public int maximum = DEFAULT_MAX;

  private final Map<Symbol, List<Tree>> assertionsInMethod = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.ABSTRACT)) {
      return;
    }

    if (isUnitTest(methodTree)) {
      List<Tree> assertionsTree = collectAssertionsInMethod(methodTree.symbol());
      int assertionsSize = assertionsTree.size();
      if (assertionsSize > maximum) {
        List<JavaFileScannerContext.Location> locations = assertionsTree.stream()
          .map(assertionTree -> new JavaFileScannerContext.Location("Assertion", assertionTree))
          .toList();

        reportIssue(methodTree.simpleName(),
          String.format("Refactor this method to reduce the number of assertions from %d to less than %d.", assertionsSize, maximum),
          locations,
          null);
      }
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    assertionsInMethod.clear();
    super.leaveFile(context);
  }

  private List<Tree> collectAssertionsInMethod(Symbol symbol) {
    if (!assertionsInMethod.containsKey(symbol)) {
      // can not be rewritten with map.computeIfAbsent() because of concurrent modification
      assertionsInMethod.put(symbol, Collections.emptyList());
      Tree declaration = symbol.declaration();
      if (declaration != null) {
        AssertionsCounterVisitor assertionsCounterVisitor = new AssertionsCounterVisitor();
        declaration.accept(assertionsCounterVisitor);
        assertionsInMethod.put(symbol, new ArrayList<>(assertionsCounterVisitor.assertions));
      }
    }

    return assertionsInMethod.get(symbol);
  }

  private class AssertionsCounterVisitor extends BaseTreeVisitor {

    private final Set<Tree> assertions = new LinkedHashSet<>();
    private final Set<Tree> chainedAssertions = new LinkedHashSet<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      super.visitMethodInvocation(mit);
      if (isAssertion(methodName(mit), mit.methodSymbol())) {
        ExpressionTree methodSelect = mit.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (assertions.contains(expression) || chainedAssertions.contains(expression)) {
            chainedAssertions.add(mit);
            return;
          }
        }
        assertions.add(mit);
      }
    }

    @Override
    public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
      super.visitMethodReference(methodReferenceTree);
      if (isAssertion(methodReferenceTree.method(), methodReferenceTree.method().symbol())) {
        assertions.add(methodReferenceTree);
      }
    }

    private boolean isAssertion(IdentifierTree method, Symbol methodSymbol) {
      return methodNameMatchesAssertionMethodPattern(method.name(), methodSymbol)
        || ASSERTION_INVOCATION_MATCHERS.matches(methodSymbol)
        || !collectAssertionsInMethod(methodSymbol).isEmpty();
    }
  }
}
