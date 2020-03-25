import java.util.ArrayList;
import java.util.List;

//represents the whole application
public class Application {

    //name of the application
    private final String name;
    //set T: list of application's component
    private List<Node> nodes;
    private GlobalState gState;
    // pi
    // b

    /**
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @param name application's name
     */
    public Application(String name) {
        assert name.length() > 4;
        this.name = name;
        this.nodes = new ArrayList<Node>();
        this.gState = new GlobalState(this);
    }

     /**
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @param name application's name
     * @param nodes list of applicaton's Node
     */
    public Application(String name, List<Node> nodes){
        assert name.length() > 4;
        this.name = name; 
        this.setNodes(nodes);
        this.gState = new GlobalState(this);
    }

    /**
     * @return current GlobalState
     */
    public GlobalState getGState() {
        return gState;
    }

    /**
     * @param gs given GlobalState 
     */
    public void setGState(GlobalState gs) {
        this.gState = gs;
    }

    /**
     * @return list of the application's Node
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @param nodes list of Node to bet set to the applicaton
     * @throws NullPointerException
     */
    public void setNodes(List<Node> nodes) {
        assert nodes != null;
        this.nodes = nodes;
    }

    /**
     * @return name of the applicaton
     */
    public String getName() {
        return name;
    }

    //crea una node istance di n
    public void scaleOut(Node n){

    }

    //distrugge una node istance di n
    public void scaleIn(Node n){

    }

}