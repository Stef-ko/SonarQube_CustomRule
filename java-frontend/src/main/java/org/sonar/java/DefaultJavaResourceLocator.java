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
package org.sonar.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;

public class DefaultJavaResourceLocator implements JavaResourceLocator {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultJavaResourceLocator.class);

  private final ClasspathForMain javaClasspath;
  private final ClasspathForTest javaTestClasspath;
  @VisibleForTesting
  Map<String, InputFile> resourcesByClass;

  public DefaultJavaResourceLocator(ClasspathForMain javaClasspath, ClasspathForTest javaTestClasspath) {
    this.javaClasspath = javaClasspath;
    this.javaTestClasspath = javaTestClasspath;
    resourcesByClass = new HashMap<>();
  }

  @Override
  public InputFile findResourceByClassName(String className) {
    String name = className.replace('.', '/');
    InputFile inputFile = resourcesByClass.get(name);
    if (inputFile == null) {
      LOG.debug("Class not found in resource cache : {}", className);
    }
    return inputFile;
  }

  private Collection<String> classKeys() {
    return new TreeSet<>(resourcesByClass.keySet());
  }

  @Override
  public Collection<File> classFilesToAnalyze() {
    List<File> result = new ArrayList<>();
    for (String key : classKeys()) {
      String filePath = key + ".class";
      for (File binaryDir : javaClasspath.getBinaryDirs()) {
        File classFile = new File(binaryDir, filePath);
        if (classFile.isFile()) {
          result.add(classFile);
          break;
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public Collection<File> classpath() {
    return Collections.unmodifiableList(javaClasspath.getElements());
  }

  @Override
  public Collection<File> testClasspath() {
    return Collections.unmodifiableList(javaTestClasspath.getElements());
  }

  @Override
  public Collection<File> binaryDirs() {
    return Collections.unmodifiableList(javaClasspath.getBinaryDirs());
  }

  @Override
  public Collection<File> testBinaryDirs() {
    return Collections.unmodifiableList(javaTestClasspath.getBinaryDirs());
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    InputFile inputFile = context.getInputFile();
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    javaFilesCache.scanFile(context);
    javaFilesCache.getClassNames().forEach(className -> resourcesByClass.put(className, inputFile));
  }
}
