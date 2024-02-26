package ambovombe.merana.process;
public class Fileupload {
    String name;            // File name
    String path;            // File path
    byte[] content;         // File content

    public Fileupload(){}
    public Fileupload(String name, byte[] content){
        setName(name);
        setContent(content);
    }
    public Fileupload(String name, String path, byte[] content){
        setName(name);
        setPath(path);
        setContent(content);
    }

    public void upload(){}

    public void upload(String new_path){}

    public byte[] getContent() {
        return content;
    }
    public String getName() {
        return name;
    }
    public String getPath() {
        return path;
    }
    public void setContent(byte[] content) {
        this.content = content;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPath(String path) {
        this.path = path;
    }
}