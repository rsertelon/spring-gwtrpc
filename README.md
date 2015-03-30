# Introduction

Spring GWT-RPC is insprired by the `gwtrpc-spring` project. Its aim is to provide a simplified and updated version of the latter.

## Features

### Dispatch to Spring beans

Spring GWT-RPC provides a simple Servlet that extends `RemoteServiceServlet` to dispatch RPC requests to the required Spring beans.

To use it you just have to declare this Servlet in the `web.xml` of your project:

```xml
<servlet>
	<servlet-name>GWTService</servlet-name>
	<servlet-class>fr.sertelon.spring.gwtrpc.SpringRemoteServiceServlet</servlet-class>
</servlet>
<servlet-mapping>
	<servlet-name>GWTService</servlet-name>
	<url-pattern>*.rpc</url-pattern><!-- use the mapping that suits you most -->
</servlet-mapping>
```

Then all you have to do is to declare your `RemoteServiceImpl` in your Spring configuration thanks `@Component` annotation, or via XML configuration.

### Reverse Proxy support for Serialization Policy

Spring GWT-RPC also provides out-of-the-box Reverse Proxy support for finding its `Serialization Policy`.

Suppose you have the following RP configuration: `http://my.site/hello/gwtapp -> http://localhost:8080/gwtapp`, GWT will complain about missing Serialization Policy because it will try to find it at `/hello/gwtapp/*.gwt.rpc` instead of `/gwtapp/*.gwt.rpc`.

To solve this, Spring GWT-RPC uses the custom `X-GWT-Path-Prefix` header to strip the given prefix and help GWT find the Serialization Policy where it actually is.

Here's a simple apache example:

```httpd
<Virtualhost *:80>
  ServerName my.site

  <Proxy *>
    Order deny,allow
    Allow from all
  </Proxy>

  RequestHeader set X-GWT-Path-Prefix /hello

  ProxyPass           /hello/gwtapp/ http://localhost:8080/gwtapp/
  ProxyPassReverse    /hello/gwtapp/ http://localhost:8080/gwtapp/
</Virtualhost>
```

## License

This project is licensed under the Apache Software License v2.0
