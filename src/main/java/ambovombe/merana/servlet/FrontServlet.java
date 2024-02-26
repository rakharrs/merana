package ambovombe.merana.servlet;

import ambovombe.merana.Mapping;
import ambovombe.merana.utils.ClassRetriever;
import ambovombe.merana.utils.mapping.Url;
import ambovombe.merana.utils.mapping.Arg;
import ambovombe.merana.utils.mapping.Auth;
import ambovombe.merana.utils.mapping.Scope;
import ambovombe.merana.utils.mapping.Session;
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
import java.util.Vector;

import javax.security.sasl.AuthenticationException;

import com.google.gson.Gson;

import java.lang.reflect.Array;

import ambovombe.merana.utils.*;

@MultipartConfig
public class FrontServlet extends HttpServlet {
    HashMap<String, Mapping> MappingUrls = new HashMap<>();
    HashMap<String, Object> SingletonController = new HashMap<>();
    protected Set<Class> classes;

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
            Method[] methods = classe.getMethods();
            for (Method method : methods)
                if(method.isAnnotationPresent(Url.class)) {
                    Url url = method.getAnnotation(Url.class);
                    this.MappingUrls.put(url.value(), new Mapping(classe.getName(), method.getName(), url.method()));
                }
        }
    }

    protected void retrieveSingleton(){
        for(Class classe : classes){
            if(is_singleton(classe)){
                getSingletonController().put(classe.getName(), null);
            }
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
        Mapping map = getMappingUrls().get(key);
        Object res = null;

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
                        req.getRequestDispatcher(modelview.getView()).forward(req, resp);
                    }
                    return true;
                }else{
                    resp.setContentType("application/json");
                    out.println( gson.toJson(res) );
                    return true;
                }
            } catch (Exception e) {
                out.println("- MODELVIEW NULL -");
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
        System.out.println("invokde meth " + method_name);
        // Method method = objet.getClass().getDeclaredMethod(method_name);
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
        Parameter[] params = method.getParameters();                                                            // parameters of the method
        if(params.length > 0){
            Class<?>[] method_parameter_class = arrayMethodParameter(method);                                   // method of the parameter
            String[][] args = new String[params.length][];
            for(int i = 0; i < params.length; i++){
                String[] param = parameters.get(params[i].getName());
                args[i] = param;
            }
            modelview = method.invoke(objet, dynamicCast(method_parameter_class, args));            // If there are parameters to the function
        }else modelview = method.invoke(objet); 
                                                            // if there are no parameter
        if(modelview == null) throw new Exception("The given Modelview is just null");
        return modelview;
    }

    /**
     * function who dynamically cast an Object with the matching classes
     * @param classes
     * @param args
     * @return Object array
     */
    private Object [] dynamicCast(Class<?>[]classes, String[][]args) throws Exception{
       Object[] array = new Object[classes.length];
       int i = 0;
       for (Class<?> cl:classes) {
            if(!cl.isArray())
                array[i] = cl.getDeclaredConstructor(String.class).newInstance(args[i][0]);
            else{
                Vector<Object> temps = new Vector<>();
                Class<?> componentType = cl.getComponentType();
                for (String arg : args[i])                                                              // we cast all of the argument to the component type exemple int, string etc...
                    temps.add(componentType.getDeclaredConstructor(String.class).newInstance(arg));
                Object[] temps_arr = temps.toArray();
                Object[] newArray = (Object[]) Array.newInstance(componentType, temps_arr.length);      // To cast objectArray to an array of cl, we use Array.newInstance() and pass componentType to get the component type of cl as the first argument. 
                                                                                                        // We specify the length of objectArray as the second argument to create a new array with the desired component type and length.
                System.arraycopy(temps_arr, 0, newArray, 0, temps_arr.length);                          // Finally, we use System.arraycopy() to copy the elements from objectArray to the new array.
                array[i] = newArray;
            }   i++;
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
                //Class<?>[] method_parameter_class = arrayMethodParameter(method);
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
     * @return
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
        HttpServletMapping mapping = req.getHttpServletMapping();

        // Hello
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");

        out.println("<h1> URI : " + req.getRequestURI() + "</h1>");
        out.println("<h1>" + req.getQueryString() + "</h1>");
        out.println("<h1> URL : " + req.getRequestURL() + "</h1>");
        out.println("<h1>" + mapping.getPattern() + "</h1>");
        out.println("<h1>" + mapping.getMatchValue() + "</h1>");
        out.println("<p>"+req.getContextPath()+"</p>");
        out.println("<p>"+Misc.getMappingValue(req)+"</p>");
        for (String k: getMappingUrls().keySet()) {
            out.print("key : " + k);
            out.println(" value : " + getMappingUrls().get(k).getClassName());
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
