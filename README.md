# MERANA
A lightweight WEB Framework for java
## Setup
### Intro
the classes have to be in the same package wich is define in *web.xml* as package_src init-parameter
```Xml
    <servlet>
        <servlet-name>merana</servlet-name>
        <servlet-class>ambovombe.merana.servlet.MeranaServlet</servlet-class>
        <init-param>
            <param-name> package_src </param-name>
            <param-value>com.example.demo.model</param-value>
        </init-param>
    </servlet>
    <servlet-mapping>
        <servlet-name>merana</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>  
```

## Instructions
- If you want to control Cors access policy you should add the following code to your project and define it in the web.xml file as following
```Java
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CorsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Allow requests from any origin. You can restrict this to specific origins.
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        // Allow the following HTTP methods.
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // Allow the following headers.
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Allow credentials (e.g., cookies).
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed.
    }

    @Override
    public void destroy() {
        // Cleanup code if needed.
    }
}



```

- And add it in the web xml file as follow
```Xml
    <filter>
        <filter-name>CorsFilter</filter-name>
        <filter-class>com.example.demo.cors.CorsFilter</filter-class> <!-- Update with the correct package and class name -->
    </filter>
    <filter-mapping>
        <filter-name>CorsFilter</filter-name>
        <url-pattern>/*</url-pattern> <!-- This allows the filter to intercept all requests -->
    </filter-mapping>
```

- the Url will be defined with the annotation @RequestUrl or the class or @Url for the method
- the @Url annotation will have a type method to define the method of the http request
    - Example
    ```Java
    @RequestUrl("/example")
    public class Test {
      @Url(method = GET)                     // the link to this function will be .../example within the method GET
      public ModelView getMethod(){
        // do something...
      }

      @Url(value = "/save", method = POST)    // the link to this function will be .../example/save within the method POST
      public ModelView postMethod(){
        // do something
      }
    }
    ```
- TO pass formData parameter you use the annotation @Param(value = "...")
- To pass data from a body you use the annotation @RequestBody
    - Example
    ```Java
    @RequestUrl("/example")
    public class Test {
      @Url(method = PUT)                     // the link to this function will be .../example within the method PUT
      public Object getMethod(@RequestBody ExampleObject example){
        // do something...
      }

      @Url(value = "/save", method = POST)    // the link to this function will be .../example/save within the method POST
      public ModelView postMethod(@Param("name") String name){
        // do something
      }
    }
    ```
### MVC model
- functions must return Modelview 
- Within the Modelview you can define the view *jsp file* to show
and send data to this view
    - Example
    ```Java
        Modelview modelview = new Modelview();
        modelview.setView("index.jsp");
        modelview.addItem("data", "Test");
    ```
 - You can also put argument in the function and defnie it by post or get method
 - And do the same thing with classes property
    - Example
        ```Java
            public class Test {
                String name;
                @Url("/tests")
                public Modelview tests(String ok){
                    Modelview modelview = new Modelview();
                    if(getName() != null)
                        modelview.addItem("test1", getName());
                    else modelview.addItem("test1", "unknown");
                    modelview.addItem("test2", ok);
                    modelview.setView("ox.jsp");
                    return modelview;
                }
            }
        ```

### WEB Service model
- when the Url annoted method return anything else than ModelView it will return a JSON
    - Example
    ```Java
  	@Url(method = POST)
  	public Commune save(@RequestBody Commune commune){
        // Do something...
        return commune;
  	}
    ```
### Session
- There is an authentification function that could be configured in the web.xml as following:
- By adding as init-param a session_name and a session_profil where the session name will be the name of the boolean value within the session that says if it could be used or not.
- And session_profil the name of the value within the session that contain the profile type
``` xml
    <servlet>
        <servlet-name>frontservlet</servlet-name>
        <servlet-class>etu1999.framework.servlet.FrontServlet</servlet-class>
        <init-param>
            <param-name>package_src</param-name>
            <param-value>controller</param-value>
        </init-param>
        <init-param>
            <param-name>session_name</param-name>
            <param-value>isConnected</param-value>
        </init-param>
        <init-param>
            <param-name>session_profile</param-name>
            <param-value>profil</param-value>
        </init-param>
    </servlet>
```

- An example of utilisation:
``` Java
@Url("/login")
    public Modelview signin(){
        Modelview modelview = new Modelview();
        modelview.setView("login.jsp");

        return modelview;
    }
    
    @Url("/signin")
    public Modelview login(String username, String password){
        Modelview modelview = new Modelview();

        if(username.equals("rakharrs") && password.equals("pixel")){
            modelview.addSession( "isConnected" , true );
			modelview.addSession( "profil" , "admin" );
        }else if(username.equals("scott") && password.equals("tiger")){
            modelview.addSession( "isConnected" , true );
			modelview.addSession( "profil" , "guest" );
        }

        modelview.setView("authed.jsp");

        return modelview;
    }

    @Auth(user = "admin")
    @Url("/limited")
    public Modelview limited(){
        Modelview modelview = new Modelview();
        modelview.setView("admin.jsp");
        return modelview;
    }
```

- There in the login page, it's just a normal login, in the signin (which is the page processing) it verify the credentials and put in the session the value
- The if a user want to access /limited page it will only be displayed if the user is authentified as an admin profil 
