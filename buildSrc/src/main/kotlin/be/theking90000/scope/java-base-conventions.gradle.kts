package be.theking90000.scope

plugins.withType<JavaPlugin> {
  extensions.getByType<JavaPluginExtension>().apply {
    toolchain {
      languageVersion = JavaLanguageVersion.of(21)
    }
  }
}
