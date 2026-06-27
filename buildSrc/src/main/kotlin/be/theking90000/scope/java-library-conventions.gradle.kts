package be.theking90000.scope

plugins {
  `java-library`
  id("be.theking90000.scope.java-base-conventions")
}

java {
  withSourcesJar()
  withJavadocJar()
}
