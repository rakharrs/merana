package ambovombe.merana.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Modelview {
    
    protected String view;
    private HashMap<String, Object> data = new HashMap<>();
    private boolean invalidate; // For session
    private List<String> removeSession = new ArrayList<>();
    HashMap<String , Object> sessions = new HashMap<String, Object>();
    boolean json;

    public String getView() {
        return view;
    }
    public void addItem(String key, Object value){
        this.getData().put(key, value);
    }

    public void addSession(String key, Object value){
        this.getSessions().put(key, value);
    }

    public HashMap<String, Object> getData(){
        return this.data;
    }

    public HashMap<String, Object> getSessions() {
        return sessions;
    }

    public boolean isJson() {
        return json;
    }

    public void setJson(boolean json) {
        this.json = json;
    }

    public void setData(HashMap<String, Object> new_attributes){
        this.data = new_attributes;
    }

    public void setSessions(HashMap<String, Object> sessions) {
        this.sessions = sessions;
    }

    public void setView(String view) {
        this.view = view;
    }

    public boolean isInvalidate() {
        return invalidate;
    }

    /**
     * For checking if the session is invalid
     * @param invalidate
     */
    public void setInvalidate(boolean invalidate) {
        this.invalidate = invalidate;
    }

    public List<String> getRemoveSession() {
        return removeSession;
    }
    public void setRemoveSession(List<String> removeSession) {
        this.removeSession = removeSession;
    }
}