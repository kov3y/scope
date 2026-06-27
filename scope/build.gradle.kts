plugins {
  be.theking90000.scope.`java-library-conventions`
}

dependencies {
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)

  testRuntimeOnly(libs.junit.platform.launcher)
}
