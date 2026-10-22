pipelineJob("CiMavenDependencyBuild") {
   description("Builds ci.maven dependency.")
   keepDependencies(false)
   definition {
      cpsScm {
         scm {
            git("https://github.com/OpenLiberty/ci.maven.git", "main")
         }
         scriptPath("jenkins/buildDependency.jenkinsfile")
         lightweight()
      }
   }
   disabled(false)
   configure {
      it / 'properties' / 'com.sonyericsson.rebuild.RebuildSettings' {
         'autoRebuild'('false')
         'rebuildDisabled'('false')
      }
   }
}
