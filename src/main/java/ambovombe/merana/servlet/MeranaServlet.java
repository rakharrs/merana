package ambovombe.merana.servlet;

import ambovombe.merana.Mapping;
import ambovombe.merana.utils.ClassRetriever;
import ambovombe.merana.utils.mapping.*;
import ambovombe.merana.utils.mapping.method.HttpMethod;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import ambovombe.merana.process.Fileupload;
import ambovombe.merana.process.Modelview;

import java.io.BufferedReader;
import java.lang.reflect.Parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.security.sasl.AuthenticationException;

import com.google.gson.Gson;

import ambovombe.merana.utils.*;

@MultipartConfig
public class MeranaServlet extends HttpServlet {
    HashMap<String, Mapping> MappingUrls = new HashMap<>();
    HashMap<String, Object> SingletonController = new HashMap<>();
    protected Set<Class> classes;
    Gson gson = new Gson();
    String session_name;
    String session_profile;

    @Override 
    public void init() throws ServletException {
        String package_src = getInitParameter("package_src");
        String auth_name = String.valueOf( getInitParameter("session_name") );
        String auth_profile = String.valueOf( getInitParameter("session_profile") ); //

        setSession_name(auth_name);
        setSession_profile(auth_profile);

        try {
            init_classes(Objects.requireNonNullElse(package_src,
                            "controller"));
            retrieveMappingUrls();
            retrieveSingleton();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp, HttpMethod httpMethod) throws IOException {
        if(!dispatch_modelview(req, resp, httpMethod))
            print_test(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, HttpMethod.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, HttpMethod.POST);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, HttpMethod.DELETE);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp, HttpMethod.PUT);
    }

    public HashMap<String, Mapping> getMappingUrls() {
        return MappingUrls;
    }

    public void setMappingUrls(HashMap<String, Mapping> mappingUrls) {
        MappingUrls = mappingUrls;
    }

    protected void init_classes(String package_name) throws ClassNotFoundException, URISyntaxException{
        this.classes = ClassRetriever.findAllClasses(package_name);
    }

    protected void retrieveMappingUrls(){
        for (Class classe : classes){
            RequestUrl requestUrl = null;
            String urlValue = "";
            if(classe.isAnnotationPresent(RequestUrl.class)){
                requestUrl = (RequestUrl) classe.getAnnotation(RequestUrl.class);
                urlValue += requestUrl.value();
            }
            Method[] methods = classe.getMethods();
            for (Method method : methods)
                if(method.isAnnotationPresent(Url.class)) {
                    Url url = method.getAnnotation(Url.class);
                    this.MappingUrls.put(url.method() + " " + urlValue + url.value(), new Mapping(classe.getName(), method.getName(), url.method()));
                }
        }
    }

    protected void retrieveSingleton(){
        for(Class classe : classes){
            if(is_singleton(classe))
                getSingletonController().put(classe.getName(), null);
            System.out.println("classe : "+classe.getName());
            System.out.println(is_singleton(classe));
        }
    }

    public boolean is_singleton(Class<?> classe){
        if(classe.isAnnotationPresent(Scope.class)){
            Scope scope = classe.getAnnotation(Scope.class);
            if(scope.value().toLowerCase().equals("singleton"))
                return true;
        } return false;
    }

    public Object instantiate_class(HttpServletRequest req, Mapping map) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
        Class<?> process_class = Class.forName(map.getClassName());
        Object objet = process_class.newInstance();
        if(is_singleton(process_class)){
            if(getSingletonController().get(map.getClassName()) != null)
                objet = getSingletonController().get(map.getClassName());
            else getSingletonController().put(map.getClassName(), objet);
        } return objet;
    }

    public boolean dispatch_modelview(HttpServletRequest req, HttpServletResponse resp, HttpMethod httpMethod) throws IOException{
        String key = Misc.getMappingValue(req);
        Mapping map = getMappingUrls().get(httpMethod + " " + key);
        Object res = null;
        if(map ==  null){
            return false;
        }

        try {
            if(map.getHttpMethod() != httpMethod){
                throw new UnsupportedOperationException("Method "+ httpMethod +" not supported");
            }

            PrintWriter out = resp.getWriter();
            Object objet = instantiate_class(req, map);

        // Maka an'ilay parameter avy @ requete
            Map<String, String[]> requestParameter = req.getParameterMap();

        // Maka ny parts anle servlet
            Collection<Part> parts = null;
            try {
                parts = req.getParts();
            } catch (Exception e) {
                e.printStackTrace();
            }

        // m'initialize anle parameter
            init_modelview_parameter(requestParameter, parts, objet);


            // M'invoke an'ilay conntroller
            try {
                try {
                    res = invoke_requested_method(req, requestParameter, objet, map.getMethod());
                    Gson gson = new Gson();
                    if(res instanceof Modelview){
                        Modelview modelview = (Modelview) res;
                        if(modelview.isJson()){
                            resp.setContentType("application/json");
                            out.println( gson.toJson(modelview.getData()));
                        }else{
                            for (String k: modelview.getData().keySet())
                                req.setAttribute(k, modelview.getData().get(k));
                            HashMap<String, Object> sessions = modelview.getSessions();
                            this.setSessions(req, sessions);
                            this.handleSession(modelview, req);
                            try {
                                req.getRequestDispatcher(modelview.getView()).forward(req, resp);
                            }catch (Exception e){
                                resp.setStatus(200);
                                resp.setContentType("application/json");
                                resp.setCharacterEncoding("UTF-8");
                                out.println("- ok -");
                                //e.printStackTrace(out);
                                out.close();
                            }

                        }
                        return true;
                    }else{
                        resp.setContentType("application/json");
                        resp.setCharacterEncoding("UTF-8");
                        out.println( gson.toJson(res) );
                        return true;
                    }

                }catch (AuthenticationException e){
                    resp.setStatus(500);
                    resp.setContentType("application/json");
                    out.println(e.getMessage());
                    out.close();
                }
            } catch (Exception e) {
                resp.setStatus(500);
                resp.setContentType("application/json");
                out.println(e.getMessage());
                e.printStackTrace(out);
                out.close();
            }

        } catch (Exception e) {
            PrintWriter out = resp.getWriter();
            e.printStackTrace(out);
            out.close();
        }
        return false;
    }

    public Method getMathingMethod(Method[] methods, String method_name) throws NoSuchMethodException{
        for(Method method : methods)
            if (method.getName().equals(method_name))
                return method;
        throw new NoSuchMethodException("No method as : "+method_name);
    }

    public void handleSession(Modelview modelview, HttpServletRequest request){
        if(modelview.isInvalidate()){
            request.getSession().invalidate();
            return;
        }
        List<String> sessions = modelview.getRemoveSession();
        for(String session : sessions)
            request.getSession().removeAttribute(session);
    }

    public Object invoke_requested_method(HttpServletRequest req, Map<String, String[]> parameters, Object objet, String method_name) throws Exception{
        Object modelview = null;
        Method method = getMathingMethod(objet.getClass().getDeclaredMethods(), method_name);

        // Checking authentification
        if(method.isAnnotationPresent(Auth.class)){
            Auth auth = method.getAnnotation(Auth.class);
            Object sessionName = req.getSession().getAttribute(this.getSession_name());
            Object sessionProfile = req.getSession().getAttribute(this.getSession_profile());

            if( sessionName == null || (sessionName != null  && !((String) sessionProfile).equalsIgnoreCase(auth.user()) ) )
                throw new AuthenticationException("Sorry You can't access that url with your privileges : " + sessionProfile);
        }                   
        
        // Checking if it got session and setting it
        if(method.isAnnotationPresent(Session.class)){
            ArrayList<String> sessions = Collections.list(req.getSession().getAttributeNames());
            HashMap<String, Object> session_copy = new HashMap<>();
            for(String attribute : sessions)
                session_copy.put(attribute, req.getSession().getAttribute(attribute));
            Method set_session = objet.getClass().getDeclaredMethod("setSession", HashMap.class);
            set_session.invoke(objet, session_copy);
        }
        
        // getting the method matching with the url
        Parameter[] params = method.getParameters();
        System.out.println("LENGH " + params.length);// parameters of the method
        if(params.length > 0){
            Class<?>[] method_parameter_class = arrayMethodParameter(method);                                   // method of the parameter
            String[][] args = new String[params.length][];
            for(int i = 0; i < params.length; i++){

                if(params[i].isAnnotationPresent(RequestBody.class)){
                    RequestBody requestBody = params[i].getAnnotation(RequestBody.class);
                    String[] val = new String[1];
                    val[0] = retrieveRequestBody(req);
                    args[i] = val;
                }
                else if(params[i].isAnnotationPresent(Param.class)){

                    Param paramValue = params[i].getAnnotation(Param.class);
                    System.out.println("aaa "+ paramValue.value());
                    String[] param = parameters.get(paramValue.value());
                    args[i] = param;
                }
            }
            System.out.println(gson.toJson(args));
            System.out.println(method_parameter_class.length);
            modelview = method.invoke(objet, dynamicCast(method_parameter_class, args));            // If there are parameters to the function
        }else modelview = method.invoke(objet);
                                                            // if there are no parameter
        if(modelview == null) throw new Exception("The given Modelview is just null");
        return modelview;
    }

    public <T> T retrieveRequestBody(HttpServletRequest req, Class<T> objectClass) throws IOException{
        BufferedReader reader = req.getReader();
        StringBuilder jsonData = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonData.append(line);
        }
        // Process the JSON data using Gson
        // Now, you can work with the deserialized Java object
        return gson.fromJson(jsonData.toString(), objectClass);
    }

    public String retrieveRequestBody(HttpServletRequest req) throws IOException{
        BufferedReader reader = req.getReader();
        StringBuilder jsonData = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            jsonData.append(line);
        return jsonData.toString();
    }

    /**
     * function who dynamically cast an Object with the matching classes
     * @param classes
     * @param args
     * @return Object array
     */
    private Object [] dynamicCast(Class<?>[] classes, String[][] args) throws Exception{

       Object[] array = new Object[classes.length];
       for (int i = 0; i < classes.length; i++){
           System.out.println("class : "+classes[i].getName());
           System.out.println("value : " + args[i][0]);
           if(!classes[i].isArray())
               array[i] = gson.fromJson(args[i][0], classes[i]);
           else array[i] = gson.fromJson(gson.toJson(args[i]),classes[i]);
       }

       return array;
   }

    /* Invoke setters */
    public void init_modelview_parameter(Map<String, String[]> parameters, Collection<Part> parts, Object objet) throws Exception{
        Method[] methods = objet.getClass().getDeclaredMethods();               // Maka ny methodn'ilay class
        for(Method method : methods){                                           // Loop all of it's method to get the set and set the parameter
            String method_name = method.getName();
            System.out.println("method name : "+method_name);
            if(!method_name.startsWith("set", 0))                               // if the method isn't a setter
                continue;
            String field_name = method_name.substring(3).toLowerCase();         // Getting the fieldname
            String[] parameter = parameters.get(field_name);                    // Getting the parameter that matches with the field name
            Class<?>[] method_parameter_class = arrayMethodParameter(method);
            if(parameter != null && parameter.length > 0){                                                    
                if(method_parameter_class[0] == Fileupload.class){
                    if(parts != null){
                        Part part = Misc.getPart(field_name, parts);
                        Fileupload fileupload = Misc.makeFileUpload(part);
                        method.invoke(objet, fileupload);
                    }
                }else method.invoke(objet, dynamicCast(method_parameter_class, parameter));
            }else{
                if(method_parameter_class[0] == Fileupload.class){
                    if(parts != null && parts.size() > 0){
                        Part part = Misc.getPart(field_name, parts);
                        Fileupload fileupload = Misc.makeFileUpload(part);
                        method.invoke(objet, fileupload);
                    }
                }
            }
        }
    }

        /**
     * function who dynamically cast an Object with the matching classes
     * @param classes
     * @param args
     * @return Object array
     */

     private Object [] dynamicCast(Class<?>[]classes,Object[]args) throws Exception{
        Object[] array = new Object[classes.length];
        int i = 0;
        for (Class<?> cl:classes) {
            if(cl == Date.class){
                array[i] = Date.valueOf((String)args[i]);
            }else array[i] = cl.getDeclaredConstructor(String.class).newInstance(args[i]);
            i++;
        }
        return array;
    }

    /**
     * Return the class of the method's argument
     * @param method
     * @return classes of method parameter(s)
     */
    private Class<?>[] arrayMethodParameter(Method method) {
        // Get the parameters of the method
        Parameter[] parameters = method.getParameters();
        // Create an array to store the classes of the parameter instances
        Class<?>[] paramClasses = new Class<?>[parameters.length];
        // Iterate through the parameters and get their classes
        for (int i = 0; i < parameters.length; i++) {
            paramClasses[i] = parameters[i].getType();
        }
        // Return the array of parameter classes
        return paramClasses;
    }

    public Method stringMatching(Method[] methods, String method_name){
        Method matching = null;
        for (Method method : methods)
            if(method.getName().equals(method_name))
                return method;
        return null;
    }

    public void print_test(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        resp.setContentType("text/html");
        resp.setStatus(404);
        HttpServletMapping mapping = req.getHttpServletMapping();

        // Hello
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1> ERROR 404 </h1>");
        out.println("<h2> URI : " + req.getRequestURI() + "</h2>");
        out.println("<h2> Query : " + req.getQueryString() + "</h2>");
        out.println("<h2> URL : " + req.getRequestURL() + "</h2>");
        out.println("<h2>" + mapping.getPattern() + "</h2>");
        out.println("<h2>" + mapping.getMatchValue() + "</h2>");
        out.println("<p>"+req.getContextPath()+"</p>");
        out.println("<p>"+Misc.getMappingValue(req)+"</p>");
        for (String k: getMappingUrls().keySet()) {
            out.print("key : " + k);
            out.println(" value : " + getMappingUrls().get(k).getClassName() + "<br>");
        }

        for(String k : getSingletonController().keySet()){
            out.print("singleton key : " + k);
            out.println(" value : " + getMappingUrls().get(k).getClassName());
        }
        System.gc();
        out.println("</body></html>");
    }

    public HashMap<String, Object> getSingletonController() {
        return SingletonController;
    }
    public void setSingletonController(HashMap<String, Object> singletonController) {
        SingletonController = singletonController;
    }

    public String getSession_name() {
        return session_name;
    }

    public String getSession_profile() {
        return session_profile;
    }

    public void setSession_name(String session_name) {
        this.session_name = session_name;
    }

    public void setSession_profile(String session_profile) {
        this.session_profile = session_profile;
    }

    private void setSessions(HttpServletRequest request, HashMap<String, Object> sessions) throws Exception {
        HttpSession session = request.getSession();
        for (Map.Entry<String, Object> sets : sessions.entrySet()) {
            session.setAttribute(sets.getKey(), sets.getValue());
        }
        // request.setAttribute(  );
    }
}
