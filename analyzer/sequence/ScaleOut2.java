package analyzer.sequence;

public class ScaleOut2 extends SequenceElement {
    
    private String nodeName;
    private String idToAssign;
    private String containerID;
    
    public ScaleOut2(String rule, String nodeName, String idToAssign, String containerID){
        this.rule = rule;
        this.nodeName = nodeName;
        this.idToAssign = idToAssign;
        this.containerID = containerID;
    }

    public String getNodeName(){
        return this.nodeName;
    }

    public String getIDToAssign(){
        return this.idToAssign;
    }

    public String getContainerID(){
        return this.containerID;
    }

    public boolean wellFormattedSequenceElement(){
        boolean ret = true;

        if(rule.equals("scaleOut1") == false)
            ret = false;
        
        if(this.nodeName == null || this.nodeName.isEmpty() == true)
            ret = false;
        
        if(this.idToAssign == null || this.idToAssign.isEmpty() == true)
            ret = false;

        if(this.containerID == null || this.containerID.isEmpty() == true)
            ret = false;
            
        return ret;
    }

}
