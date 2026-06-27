plugins {
  be.theking90000.scope.`java-library-conventions`
  be.theking90000.scope.`maven-publishing-conventions`
}

dependencies {
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)

  testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
  test {
    configureEach {
      useJUnitPlatform()
    }
  }
}
