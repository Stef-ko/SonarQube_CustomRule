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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2639")
public class InappropriateRegexpCheck extends AbstractMethodDetection {

  private static final String INAPPROPRIATE_REGEXPS = "[.|]";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.lang.String")
      .names("replaceAll", "replaceFirst")
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArg = mit.arguments().get(0);
    if (isInappropriateRegexpStringLiteral(firstArg) || isFileSeparator(firstArg)) {
      reportIssue(firstArg, "Correct this regular expression.");
    }
  }

  private static boolean isInappropriateRegexpStringLiteral(ExpressionTree firstArg) {
    return firstArg.asConstant(String.class)
      .filter(regexp -> regexp.matches(INAPPROPRIATE_REGEXPS))
      .isPresent();
  }

  private static boolean isFileSeparator(ExpressionTree firstArg) {
    if (firstArg.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) firstArg;
      return "separator".equals(mse.identifier().name()) && mse.expression().symbolType().is("java.io.File");
    }
    return false;
  }

}
