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
package org.sonar.java.model.declaration;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MethodTreeImplTest {

  @Test
  void override_without_annotation_should_be_detected() {
    CompilationUnitTree cut = createTree("interface T { int m(); } class A implements T { public int m(){return 0;}}");
    ClassTree interfaze = (ClassTree) cut.types().get(0);
    MethodTreeImpl methodInterface = (MethodTreeImpl) interfaze.members().get(0);
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodClazz = (MethodTreeImpl) clazz.members().get(0);
    assertThat(methodInterface.isOverriding()).isFalse();
    assertThat(methodClazz.isOverriding()).isTrue();
  }

  @Test
  void override_with_generic_parameters_should_be_detected() {
    CompilationUnitTree cut = createTree("""
      public class ReferenceQueue<T> {
        private static class Null extends ReferenceQueue {
          boolean enqueue(Reference r) {
            return false;
          }
        }
        boolean enqueue(Reference<? extends T> r) {}
      }
      public abstract class Reference<T> {}
    """);

    ClassTree innerClass = (ClassTree) cut.types().get(0);
    MethodTreeImpl methodInterface = (MethodTreeImpl) ((ClassTree) innerClass.members().get(0)).members().get(0);

    assertThat(methodInterface.isOverriding()).isTrue();

  }

  @Test
  void hiding_of_static_methods() {
    CompilationUnitTree cut = createTree("class A { static void foo() {} } class B extends A { void foo(){} } ");
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodTree = (MethodTreeImpl) clazz.members().get(0);

    assertThat(methodTree.isOverriding()).isFalse();
  }

  @Test
  void override_from_object_should_be_detected() {
    MethodTreeImpl method = getUniqueMethod("class A { public String toString(){return \"\";}}");
    assertThat(method.isOverriding()).isTrue();
  }

  @Test
  void override_unknown() {
    MethodTreeImpl method = getUniqueMethod("class A extends Unknown { void foo(){}}");
    assertThat(method.isOverriding()).isNull();
  }

  @Test
  void unknown_override_because_of_unknown_parameter_type() {
    CompilationUnitTree cut = createTree("""
      interface I       { void bar(int a); }
      class A           { void foo(String arg) {} }
      class B extends A implements I { void foo(Unknown arg) {} void bar(Unknown arg) {} }
      """);
    ClassTree classB = (ClassTree) cut.types().get(2);
    MethodTreeImpl fooMethodOfB = (MethodTreeImpl) classB.members().get(0);
    MethodTreeImpl barMethodOfB = (MethodTreeImpl) classB.members().get(1);
    assertThat(fooMethodOfB.isOverriding()).isNull();
    assertThat(barMethodOfB.isOverriding()).isNull();
  }

  @Test
  void unknown_override_because_of_unknown_parameter_type_and_hierarchy() {
    CompilationUnitTree cut = createTree("""
      class A extends Unknown {}
      class B extends A { void foo(Unknown arg) {} }
      """);
    ClassTree classB = (ClassTree) cut.types().get(1);
    MethodTreeImpl fooMethodOfB = (MethodTreeImpl) classB.members().get(0);
    assertThat(fooMethodOfB.isOverriding()).isNull();
  }

  @Test
  void resolved_override_even_with_unknown_parameter_type() {
    CompilationUnitTree cut = createTree("""
      interface I          { void foo(int a, int b); }
      class A implements I { void bar(Unknown arg) {} void foo() {} int foo; }
      class B extends A    { void foo(Unknown arg) {} void foo(String arg) {} }
      """);
    ClassTree classB = (ClassTree) cut.types().get(2);
    MethodTreeImpl fooMethodOfB = (MethodTreeImpl) classB.members().get(0);
    assertThat(fooMethodOfB.isOverriding()).isFalse();
  }

  @Test
  void override_unknown_in_super_class() {
    CompilationUnitTree cut = createTree("""
      class A extends Unknown {}
      class B extends A {
        void foo() {}
      }
      """);
    MethodTreeImpl methodTree = (MethodTreeImpl)((ClassTree) cut.types().get(1)).members().get(0);
    assertThat(methodTree.isOverriding()).isNull();
  }

  @Test
  void override_unknown_in_interface() {
    MethodTreeImpl method = getUniqueMethod("class A implements Unknown { void foo(){}}");
    assertThat(method.isOverriding()).isNull();
  }

  @Test
  void override_unknown_in_parent_interface() {
    CompilationUnitTree cut = createTree("interface A extends Unknown {}\n" +
      "class B implements A { void foo(){}}");
    MethodTreeImpl methodTree = (MethodTreeImpl)((ClassTree) cut.types().get(1)).members().get(0);
    assertThat(methodTree.isOverriding()).isNull();
  }

  @Test
  void override_unknown_in_parent_interface_parent() {
    CompilationUnitTree cut = createTree("""
      interface I extends Unknown {}
      class A implements I {}
      class B extends A { void foo() {} }
      """);
    MethodTreeImpl methodTree = (MethodTreeImpl)((ClassTree) cut.types().get(2)).members().get(0);
    assertThat(methodTree.isOverriding()).isNull();
  }

  @Test
  void override_result_is_cached() {
    MethodTreeImpl method = spy(getUniqueMethod("class A extends Unknown { void foo(){}}"));
    assertThat(method.isOverriding()).isEqualTo(method.isOverriding());
    verify(method, times(1)).isAnnotatedOverride();
  }

  @Test
  void override_with_non_compiling_code() {
    CompilationUnitTree cut = createTree("""
      class A {
        void foo(){}
        void foo(){}
      }
      """);
    List<Tree> members = ((ClassTree) cut.types().get(0)).members();
    // The semantic for the first method is correct
    assertThat(((MethodTreeImpl) members.get(0)).isOverriding()).isFalse();
    // The semantic for the second method is broken (due to the duplicate)
    assertThat(((MethodTreeImpl) members.get(1)).isOverriding()).isNull();
  }

  @Test
  void static_method_cannot_be_overridden() {
    assertThat(getUniqueMethod("class A{ static void m(){}}").isOverriding()).isFalse();
  }

  @Test
  void private_method_cannot_be_overridden() {
    assertThat(getUniqueMethod("class A{ private void m(){}}").isOverriding()).isFalse();
  }

  @Test
  void override_annotated_method_should_be_overridden() {
    assertThat(getUniqueMethod("class A{ @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @Foo @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @java.lang.Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @cutom.namespace.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @foo.bar.lang.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @foo.lang.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @Foo void m(){}}").isOverriding()).isFalse();
  }

  @Test
  void compute_cfg() {
    MethodTree methodWithoutBody = getUniqueMethod("interface A { void foo(int arg) throws Exception; }");
    ControlFlowGraph cfg = methodWithoutBody.cfg();
    assertThat(cfg).isNull();

    MethodTree method = getUniqueMethod("class A { void foo(int arg) throws Exception { }}");
    cfg = method.cfg();
    assertThat(cfg).isNotNull();
    assertThat(method.cfg()).isSameAs(cfg);

    MethodTree methodWithSwitchAndYield = getUniqueMethod("class A { String foo(int arg) {return switch (arg) {case 1: yield \"Got a 1\"; case 2: yield \"Got a 2\"; default: yield \"More than 2\";};}}");
    cfg = methodWithSwitchAndYield.cfg();
    assertThat(cfg).isNotNull();
    assertThat(methodWithSwitchAndYield.cfg()).isSameAs(cfg);
  }

  @Test
  void has_all_syntax_token() {
    MethodTreeImpl method = getUniqueMethod("class A { public void foo(int arg) throws Exception {} }");
    assertThat(method.openParenToken()).isNotNull();
    assertThat(method.closeParenToken()).isNotNull();
    assertThat(method.semicolonToken()).isNull();
    assertThat(method.throwsToken()).isNotNull();

    method = getUniqueMethod("abstract class A { public abstract void foo(int arg); }");
    assertThat(method.openParenToken()).isNotNull();
    assertThat(method.closeParenToken()).isNotNull();
    assertThat(method.semicolonToken()).isNotNull();
    assertThat(method.throwsToken()).isNull();

    method = getUniqueMethod("record Output(String title) { public Output {} }");
    assertThat(method.openParenToken()).isNull();
    assertThat(method.closeParenToken()).isNull();
    assertThat(method.semicolonToken()).isNull();
    assertThat(method.throwsToken()).isNull();
  }

  @Test
  void getLine_return_line_of_method_declaration() {
    MethodTreeImpl method = getUniqueMethod("class A { public void foo(int arg) throws Exception {} }");
    assertThat(method.getLine()).isEqualTo(1);

    method = getUniqueMethod("record Output(String title) { public Output {} }");
    assertThat(method.getLine()).isEqualTo(1);
  }

  @Test
  void varargs_flag() {
    Symbol.MethodSymbol methodSymbol = getUniqueMethod("class A { public static void main(String[] args){} }").symbol();
    assertThat(methodSymbol.isVarArgsMethod()).isFalse();
    methodSymbol = getUniqueMethod("class A { public static void main(String... args){} }").symbol();
    assertThat(methodSymbol.isVarArgsMethod()).isTrue();
  }

  private static MethodTreeImpl getUniqueMethod(String code) {
    CompilationUnitTree cut = createTree(code);
    return (MethodTreeImpl) ((ClassTree) cut.types().get(0)).members().get(0);
  }

  private static CompilationUnitTree createTree(String code) {
    return JParserTestUtils.parse(code);
  }

}
