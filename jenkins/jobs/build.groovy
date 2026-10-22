pipelineJob("CiMavenBuild") {
   description("Builds ci.maven.")
   keepDependencies(false)
   definition {
      cpsScm {
         scm {
            git("https://github.com/OpenLiberty/ci.maven.git", "main")
         }
         scriptPath("jenkins/build.jenkinsfile")
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
