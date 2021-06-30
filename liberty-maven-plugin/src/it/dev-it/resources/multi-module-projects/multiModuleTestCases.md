## Multi module project layouts

a)  pom, ear+war+jar
- Server info in ear module
- Main pom: ./pom.xml
- Tests exist in every module

a.2) pom folder at the same level as (sibling folder of) ear+war+jar
- Server info in ear module
 - Main pom: ./pom/pom.xml

b)  pom, war+jar
- Server info in war module
- Main pom: ./pom.xml

e)  pom, liberty-assembly+ear+war+jar    
- Server info in pom module
- Main pom: ./pom.xml
- For devc, use -Ddockerfile=../Dockerfile
- Tests exist in every module

g)  pom,  server(pom)+ear+war+jar    
- Server info in pom module
- Main pom: ./pom.xml
- For devc, use -Ddockerfile=../Dockerfile

h)  server(pom)+ear+war+jar    
- Server info in pom module
- Main pom: ./pom/pom.xml
- For devc, use -Ddockerfile=../Dockerfile

i)   same as (a) but "ear" project has a `<parent>` which is not the "pom" project
- Server info in ear module
- Main pom: ./pom.xml

j) server(pom)+war1+war2+jar
- Server info in pom module
- Main pom: ./pom.xml
- For devc, use -Ddockerfile=../Dockerfile
- This project contains two web applications, war1 (/converter1) and war2 (/converter2)
