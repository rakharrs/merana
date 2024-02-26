package ambovombe.merana.utils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ClassRetriever {
    public static Set<Class> findAllClasses(String packageName) throws URISyntaxException, ClassNotFoundException {
        System.out.println(packageName);
        URL stream = Thread.currentThread().getContextClassLoader().getResource(packageName.replaceAll("[.]", "/"));
        File dir = new File(stream.toURI());
        File[] files = dir.listFiles(file -> file.getName().endsWith(".class"));
        Set<Class> classes = new HashSet<>();
        for (File file: files) {
            System.out.println(file.getName());
            String c = packageName + "." + file.getName().substring(0, file.getName().lastIndexOf("."));
            classes.add(Class.forName(c));
        }
        return classes;
    }

    public static Set<Class> find_classes(String package_name) throws ClassNotFoundException {
        String path = package_name.replaceAll("[.]", "/");
        File dir = new File("path");
        FileFilter filter = file -> file.getName().endsWith(".class");
        File[] classe_files = dir.listFiles(filter);
        Set<Class> classes = new HashSet<>();
        for(File file : classe_files)
            classes.add(Class.forName(path + file.getName()));
        return classes;
    }


    private static Class getClass(String className, String packageName){
        try{
            return Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')));
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws URISyntaxException, ClassNotFoundException {
        Set<Class> a = findAllClasses("etu1999.framework.controller");
        System.out.println(a);
    }
}
