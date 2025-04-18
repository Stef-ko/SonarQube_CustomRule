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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S1210")
public class EqualsNotOverriddenWithCompareToCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!isComparable(classTree)) {
      return;
    }
    boolean hasEquals = false;
    MethodTree compare = null;

    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;

        if (isEqualsMethod(method)) {
          hasEquals = true;
        } else if (isCompareToMethod(method)) {
          compare = method;
        }
      }
    }

    if (compare != null && !hasEquals) {
      reportIssue(compare.simpleName(), "Override \"equals(Object obj)\" to comply with the contract of the \"compareTo(T o)\" method.");
    }
  }

  private static boolean isCompareToMethod(MethodTree method) {
    String name = method.simpleName().name();
    return "compareTo".equals(name) && returnsInt(method) && method.parameters().size() == 1;
  }

  private static boolean isEqualsMethod(MethodTree method) {
    return MethodTreeUtils.isEqualsMethod(method);
  }

  private static boolean isComparable(ClassTree tree) {
    for (Type type : tree.symbol().interfaces()) {
      if (type.is("java.lang.Comparable")) {
        return true;
      }
    }
    return false;
  }

  private static boolean returnsInt(MethodTree tree) {
    TypeTree typeTree = tree.returnType();
    return typeTree != null && typeTree.symbolType().isPrimitive(Type.Primitives.INT);
  }

}
