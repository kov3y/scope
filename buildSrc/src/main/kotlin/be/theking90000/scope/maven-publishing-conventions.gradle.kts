package be.theking90000.scope

plugins {
  `maven-publish`
}

extensions.configure<PublishingExtension> {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      pom {
        name = project.name
        description = "Dependency injection utilities for Java."
        url = "https://github.com/theking90000/scope"

        licenses {
          license {
            name = "MIT License"
            url = "https://opensource.org/licenses/MIT"
          }
        }

        developers {
          developer {
            id = "theking90000"
            name = "theking90000"
          }
        }

        scm {
          connection = "scm:git:git://github.com/theking90000/scope.git"
          developerConnection = "scm:git:ssh://github.com/theking90000/scope.git"
          url = "https://github.com/theking90000/scope"
        }
      }
    }
  }

  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/theking90000/scope")

      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
}
