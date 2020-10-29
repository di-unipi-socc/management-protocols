package mprot.lib.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mprot.lib.analyzer.execptions.*;
import mprot.lib.model.exceptions.*;
import mprot.lib.model.*;
import mprot.lib.analyzer.executable_element.*;

/**
 * idea del metodo analisi della sequenza con /pi deterministica algoritmo
 * ricorsivo che prende una lista di coppie <app, op> cioe' <app su cui
 * applicare op, op>
 * 
 * -> per la biforcazione dei fault ci serve una deep copy
 */

public class Analyzer {

    private Map<String, AnalysisFailReport> fails;

    public Map<String, AnalysisFailReport> getFailReports() {
        return this.fails;
    }

    public Analyzer() throws NullPointerException {
        this.fails = new HashMap<>();
    }

    /**
     * @param app      application on which the analysis will be executed
     * @param sequence sequence of "operation" of which the validity check is needed
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws IllegalSequenceElementException
     * @throws InstanceUnknownException
     */
    public boolean isValidSequence(Application app, List<ExecutableElement> sequence) 
        throws 
            NullPointerException,
            IllegalArgumentException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {

        List<ExecutableElement> backupSequence = this.clonePerm(sequence);
        if (app.isPiDeterministic() == true){

            if(this.deterministicIsValidSequence(app, sequence) == false){
                AnalysisFailReport fail = fails.get(app.getName());
                fail.setSequence(backupSequence);
                return false;
            }

        }
        else{
            if(this.nonDeterministicIsValidSeq(app, sequence) == false){
                AnalysisFailReport fail = fails.get(app.getName());
                fail.setSequence(backupSequence);
                return false;
            }
        }

        return true;
    }

    public List<List<RuntimeBinding>> createRunBindingCombs(Application app, String instanceID)
            throws NullPointerException, IllegalArgumentException, InstanceUnknownException {
        List<List<RuntimeBinding>> combinations = new ArrayList<>();
        NodeInstance instance = app.getGlobalState().getNodeInstanceByID(instanceID);

        Map<Requirement, List<NodeInstance>> reqToCapableInstance = new HashMap<>();

        for (Requirement r : instance.getNeededReqs())
            reqToCapableInstance.put(r, app.getGlobalState().getCapableInstances(instanceID, r));

        // cloning the neededReqs of instance to avoid the removal of the containment
        // req
        List<Requirement> neededReqs = new ArrayList<>();
        for (Requirement r : instance.getNeededReqs())
            neededReqs.add(r);

        Requirement containmentReq = null;
        for (Requirement r : neededReqs) {
            if (r.isContainment() == true)
                containmentReq = r;
        }

        if (containmentReq != null)
            neededReqs.remove(containmentReq);

        int reqsStart = 0;
        int reqsEnd = neededReqs.size() - 1;

        if (reqsEnd <= 0)
            return combinations;

        this.recursiveRunBindingPerm(reqsStart, reqsEnd, neededReqs, reqToCapableInstance, combinations,
                new ArrayList<RuntimeBinding>());

        return combinations;
    }

    public void recursiveRunBindingPerm(int reqsStart, int reqsEnd, List<Requirement> neededReqs,
            Map<Requirement, List<NodeInstance>> reqToCapableInstance, List<List<RuntimeBinding>> combinations,
            List<RuntimeBinding> currentCombination) {

        Requirement req = neededReqs.get(reqsStart);
        ArrayList<NodeInstance> capableInstance = (ArrayList<NodeInstance>) reqToCapableInstance.get(req);

        List<RuntimeBinding> combination = null;

        if (reqsEnd == reqsStart) {
            for (NodeInstance i : capableInstance) {
                RuntimeBinding newRunBinding = new RuntimeBinding(req, i.getID());

                combination = this.cloneList(currentCombination);
                combination.add(newRunBinding);
                combinations.add(combination);
            }
            return;

        } else {
            for (NodeInstance i : capableInstance) {
                combination = this.cloneList(currentCombination);
                combination.add(new RuntimeBinding(req, i.getID()));
                this.recursiveRunBindingPerm(reqsStart + 1, reqsEnd, neededReqs, reqToCapableInstance, combinations,
                        combination);
            }
        }
        return;
    }

    private List<RuntimeBinding> cloneList(List<RuntimeBinding> currentCombination) {
        List<RuntimeBinding> ret = new ArrayList<>();

        for (RuntimeBinding runtimeBinding : currentCombination)
            ret.add(new RuntimeBinding(runtimeBinding.getReq(), runtimeBinding.getNodeInstanceID()));

        return ret;
    }

    /**
     * @param app      application on which the analysis will be executed
     * @param sequence sequence of "operation" of which the validity check is needed
     * @return
     * @throws NullPointerException
     * @throws IllegalSequenceElementException
     * @throws InstanceUnknownException
     */
    public boolean isWeaklyValidSequence(Application app, List<ExecutableElement> sequence)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    
    {
        List<ExecutableElement> backupSequence = this.clonePerm(sequence);

        if (app.isPiDeterministic() == true){
            if(this.deterministicIsWeaklyValidSequence(app, sequence) == true)
                return true;

        } else{
            if(this.nonDeterministicIsWeaklyValidSeq(app, sequence) == true)
                return true;
        }

        AnalysisFailReport fail = fails.get(app.getName());
        fail.setSequence(backupSequence);
        return false;
    }

    public boolean isNotValidSequence(Application app, List<ExecutableElement> sequence)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {
        if (app.isPiDeterministic() == true)
            return !this.deterministicIsWeaklyValidSequence(app, sequence);
        else
            return !this.nonDeterministicIsWeaklyValidSeq(app, sequence);
    }

    private boolean deterministicIsValidSequence(Application app, List<ExecutableElement> sequence)
        throws 
            IllegalSequenceElementException, 
            NullPointerException, 
            IllegalArgumentException, 
            InstanceUnknownException
            
    {
        if (app == null)
            throw new NullPointerException();

        if (this.wellFormattedSequence(sequence) == false)
            throw new IllegalSequenceElementException();

        if (sequence.isEmpty() == true)
            return true;

        // pop one element from the sequence than execute it
        ExecutableElement seqElement = sequence.remove(0);
        try {
            app.execute(seqElement);
        } catch(FailedOperationException e){
            // keep going, this will be handled by the fault handler or leaved as it is
        } catch (Exception e) {
            fails.put(app.getName(), new AnalysisFailReport(seqElement, e));
            return false;
        }

        //for every fault the tree has n new biforcation to check
        if(this.checkFaultsValid(app, sequence, true) == false)
            return false;

        return true;
    }

    private boolean deterministicIsWeaklyValidSequence(Application app, List<ExecutableElement> sequence)
        throws 
            IllegalSequenceElementException, 
            NullPointerException,  
            InstanceUnknownException 
    {

        if (app == null)
            throw new NullPointerException();

        if (this.wellFormattedSequence(sequence) == false)
            throw new IllegalSequenceElementException();

        if (sequence.isEmpty() == true)
            return true;

        ExecutableElement seqElement = sequence.remove(0);
        try {
            app.execute(seqElement);
        } catch (FailedOperationException e) {
            // keep going, this will be handled by the fault handler or leaved as it is
        } catch (Exception e) {
            fails.put(app.getName(), new AnalysisFailReport(seqElement, e));
            return false;
        }

        if(this.checkFaultsWeaklyValid(app, sequence, true) == true)
            return true;
        
        return false;
    }

    private boolean nonDeterministicIsWeaklyValidSeq(Application app, List<ExecutableElement> sequence)
        throws 
            IllegalSequenceElementException, 
            NullPointerException, 
            InstanceUnknownException 
    {
        if (app == null)
            throw new NullPointerException();

        if (this.wellFormattedSequence(sequence) == false)
            throw new IllegalSequenceElementException();

        if (sequence.isEmpty() == true)
            return true;        

        ExecutableElement op = sequence.remove(0);

        //for each kind of op there is a specific check
        if (op instanceof OpStart) {
            if(this.nonDetOpStartOpEnd(app, op, true, sequence) == true)
                return true;
        }
        if (op instanceof OpEnd) {
            if(this.nonDetOpStartOpEnd(app, op, true, sequence) == true)
                return true;
        }
        if (op instanceof ScaleIn) {
            if(this.nonDetScaleIn(app, op, true, sequence) == true)
                return true;
        }
        if (op instanceof ScaleOut1) {
            if(this.nonDetScaleOut1(app, op, true, sequence) == true)
                return true;
        }
        if (op instanceof ScaleOut2){
            if(this.nonDetScaleOut2(app, op, true, sequence) == true)
                return true;
        }

        return false;
    }

    private boolean nonDeterministicIsValidSeq(Application app, List<ExecutableElement> sequence)
        throws 
            IllegalSequenceElementException, 
            NullPointerException, 
            InstanceUnknownException 
    {
        if (app == null)
            throw new NullPointerException();

        if (this.wellFormattedSequence(sequence) == false)
            throw new IllegalSequenceElementException();

        if (sequence.isEmpty() == true)
            return true;        
        
        ExecutableElement op = sequence.remove(0);

        if (op instanceof OpStart) {
            if(this.nonDetOpStartOpEnd(app, op, false, sequence) == false)
                return false;
        }

        if (op instanceof OpEnd) {
            if(this.nonDetOpStartOpEnd(app, op, false, sequence) == false)
                return false;
            
        }
        if (op instanceof ScaleIn){
            if(this.nonDetScaleIn(app, op, false, sequence) == false)
                return false;
        
        }
        if (op instanceof ScaleOut1) {
            if(this.nonDetScaleOut1(app, op, false, sequence) == false)
                return false;
            
        }
        if (op instanceof ScaleOut2){
            if(this.nonDetScaleOut2(app, op, false, sequence) == false)
                return false;
            
        }

        return true;
    }

    private boolean wellFormattedSequence(List<ExecutableElement> sequence){
        boolean res = true;
        
        for (ExecutableElement element : sequence) { 
            if (element.wellFormattedSequenceElement() == false)
                res = false;
        }

        return res;
    }
    
    private boolean checkFaultsValid(Application app, List<ExecutableElement> sequence, boolean isDeterministic)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {

        if(isDeterministic == false){
            ArrayList<Fault> pendingFaults = (ArrayList<Fault>) app.getGlobalState().getPendingFaults();
            ArrayList<NodeInstance> brokenInstances = (ArrayList<NodeInstance>) app.getGlobalState().getBrokeninstances();
    
            Application cloneApp = null;
    
            if (brokenInstances.isEmpty() == false) {
                cloneApp = app.clone();

                String toKillID = brokenInstances.get(0).getID();
                try {
                    cloneApp.scaleIn(toKillID);
                    if (this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                        return false;
    
                } catch (RuleNotApplicableException | InstanceUnknownException e) {
                    this.fails.put(app.getName(), new AnalysisFailReport(toKillID, e));
                    return false;
                }
                cloneApp = app.clone(); // clone this to reset the clone well
            }
    
            cloneApp = app.clone();
    
            if (this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                return false;
    
            if (pendingFaults.isEmpty() == false) {
                // for each fault check if it is pending or resolvable
                for (Fault f : pendingFaults) {
                    cloneApp = app.clone();
    
                    if (app.getGlobalState().isResolvableFault(f) == true) {
                        try {
                            cloneApp.autoreconnect(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                                return false;
                        } catch (RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        }
                    } else {
                        try {
                            cloneApp.fault(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                                return false;
    
                        } catch (FailedFaultHandlingExecption | RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        } 
                    }
                }
            }
            return true;
        }else{
            ArrayList<Fault> pendingFaults = (ArrayList<Fault>) app.getGlobalState().getPendingFaults();
            ArrayList<NodeInstance> brokenInstances = (ArrayList<NodeInstance>) app.getGlobalState().getBrokeninstances();

            Application cloneApp = null;

            if (brokenInstances.isEmpty() == false) {
                cloneApp = app.clone();
                String toKillID = brokenInstances.get(0).getID();
                try {
                    cloneApp.scaleIn(toKillID);
                    if (this.deterministicIsValidSequence(cloneApp, sequence) == false)
                        return false;

                } catch (RuleNotApplicableException | InstanceUnknownException e) {
                    this.fails.put(app.getName(), new AnalysisFailReport(toKillID, e));
                    return false;
                }
                cloneApp = app.clone(); // clone this to reset the clone well
            }

            cloneApp = app.clone();

            if (this.deterministicIsValidSequence(cloneApp, sequence) == false)
                return false;

            if (pendingFaults.isEmpty() == false) {
                // for each fault check if it is pending or resolvable
                for (Fault f : pendingFaults) {
                    cloneApp = app.clone();

                    if (app.getGlobalState().isResolvableFault(f) == true) {
                        try {
                            cloneApp.autoreconnect(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.deterministicIsValidSequence(cloneApp, sequence) == false)
                                return false;
                        } catch (RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        }
                    } else {
                        try {
                            cloneApp.fault(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.deterministicIsValidSequence(cloneApp, sequence) == false)
                                return false;

                        } catch (FailedFaultHandlingExecption | RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        } 
                    }
                }
            }
            return true;
        }

    }

    private boolean checkFaultsWeaklyValid(Application app, List<ExecutableElement> sequence, boolean isDeterministic)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            IllegalArgumentException, 
            InstanceUnknownException 
    {
        if(isDeterministic == false){
            ArrayList<Fault> pendingFaults = (ArrayList<Fault>) app.getGlobalState().getPendingFaults();
            ArrayList<NodeInstance> brokenInstances = (ArrayList<NodeInstance>) app.getGlobalState().getBrokeninstances();
    
            Application cloneApp = null;
    
            if (brokenInstances.isEmpty() == false) {
                cloneApp = app.clone();

                String toKillID = brokenInstances.get(0).getID();
                try {
                    cloneApp.scaleIn(brokenInstances.get(0).getID());
                    if (this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                        return true;
    
                } catch (RuleNotApplicableException | InstanceUnknownException e) {
                    this.fails.put(app.getName(), new AnalysisFailReport(toKillID, e));
                    return false;
                }
                cloneApp = app.clone(); // clone this to reset the clone well
            }
    
            cloneApp = app.clone();
    
            if (this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                return true;
    
            if (pendingFaults.isEmpty() == false) {
                // for each fault check if it is pending or resolvable
                for (Fault f : pendingFaults) {
                    cloneApp = app.clone();
    
                    if (app.getGlobalState().isResolvableFault(f) == true) {
                        try {
                            cloneApp.autoreconnect(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                                return true;
                        } catch (RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        }
                    } else {
                        try {
                            cloneApp.fault(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                                return true;
    
                        } catch (FailedFaultHandlingExecption | RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        } 
                    }
                }
            }
            return false;
        }else{
            ArrayList<Fault> pendingFaults = (ArrayList<Fault>) app.getGlobalState().getPendingFaults();
            ArrayList<NodeInstance> brokenInstances = (ArrayList<NodeInstance>) app.getGlobalState().getBrokeninstances();

            Application cloneApp = null;

            if (brokenInstances.isEmpty() == false) {
                cloneApp = app.clone();

                String toKillID = brokenInstances.get(0).getID();
                try {
                    cloneApp.scaleIn(brokenInstances.get(0).getID());
                    if (this.deterministicIsWeaklyValidSequence(cloneApp, sequence) == true)
                        return true;

                } catch (RuleNotApplicableException | InstanceUnknownException e) {
                    this.fails.put(app.getName(), new AnalysisFailReport(toKillID, e));
                    return false;
                }
                cloneApp = app.clone(); // clone this to reset the clone well
            }

            cloneApp = app.clone();

            if (this.deterministicIsWeaklyValidSequence(cloneApp, sequence) == true)
                return true;

            if (pendingFaults.isEmpty() == false) {
                // for each fault check if it is pending or resolvable
                for (Fault f : pendingFaults) {
                    cloneApp = app.clone();

                    if (app.getGlobalState().isResolvableFault(f) == true) {
                        try {
                            cloneApp.autoreconnect(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.deterministicIsWeaklyValidSequence(cloneApp, sequence) == true)
                                return true;
                        } catch (RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        }
                    } else {
                        try {
                            cloneApp.fault(f.getInstanceID(), f.getReq());
                            // if the fault is resolved keep exploring the branch
                            if (this.deterministicIsWeaklyValidSequence(cloneApp, sequence) == true)
                                return true;

                        } catch (FailedFaultHandlingExecption | RuleNotApplicableException | InstanceUnknownException e) {
                            fails.put(app.getName(), new AnalysisFailReport(e, f));
                            return false;
                        } 
                    }
                }
            }
            return false;
        }
    }



    public Map<ExecutableElement, List<ExecutableElement>> buildConstraintMap(List<ExecutableElement> planExecutableElements, List<Constraint> constraints){

        Map<ExecutableElement, List<ExecutableElement>> constraintsMap = new HashMap<>();

        for(ExecutableElement elem : planExecutableElements)
            constraintsMap.put(elem, new ArrayList<ExecutableElement>());

        for(Constraint constraint : constraints)
            constraintsMap.get(constraint.getBefore()).add(constraint.getAfter());

        return constraintsMap;
    }

    private List<ExecutableElement> clonePerm(List<ExecutableElement> perm){

        List<ExecutableElement> clone = new ArrayList<>();

        for (ExecutableElement executableElement : perm) 
            clone.add(executableElement);
        
        return clone;

    }

    
    //planExecutableElement is going to be altered, if needed always pass a clone
    public boolean planValidity(Application app, boolean weaklyValid, List<ExecutableElement> planExecutableElements, Map<ExecutableElement, List<ExecutableElement>> constraintsMap)
        throws 
            NullPointerException, 
            IllegalArgumentException, 
            IllegalSequenceElementException,
            InstanceUnknownException 
    {
        
        if(this.checkConstraints(planExecutableElements, constraintsMap) == true){
            if(weaklyValid == true){
                if(this.isWeaklyValidSequence(app.clone(), this.clonePerm(planExecutableElements)) == true)
                    return true;
            }
            else{
                if(this.isValidSequence(app.clone(), this.clonePerm(planExecutableElements)) == false)
                    return false;
            }
        }

        int permSize = planExecutableElements.size();

        //creates all perms and check one by one (heap algorithm)
        int[] c = new int[permSize];        
        for(int i = 0; i < permSize; i++)
            c[i] = 0;
        
        int i = 0;
        while(i < permSize){
            
            if(c[i] < i){
                
                if(i % 2 == 0)
                    Collections.swap(planExecutableElements, 0, i);
                else
                    Collections.swap(planExecutableElements, i, c[i]);

                if(this.checkConstraints(planExecutableElements, constraintsMap) == true){
                    if(weaklyValid == true){
                        if(this.isWeaklyValidSequence(app.clone(), this.clonePerm(planExecutableElements)) == true)
                            return true;
                    }
                    else{
                        if(this.isValidSequence(app.clone(), this.clonePerm(planExecutableElements)) == false)
                            return false;
                    }
                }
                
                c[i]++;
                i = 0;
            
            }else{
                c[i] = 0;
                i++;
            }
                                
        }
        
        return true;
    }


    public boolean isValidPlan(Application app, List<ExecutableElement> planExecutableElements, List<Constraint> constraints)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {   
        //e1 -> [e2, e3, ...]: match e1 with the executable elements that must be executed after e1
        Map<ExecutableElement, List<ExecutableElement>> constraintsMap = this.buildConstraintMap(planExecutableElements, constraints);
        return this.planValidity(app, false, planExecutableElements, constraintsMap);
    }

    public boolean isWeaklyValidPlan(Application app, List<ExecutableElement> planExecutableElements, List<Constraint> constraints)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {
        //e1 -> [e2, e3, ...]: match e1 with the executable elements that must be executed after e1
        Map<ExecutableElement, List<ExecutableElement>> constraintsMap = this.buildConstraintMap(planExecutableElements, constraints);
        return this.planValidity(app, true, planExecutableElements, constraintsMap);
    
    }

    public boolean isNotValidPlan(Application app, List<ExecutableElement> planExecutableElements, List<Constraint> constraints)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {
        return !this.isWeaklyValidPlan(app, planExecutableElements, constraints);
    }

    public boolean checkConstraints(List<ExecutableElement> sequence, Map<ExecutableElement, List<ExecutableElement>> constraintsMap){
        
        for(ExecutableElement elem : sequence){

            List<ExecutableElement> afterElements = constraintsMap.get(elem);

            if(afterElements.isEmpty() == false){

                List<ExecutableElement> subSequence = sequence.subList(sequence.indexOf(elem) + 1, sequence.size());

                if(subSequence.isEmpty() == true)
                    return false;
                
                for(ExecutableElement afterElem : afterElements){
                    if(subSequence.contains(afterElem) == false)
                        return false;
                }
            }
        }

        return true;
    }


    private boolean nonDetOpStartOpEnd(Application app, ExecutableElement op, boolean weaklyValid, List<ExecutableElement> sequence)
        throws 
            NullPointerException, 
            IllegalArgumentException, 
            InstanceUnknownException,
            IllegalSequenceElementException 
    {

        if(op instanceof OpStart){

            OpStart todo = (OpStart) op;
            NodeInstance instance = app.getGlobalState().getNodeInstanceByID(todo.getInstnaceID());
    
            //requirements needed by instance before doing the op
            List<Requirement> neededReqsBeforeOp = instance.getNeededReqs();
            try {
                app.execute(todo);
            } catch (FailedOperationException e) {
                // keep going, this will be handled by the fault handler or leaved as it is
            } catch (Exception e) {
                fails.put(app.getName(), new AnalysisFailReport(todo, e));
                return false;
            }
            
            //check the fault branches
            if(weaklyValid == true){
                if(this.checkFaultsWeaklyValid(app, sequence, false) == true)
                    return true;
            }else{
                if(this.checkFaultsValid(app, sequence, false) == false)
                    return false;
            }

            //requirements needed after doing the op
            List<Requirement> neededReqsAfterOp = instance.getNeededReqs();
    
            //here will be the requirements that were needed before op and are still needed
            List<Requirement> neededReqsBeforeAndAfter = new ArrayList<>();
            for (Requirement req : neededReqsBeforeOp) {
                if (neededReqsAfterOp.contains(req))
                    neededReqsBeforeAndAfter.add(req);
            }
    
            //current runtime binding of instance
            List<RuntimeBinding> instanceRuntimeBinding = app.getGlobalState().getRuntimeBindings().get(instance.getID());
    
            //here will be the runtime binding that are about the requirement that was and are still needed 
            //those runtime bindings will not be changed with the combinations
            List<RuntimeBinding> runtimeBindingBeforeAndAfter = new ArrayList<>();
    
            for (RuntimeBinding rb : instanceRuntimeBinding) {
                for (Requirement req : neededReqsBeforeAndAfter) {
                    if (req.equals(rb.getReq()))
                        runtimeBindingBeforeAndAfter.add(rb);
                }
            }
            
            List<List<RuntimeBinding>> combinations = this.createRunBindingCombs(app, todo.getInstnaceID());

            if(weaklyValid == true){
                if(combinations.isEmpty() == true)
                    return this.nonDeterministicIsWeaklyValidSeq(app.clone(), sequence);
            }else{
                if(combinations.isEmpty() == true)
                    return this.nonDeterministicIsValidSeq(app.clone(), sequence);
            }



            Application cloneApp = null;

            //for each combination of runtime bindings we check if the combination contains the "unchangable" runtimebindings
            //if so we complement with the new runtime binding, checking for each possible combination
            for (List<RuntimeBinding> comb : combinations) {
                if (comb.containsAll(runtimeBindingBeforeAndAfter) == true) {
                    cloneApp = app.clone();
                    for (RuntimeBinding rb : comb) 
                        cloneApp.getGlobalState().addBinding(instance.getID(), rb.getReq(), rb.getNodeInstanceID());
                    
                    if(weaklyValid == true){
                        if(this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                            return true;
                    }else{
                        if(this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                        return false;
                    }
                    
                }
            }

        }else if(op instanceof OpEnd){

            OpEnd todo = (OpEnd) op;
            NodeInstance instance = app.getGlobalState().getActiveNodeInstances().get(todo.getInstanceID());

            List<Requirement> neededReqsBeforeOp = instance.getNeededReqs();
            
            try {
                app.execute(todo);
            } catch (FailedOperationException e) {
                // keep going, this will be handled by the fault handler or leaved as it is
            } catch (Exception e) {
                fails.put(app.getName(), new AnalysisFailReport(todo, e));
                return false;
            }

            //check the fault branches
            if(weaklyValid == true){
                if(this.checkFaultsWeaklyValid(app, sequence, false) == true)
                    return true;
            }else{
                if(this.checkFaultsValid(app, sequence, false) == false)
                    return false;
            }

            List<Requirement> neededReqsAfterOp = instance.getNeededReqs();
            List<Requirement> neededReqsBeforeAndAfter = new ArrayList<>();

            for (Requirement req : neededReqsBeforeOp) {
                if (neededReqsAfterOp.contains(req))
                    neededReqsBeforeAndAfter.add(req);
            }

            List<RuntimeBinding> instanceRuntimeBinding = app.getGlobalState().getRuntimeBindings().get(instance.getID());
            List<RuntimeBinding> runtimeBindingBeforeAndAfter = new ArrayList<>();

            for (RuntimeBinding rb : instanceRuntimeBinding) {
                for (Requirement req : neededReqsBeforeAndAfter) {
                    if (req.equals(rb.getReq()))
                        runtimeBindingBeforeAndAfter.add(rb);
                }
            }


            List<List<RuntimeBinding>> combinations = this.createRunBindingCombs(app, todo.getInstanceID());

            if(weaklyValid == true){
                if(combinations.isEmpty() == true)
                    return this.nonDeterministicIsWeaklyValidSeq(app.clone(), sequence);
            }else{
                if(combinations.isEmpty() == true)
                    return this.nonDeterministicIsValidSeq(app.clone(), sequence);
            }

            Application cloneApp = null;

            for (List<RuntimeBinding> comb : combinations) {
                if (comb.containsAll(runtimeBindingBeforeAndAfter) == true) {
                    cloneApp = app.clone();
                    for (RuntimeBinding rb : comb) 
                        cloneApp.getGlobalState().addBinding(instance.getID(), rb.getReq(), rb.getNodeInstanceID());
                    
                    if(weaklyValid == true){
                        if(this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                            return true;
                    }else{
                        if(this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                            return false;
                    }
                }
            }
        }
       
        return false;
    }

    private boolean nonDetScaleIn(Application app, ExecutableElement op, boolean weaklyValid, List<ExecutableElement> sequence)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {
        ScaleIn todo = (ScaleIn) op;

        try {
            app.execute(todo);
        } catch (FailedOperationException e) {
            // keep going, this will be handled by the fault handler or leaved as it is
        } catch (Exception e) {
            fails.put(app.getName(), new AnalysisFailReport(todo, e));
            return false;
        }

        if(weaklyValid == true){
            if(this.checkFaultsWeaklyValid(app, sequence, false) == true)
                return true;
        }else{
            if(this.checkFaultsValid(app, sequence, false) == true)
                return true;
        }
            
        if(weaklyValid == true){
            if(this.nonDeterministicIsWeaklyValidSeq(app.clone(), sequence) == true)
                return true;
        }else{
            if(this.nonDeterministicIsValidSeq(app.clone(), sequence) == false)
                return false;
        }

        return false;
    }

    private boolean nonDetScaleOut1(Application app, ExecutableElement op, boolean weaklyValid, List<ExecutableElement> sequence)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {

        ScaleOut1 todo = (ScaleOut1) op;

        try {
            app.execute(todo);
        } catch (FailedOperationException e) {
            // keep going, this will be handled by the fault handler or leaved as it is
        } catch (Exception e) {
            fails.put(app.getName(), new AnalysisFailReport(todo, e));
            return false;
        }

        if(weaklyValid == true){
            if(this.checkFaultsWeaklyValid(app, sequence, false) == true)
                return true;
        }else{
            if(this.checkFaultsValid(app, sequence, false) == false)
                return false;
        }
        
        //ScaleOut1 creates automatically the needed runtime binding but we want every possible cominations
        //so we removes the bindings and go with the combinations
        //TODO: chiedi se e' giusto both ways oppure quelli devono essere lasciati
        app.getGlobalState().removeAllBindingsBothWays(todo.getIDToAssign());

        List<List<RuntimeBinding>> combinations = this.createRunBindingCombs(app, todo.getIDToAssign());

        Application cloneApp = null;
        
        if(combinations.isEmpty() == true){
            if(weaklyValid == true)
                return this.nonDeterministicIsWeaklyValidSeq(app.clone(), sequence);
            else
                return this.nonDeterministicIsValidSeq(app.clone(), sequence);
        }
       
        for(List<RuntimeBinding> comb : combinations){
            cloneApp = app.clone();
            for(RuntimeBinding rb : comb)
                cloneApp.getGlobalState().addBinding(todo.getIDToAssign(), rb.getReq(), rb.getNodeInstanceID());
            
            if(weaklyValid == true){
                if(this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                    return true;
            }else{
                if(this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                    return false;
            }
        }

        return false;
    }

    private boolean nonDetScaleOut2(Application app, ExecutableElement op, boolean weaklyValid, List<ExecutableElement> sequence)
        throws 
            NullPointerException, 
            IllegalSequenceElementException, 
            InstanceUnknownException 
    {
        ScaleOut2 todo = (ScaleOut2) op;

        try {
            app.execute(todo);
        } catch (FailedOperationException e) {
            // keep going, this will be handled by the fault handler or leaved as it is
        } catch (Exception e) {
            fails.put(app.getName(), new AnalysisFailReport(todo, e));
            return false;
        }
        
        if(weaklyValid == true){
            if(this.checkFaultsWeaklyValid(app, sequence, false) == true)
                return true;
        }else{
            if(this.checkFaultsValid(app, sequence, false) == false)
                return false;
        }
        

        RuntimeBinding containmentRB = null;
        for(RuntimeBinding rb : app.getGlobalState().getRuntimeBindings().get(todo.getIDToAssign())){
            if(rb.getReq().isContainment() == true)
                containmentRB = rb;
        }

        app.getGlobalState().removeAllBindingsBothWays(todo.getIDToAssign());
        app.getGlobalState().addBinding(todo.getIDToAssign(), containmentRB.getReq(), todo.getContainerID());

        
        List<List<RuntimeBinding>>combinations = this.createRunBindingCombs(app, todo.getIDToAssign());

        if(combinations.isEmpty() == true){
            if(weaklyValid == true)
                return this.nonDeterministicIsWeaklyValidSeq(app.clone(), sequence);
            else
                return this.nonDeterministicIsValidSeq(app.clone(), sequence);
        }
            
        Application cloneApp = null;

        for(List<RuntimeBinding> comb : combinations){
            cloneApp = app.clone();
            for(RuntimeBinding rb : comb)
                cloneApp.getGlobalState().addBinding(todo.getIDToAssign(), rb.getReq(), rb.getNodeInstanceID());
            
            if(weaklyValid == true){
                if(this.nonDeterministicIsWeaklyValidSeq(cloneApp, sequence) == true)
                    return true;
            }else{
                if(this.nonDeterministicIsValidSeq(cloneApp, sequence) == false)
                    return false;
            }
            
        }

        return false;
    }

}