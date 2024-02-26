package ambovombe.merana.utils;

import ambovombe.merana.Mapping;
import ambovombe.merana.process.Fileupload;
import ambovombe.merana.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class Misc {

    // Maka anle valeur ao aorian'ny context
    public static String getMappingValue(HttpServletRequest request){
        String value = new String();
        String URI = request.getRequestURI();
        String context_path = request.getContextPath();
        value = URI.substring(context_path.length());
        return value;
    }

        /**
     * Get the matching part with the partname
     * @param partName
     * @param parts
     * @return
     */
    public static Part getPart(String partName, Collection<Part> parts){
        return parts.stream().filter(part -> part.getName().equals(partName)).findFirst().orElse(null);
    }

    public static ArrayList<Part> getParts(String partName, Collection<Part> parts){
        ArrayList<Part> results = new ArrayList<>();
        for (Part part : parts)
            if(part.getName().equals(partName))
                results.add(part);
        return results;
    }
    /**
     * Instantiate Fileupload class from Part class
     * @param part
     * @return
     * @throws IOException
     */
    public static Fileupload makeFileUpload(Part part) throws IOException{
        return new Fileupload(part.getSubmittedFileName(), part.getInputStream().readAllBytes());
    }

    public static Fileupload[] makeFileUploads(List<Part> parts) throws IOException{
        Vector<Fileupload> results = new Vector<>();
        for (Part part : parts)
            results.add(makeFileUpload(part));
        return results.toArray(new Fileupload[0]);
    }
}
