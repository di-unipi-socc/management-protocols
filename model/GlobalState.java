package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//represents the current runtime state of the applicaton 
public class GlobalState {
    Application app;

    // active node instances: <NodeInstance unique id, NodeInstance>
    Map<String, NodeInstance> activeNodeInstances;

    // set of runtime binding such as:
    // id of node instance n-> list of Binding <Requirement r of n, unique id of n1
    // that satisfies r>
    Map<String, List<RuntimeBinding>> runtimeBindings;

    /**
     * @param app application of which this is the global state
     * @throws NullPointerException
     */
    public GlobalState(Application app) {
        if(app == null)
            throw new NullPointerException("app null");

        this.app = app;

        this.activeNodeInstances = new HashMap<>();
        this.runtimeBindings = new HashMap<>();
    }

    /**
     * @return map of the node instances currently active
     */
    public Map<String, NodeInstance> getActiveNodeInstances() {
        return this.activeNodeInstances;
    }

    /**
     * @return map of the runtime bindings between node instances
     */
    public Map<String, List<RuntimeBinding>> getRuntimeBindings() {
        return this.runtimeBindings;
    }

    /**
     * @param instance node instance of which it's asked the list of satisfied reqs
     * @return list of the satisfied requirement of instance
     * @throws NullPointerException
     */
    public List<Requirement> getSatisfiedReqs(NodeInstance instance) throws NullPointerException {
        if(instance == null)
            throw new NullPointerException("instance null");

        List<Requirement> satisfiedReqs = new ArrayList<>();

        // runtime bindings of instance
        List<RuntimeBinding> instanceRunBindings = this.runtimeBindings.get(instance.getID());

        for (RuntimeBinding runBinding : instanceRunBindings) {
            Node instanceType = instance.getNodeType();

            // needed for the binding function b: static binding <node, req> -> static binding <node, cap for req>
            StaticBinding reqStaticBinding = new StaticBinding(instanceType.getName(), runBinding.getReq().getName());
            // <node, capabiliity that satisfy the requirement of instance>
            StaticBinding capStaticBinding = this.app.getBindingFunction().get(reqStaticBinding);

            if (capStaticBinding != null) {
                // it's defined a static binding such as <node, req> -> <node, cap> and the
                // latter is the capStaticBinding

                // the serving instance that is currently helding the runtime binding with instance
                NodeInstance servingInstance = this.activeNodeInstances.get(runBinding.getNodeInstanceID());

                // the serving instance is the right kind of Node?
                boolean servingInsRightNodeType = servingInstance.getNodeType().getName().equals(capStaticBinding.getNodeName());
                // the serving instance is currently offering the right cap of instance?

                boolean servingInsOfferingRightCap = servingInstance.getOfferedCaps()
                        .contains(capStaticBinding.getCapOrReq());

                if (servingInsOfferingRightCap == servingInsRightNodeType == true)
                    satisfiedReqs.add(runBinding.getReq());
            }
            // TODO else ?
        }
        return satisfiedReqs;
    }

    /**
     * @param instance node instance of which we want to remove the old bindings
     * @throws NullPointerException
     */
    public void removeOldBindings(NodeInstance instance) throws NullPointerException {
        if(instance == null)
            throw new NullPointerException("instance null");

        // if a req is satisfied but not needed it has to be removed
        for (Requirement satisfiedReq : this.getSatisfiedReqs(instance)) {
            if (satisfiedReq.isContainment() == false && instance.getNeededReqs().contains(satisfiedReq) == false)
                this.removeRuntimeBinding(instance, satisfiedReq);
        }
    }

    /**
     * @param instance node instance that needs new bindings since it had a change of state
     * @throws NullPointerException
     */
    public void addNewBindings(NodeInstance instance) throws NullPointerException {
        if(instance == null)
            throw new NullPointerException("instance null");
        
        // for each neededReq (not containment) we check if that is satisfied, if it is
        // not satisfied we try to we create a binding
        for (Requirement neededReq : instance.getNeededReqs()) {
            if (neededReq.isContainment() == false && this.getSatisfiedReqs(instance).contains(neededReq) == false) {
                NodeInstance capableInstance = this.app.defaultPi(instance, neededReq);
                if(capableInstance != null)
                    this.addBinding(instance, neededReq, capableInstance);  
            }          
        }
    }

    /**
     * add a runtime binding such as <askingInstance, req, servingInstance>
     * @param askingInstance node instance asking for the requirement req
     * @param req requirement that is being asked by askingInstance
     * @param servingInstance node istance that satisfy req with the correct capability
     * @throws NullPointerException
     */
    public void addBinding(NodeInstance askingInstance, Requirement req, NodeInstance servingInstance) 
        throws NullPointerException
    {
        //TODO: questo e' pubblico ma non viene controllato che 
        //serving instance offra la giusta cap, che askingInstance richieda req
        //controlli saltati? Questo crea binding anche sbagliati se gli si passa
        //roba a caso
        //OPPURE questi controlli li faccio da altre parti?

        if(askingInstance == null)
            throw new NullPointerException("askingInstance null");
        if(req == null)
            throw new NullPointerException("req null");
        if(servingInstance == null)
            throw new NullPointerException("servingInstance null");

        if(this.runtimeBindings.get(askingInstance.getID()) == null){ 
            //means that asking instance has not yet a single binding, hence has not an entry in the runtimeBindings
            this.runtimeBindings.put(askingInstance.getID(), new ArrayList<RuntimeBinding>());
            this.runtimeBindings.get(askingInstance.getID()).add(new RuntimeBinding(req, servingInstance.getID()));
        }else
            this.runtimeBindings.get(askingInstance.getID()).add(new RuntimeBinding(req, servingInstance.getID()));
    }

    /**
     * remove a runtime binding such as <n, r, *>
     * @param instance node instance that was asking for the requirement req
     * @param req requirement that was required
     * @throws NullPointerException
     */
    public void removeRuntimeBinding(NodeInstance instance, Requirement req) throws NullPointerException{
        if(instance == null)
            throw new NullPointerException("instance null");
        
        if(req == null)
            throw new NullPointerException("req null");

        RuntimeBinding badBinding = null;

        ArrayList<RuntimeBinding> instanceRunBindings = (ArrayList<RuntimeBinding>) this.runtimeBindings.get(instance.getID());
        //we are already in a situation such as <n, ., .>
        for (RuntimeBinding runBinding : instanceRunBindings) {
            if(runBinding.getReq().equals(req) == true)
                //we have <instance, req, *> so we remove it
                badBinding = runBinding;
        }

        if(badBinding != null)
            instanceRunBindings.remove(badBinding);  

    }

    /**
     * given a node instance n this remove all the binding such as <n, *, *> and <*, *, n>
     * @param target node instance whose bindings are to be deleted. delete all bindings of the target
     * @throws NullPoninterException
     */
    public void removeAllBindingsBothWays(NodeInstance targetInstance) throws NullPointerException{

        Collection<NodeInstance> activeInstancesCollection =  this.activeNodeInstances.values();
        ArrayList<NodeInstance> activeInstances = new ArrayList<>(activeInstancesCollection);

        //direct way: targetInstance -> list of runtime bindings with other instances
        if(this.activeNodeInstances.containsKey(targetInstance.getID()) == false)
            //targetInstance was destroyed with a scaleIn
            this.runtimeBindings.remove(targetInstance.getID());
        else
            //for some reason all bindings of targetInstance must be eliminated  
            this.runtimeBindings.replace(targetInstance.getID(), new ArrayList<>());
        
        //other way; now we delete those runtime bindings that have targetInstance as the server of the requirement
        ArrayList<RuntimeBinding> otherWayBadBindings = new ArrayList<>();
        for (NodeInstance activeInstance : activeInstances) {
            
            ArrayList<RuntimeBinding> activeInstanceRunBindings = (ArrayList<RuntimeBinding>) this.runtimeBindings.get(activeInstance.getID());
            for(RuntimeBinding runBinding : activeInstanceRunBindings){ 
                //if the target instance is the capability-giver instance we remove (later) the runtime binding <activeInstance, *, targetInstance>
                if(runBinding.getNodeInstanceID().equals(targetInstance.getID()))
                    otherWayBadBindings.add(runBinding);   
            }
            for(RuntimeBinding binding : otherWayBadBindings)
                activeInstanceRunBindings.remove(binding);
        }   
        
    }

    /**
     * pending fault: a node instance require a requirement and there isn't a node
     * instance offering that capability
     * @param instance node instance of which it's asked the pending faults
     * @return list of requirement of instance that are not met 
     * @throws NullPointerException
     */
    public List<Fault> getPendingFaults(NodeInstance instance) throws NullPointerException{
        if(instance == null)
            throw new NullPointerException("instance null");

        List<Fault> faults = new ArrayList<>();

        //for each neededReq we check that it is also satisfied, if a needed requirement is not satisfied we have a fault
        for(Requirement neededReq : instance.getNeededReqs()){
            if(neededReq.isContainment() == false && this.getSatisfiedReqs(instance).contains(neededReq) == false)
                faults.add(new Fault(instance.getID(), neededReq));
        }
        return faults;
    }

    /**
     * @return list of all the pending faults of the whole application
     */
    public List<Fault> getPendingFaults(){
       List<Fault> faults = new ArrayList<>();

        Collection<NodeInstance> activeInstancesCollection =  this.activeNodeInstances.values();
        ArrayList<NodeInstance> activeInstances = new ArrayList<>(activeInstancesCollection);
        
        //for each instance we get the list of failed requirement and then we add <instance, failed req> 
        //to the list of all faults
        for(NodeInstance instance : activeInstances){
            ArrayList<Fault> instancePendingFaults = (ArrayList<Fault>) this.getPendingFaults(instance);
            faults.addAll(instancePendingFaults);
        }
        return faults;
    }

    /**
     * @param instance node instance of which we want to know if he is a broken instance
     * @return true if instance is a broken instance, false otherwise
     * @throws NullPointerException
     */
    public boolean isBrokenInstance(NodeInstance instance) throws NullPointerException{
        if(instance == null)
            throw new NullPointerException("instance null");
    
        boolean res = false;
        
        /**
         * we check if instance has a containment requirement. 
         * If so we get all the bindings of instance. 
         *  - if there is not a binding (among the existing bindings) that involves the containment requirement we have a broken instance
         *  - if the binding of the containment requirement involves a nodeInstance container that is no longer active 
         *    we have a broken instance
         *  - if there are no binding we have a broken instance
         */

        ArrayList<RuntimeBinding> instanceRunBindings = (ArrayList<RuntimeBinding>) this.runtimeBindings.get(instance.getID());
        int check = 0;

        ArrayList<Requirement> instanceReqs = (ArrayList<Requirement>) instance.getNeededReqs();
        for (Requirement req : instanceReqs) {
            if(req.isContainment() == true){

                if(instanceRunBindings == null){
                    res = true;
                    break;
                }

                if(instanceRunBindings.size() == 0)
                    res = true;

                for(RuntimeBinding binding : instanceRunBindings){
                    if(binding.getReq().equals(req)){
                        check ++;
                        //this is the binding that we have to check
                        if(this.getActiveNodeInstances().get(binding.getNodeInstanceID()) == null)
                            res = true;
                        
                        StaticBinding reqStaticBinding = new StaticBinding(instance.getNodeType().getName(), req.getName());
                        StaticBinding capStaticBinding = this.app.getBindingFunction().get(reqStaticBinding);
                        NodeInstance container = this.getActiveNodeInstances().get(binding.getNodeInstanceID());

                        // boolean containerRightKindOfNode = container.getNodeType().getName().equals(capStaticBinding.getNodeName());
                        boolean containerOfferingCap = container.getOfferedCaps().contains(capStaticBinding.getCapOrReq());
                        
                        if(containerOfferingCap == false)
                            res = true;
            
                        break;
                    } 
                }

                //among bindings there is not a binding that involves the containment req
                if(check == 0)
                    res = true;

                break;
            }
        }
        return res;
    }

    /**
     * broken instance: a node instance have a "contain" relation with a 
     * node instance that is no longer alive
     * @return list of node instances that have a broken instance
     */
    public List<NodeInstance> getBrokeninstances(){
        List<NodeInstance> brokeninstances = new ArrayList<>();

        //all active node instances
        Collection<NodeInstance> activeInstancesCollection =  this.activeNodeInstances.values();
        ArrayList<NodeInstance> activeInstances = new ArrayList<>(activeInstancesCollection);

        for(NodeInstance instance : activeInstances){
            if(this.isBrokenInstance(instance) == true)
                brokeninstances.add(instance);
        }
        return brokeninstances;    
    }

    /**
     * @param fault fault of which we want to know if it is resolvable
     * @return true if fault is resolvable
     * @throws NullPointerException
     */
    public boolean isResolvableFault(Fault fault) throws NullPointerException{
        if(fault == null)
            throw new NullPointerException("fault null");
        boolean res = false;

        //all active node instances
        Collection<NodeInstance> activeInstancesCollection =  this.activeNodeInstances.values();
        ArrayList<NodeInstance> activeInstances = new ArrayList<>(activeInstancesCollection);
        
        //data extracted from fault
        NodeInstance faultedInstance = this.activeNodeInstances.get(fault.getInstanceID());
        Node faultedNodeInstanceType = faultedInstance.getNodeType();
        Requirement faultedReq = fault.getReq();

        StaticBinding reqStaticBinding = new StaticBinding(faultedNodeInstanceType.getName(), faultedReq.getName());
        StaticBinding capStaticBinding = null;

        //a fault can be resolvable only if it is replica unware
        if(faultedReq.isReplicaUnaware() == true){
            //for each active node instance we check if it offer the right capability to resolve the fault
            for(NodeInstance instance : activeInstances){
                capStaticBinding = this.app.getBindingFunction().get(reqStaticBinding);

                //a static binding might not be defined
                if(capStaticBinding != null){
                    //instance is the right kind of Node?
                    boolean instanceRightType = instance.getNodeType().getName().equals(capStaticBinding.getNodeName());
                    //instance is currently offering the right cap of instance?
                    boolean instanceOfferingRightCap = instance.getOfferedCaps().contains(capStaticBinding.getCapOrReq()); 

                    if(instanceOfferingRightCap == instanceRightType == true){
                        res = true;
                        break;
                    }
                }
            }
        }   
        return res;
    }

    /**
     * @param instance node instance of which we want the resolvable faults
     * @return list of resolvable faults of n
     * @throws NullPointerException
     */
    public List<Fault> getResolvableFaults(NodeInstance instance) throws NullPointerException{
        if(instance == null)
            throw new NullPointerException("instance null");

        List<Fault> resolvableFaults = new ArrayList<>();
        ArrayList<Fault> pendingFaults = (ArrayList<Fault>) this.getPendingFaults(instance);

        for(Fault f : pendingFaults){
            if(this.isResolvableFault(f) == true)
                resolvableFaults.add(f);
        }
        return resolvableFaults;
    }

    /**
     * @return list of all the resolvable faults of the whole applicaton
     */
    public List<Fault> getResolvableFaults(){
        List<Fault> resolvableFault = new ArrayList<>();

        
        Collection<NodeInstance> activeInstancesCollection =  this.activeNodeInstances.values();
        ArrayList<NodeInstance> activeInstances = new ArrayList<>(activeInstancesCollection);

        for(NodeInstance n : activeInstances)
            resolvableFault.addAll(this.getResolvableFaults(n));
        
        return resolvableFault;
    }

}