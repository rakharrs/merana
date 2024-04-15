package ambovombe.merana.utils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ClassRetriever {

    public static Set<Class> findAllClasses(String packageName) throws URISyntaxException, ClassNotFoundException {
        // Convert package name to directory path
        URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(packageName.replaceAll("[.]", "/"));
        File packageDir = new File(packageUrl.toURI());

        // Initialize set to hold found classes
        Set<Class> classes = new HashSet<>();

        // Recursive method to traverse directories and find classes
        findClassesRecursively(packageDir, packageName, classes);

        return classes;
    }

    private static void findClassesRecursively(File directory, String packageName, Set<Class> classes) throws ClassNotFoundException {
        // List files in the directory
        File[] files = directory.listFiles();

        // Iterate through files
        for (File file : files) {
            if (file.isDirectory()) {
                // If the file is a directory, recursively call this method
                findClassesRecursively(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                // If the file is a class file, load and add the class to the set
                String className = packageName + "." + file.getName().substring(0, file.getName().lastIndexOf("."));
                classes.add(Class.forName(className));
            }
        }
    }

    /*public static Set<Class> findAllClasses(String packageName) throws URISyntaxException, ClassNotFoundException {
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
    }*/

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
