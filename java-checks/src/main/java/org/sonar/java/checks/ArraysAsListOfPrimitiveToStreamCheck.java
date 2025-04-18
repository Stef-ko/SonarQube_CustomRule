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

import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3631")
public class ArraysAsListOfPrimitiveToStreamCheck extends AbstractMethodDetection {

  private static final MethodMatchers ARRAYS_AS_LIST = MethodMatchers.create()
    .ofTypes("java.util.Arrays").names("asList").withAnyParameters().build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.util.List").names("stream").addWithoutParametersMatcher().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree arrayAsListInvocation = (MethodInvocationTree) expression;
        if (ARRAYS_AS_LIST.matches(arrayAsListInvocation) && shouldUsePrimitiveStream(arrayAsListInvocation)) {
          reportIssue(arrayAsListInvocation.methodSelect(), "Use \"Arrays.stream\" instead of \"Arrays.asList\".");
        }
      }
    }
  }

  private static boolean shouldUsePrimitiveStream(MethodInvocationTree mit) {
    Set<Type> argumentTypes = argumentTypes(mit.arguments());
    return argumentTypes.size() == 1 && isPrimitiveTypeHandledByStream(argumentTypes.iterator().next());
  }

  private static Set<Type> argumentTypes(Arguments arguments) {
    return arguments.stream().map(ExpressionTree::symbolType).collect(Collectors.toSet());
  }

  private static boolean isPrimitiveTypeHandledByStream(Type type) {
    return type.isPrimitive(Type.Primitives.INT) || type.isPrimitive(Type.Primitives.DOUBLE) || type.isPrimitive(Type.Primitives.LONG);
  }

}
