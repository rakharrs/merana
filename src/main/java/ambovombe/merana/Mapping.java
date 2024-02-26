package ambovombe.merana;

import ambovombe.merana.utils.mapping.method.HttpMethod;

import java.util.HashMap;

public class Mapping {
    String className;
    HttpMethod httpMethod;
    String Method;

    public Mapping(String classname, String method, HttpMethod httpMethod){
        setClassName(classname);
        setMethod(method);
        setHttpMethod( httpMethod);
    }

    public String getClassName() {
        return className;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethod() {
        return Method;
    }

    public void setMethod(String method) {
        Method = method;
    }
}
