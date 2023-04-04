<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
      <jsp:useBean id="datetime" class="java.util.Date" />
      <title>Hello IBM WebSphere Application Server Liberty</title>
  </head>
  <body>
      <h2>Welcome to IBM Liberty</h2>
      <p>Congratulations on running this very simple demo application on ${datetime}.</p>
  </body>
</html>
