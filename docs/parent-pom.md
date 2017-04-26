#### Parent Pom 
---
`liberty-maven-app-parent` is a parent pom that contains a `<pluginManagement/>` section for binding the 
`liberty-maven-plugin` goals to the Maven default build lifecycle.

###### 

The following is the mapping of liberty goals to the Maven default build lifecycle defined in the parent pom.

| Phase | Goal |
| ----- | ---- | 
| pre-clean | liberty:stop-server |
| prepare-package | liberty:install-server |
| prepare-package | liberty:create-server |
| package | liberty:install-apps |
| package | liberty:package-server |
| pre-integration-test | liberty:test-start-server |
| post-integration-test | liberty:test-stop-server |

######

If there is already an organization or community specific parent pom used in the project, the `<pluginManagement/>` 
section from [liberty-maven-app-parent/pom.xml](../liberty-maven-app-parent/pom.xml) can be added to the 
organization parent pom, or include the `<pluginManagement/>` section directly into the project pom.xml.
