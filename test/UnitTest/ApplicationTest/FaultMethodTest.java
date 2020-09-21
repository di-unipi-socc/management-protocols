package test.UnitTest.ApplicationTest;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.*;
import exceptions.AlreadyUsedIDException;
import exceptions.FailedFaultHandlingExecption;
import exceptions.InstanceUnknownException;
import exceptions.NodeUnknownException;
import exceptions.RuleNotApplicableException;

public class FaultMethodTest {

    public Application testApp;
    public Node nodeA;
    public Node nodeB;
    public Node nodeC;

    // the faulty instance
    public NodeInstance instanceOfA;

    // the sane instance
    public NodeInstance instanceOfB;

    // the not having fault handling state instance
    public NodeInstance instanceOfC;

    public Requirement testReq;

    /**
     * creates a custom simple application with 3 nodes, nodeA, nodeB and nodeC.
     * nodeB do not offer any cap nor need any requirements nodeA has a requirement
     * and has a fault handling state nodeC has a requirement but do not has a fault
     * handling state instanceOf* and the instances of node
     * 
     * @throws AlreadyUsedIDException
     * @throws InstanceUnknownException
     * @throws IllegalArgumentException*
     */

    @Before
    public void setUp() 
        throws 
            NullPointerException, 
            RuleNotApplicableException, 
            NodeUnknownException,
            IllegalArgumentException, 
            InstanceUnknownException, 
            AlreadyUsedIDException
    {
        this.testReq = new Requirement("testReq", RequirementSort.REPLICA_UNAWARE);

        this.nodeA = this.createNodeA();
        this.nodeB = this.createNodeB();
        this.nodeC = this.createNodeC();

        this.testApp = new Application("testApp");

        this.testApp.addNode(this.nodeA);
        this.testApp.addNode(this.nodeB);
        this.testApp.addNode(this.nodeC);
        
        this.instanceOfA = this.testApp.scaleOut1(this.nodeA.getName(), "instanceOfA");
        this.instanceOfB = this.testApp.scaleOut1(this.nodeB.getName(), "instanceOfB");
        this.instanceOfC = this.testApp.scaleOut1(this.nodeC.getName(), "instanceOfC");
    }

    public Node createNodeA(){
        Node ret = new Node("nodeA", "state1", new ManagementProtocol());
        ManagementProtocol mp = ret.getManagementProtocol();

        ret.addState("state1");
        ret.addState("faultHandlingStatte");

        ret.addRequirement(this.testReq);
        
        List<Requirement> testReqs = new ArrayList<>();
        testReqs.add(this.testReq);
        mp.addRhoEntry("state1", testReqs);
        mp.addRhoEntry("faultHandlingState", new ArrayList<Requirement>());

        //gamma: state -> caps offered in that state
        for (String state : ret.getStates())
            mp.addGammaEntry(state, new ArrayList<String>());

        List<String> faultHandlingStates = new ArrayList<>();
        faultHandlingStates.add("faultHandlingState");
        mp.addPhiEntry("state1", faultHandlingStates);

        return ret;
    }

    public Node createNodeB(){
        Node ret = new Node("nodeB", "state1", new ManagementProtocol());
        ManagementProtocol mp = ret.getManagementProtocol();

        ret.addState("state1");
       
        for (String state : ret.getStates())
            mp.addRhoEntry(state, new ArrayList<Requirement>());

        //gamma: state -> caps offered in that state
        for (String state : ret.getStates())
            mp.addGammaEntry(state, new ArrayList<String>());

        for (String state : ret.getStates()) 
            mp.addPhiEntry(state, new ArrayList<String>());
        
        return ret;
    }

    public Node createNodeC(){
        Node ret = new Node("nodeC", "state1", new ManagementProtocol());
        ManagementProtocol mp = ret.getManagementProtocol();

        ret.addState("state1");

        ret.addRequirement(this.testReq);
        
        List<Requirement> testReqs = new ArrayList<>();
        testReqs.add(this.testReq);
        mp.addRhoEntry("state1", testReqs);

        //gamma: state -> caps offered in that state
        for (String state : ret.getStates())
            mp.addGammaEntry(state, new ArrayList<String>());

        for (String state : ret.getStates()) 
            mp.addPhiEntry(state, new ArrayList<String>());

        return ret;
    }

    //fault throws a NullPointerException when the passed instanceID is null
    @Test(expected = NullPointerException.class)
    public void faultMethodNullInstanceIDTest()
        throws 
            NullPointerException, 
            FailedFaultHandlingExecption, 
            RuleNotApplicableException, 
            InstanceUnknownException 
    {
        this.testApp.fault(null, this.testReq);
    }

    //fault throws an IllegalArgumentException when the passed instanceID is empty
    @Test(expected = IllegalArgumentException.class)
    public void faultMethodEmptyInstanceIDTest()
        throws 
            NullPointerException, 
            FailedFaultHandlingExecption, 
            RuleNotApplicableException, 
            InstanceUnknownException 
    {
        this.testApp.fault("", this.testReq);
    }


    //fault throws a NullPointerException when the passed req is null
    @Test(expected = NullPointerException.class)
    public void faultMethodNullReqTest()
        throws 
            NullPointerException, 
            FailedFaultHandlingExecption, 
            RuleNotApplicableException, 
            InstanceUnknownException 
    {
        this.testApp.fault(this.instanceOfA.getID(), null);
    }

    //fault throws a RuleNotAplicableException when the passed <instance, req> creates no fault
    @Test (expected = RuleNotApplicableException.class)
    public void faultMethodNotFaultedInstanceTest()
        throws 
        NullPointerException, 
        FailedFaultHandlingExecption, 
        RuleNotApplicableException, 
        InstanceUnknownException 
    {
        this.testApp.fault(this.instanceOfB.getID(), this.testReq);
    }

    //fault throws a RuleNotAplicableException when the passed instanceID is not assciated with an instance
    @Test (expected = RuleNotApplicableException.class)
    public void faultMethodInstanceUnknownTest()
        throws 
        NullPointerException, 
        FailedFaultHandlingExecption, 
        RuleNotApplicableException, 
        InstanceUnknownException 
    {
        this.testApp.fault("unkownInstanceID", this.testReq);
    }


    //fault throws a FailedFaultHanldingException if there is not found a fault handlig state to go
    @Test(expected = FailedFaultHandlingExecption.class)
    public void faultMethodNotFaultHandlingStateTest()
        throws 
            NullPointerException, 
            FailedFaultHandlingExecption, 
            RuleNotApplicableException, 
            InstanceUnknownException 
    {
        this.testApp.fault(this.instanceOfC.getID(), this.testReq);
    }

    @Test
    public void faultMethodTest()
        throws 
            NullPointerException, 
            FailedFaultHandlingExecption, 
            RuleNotApplicableException, 
            InstanceUnknownException 
    {
        this.testApp.fault(this.instanceOfA.getID(), this.testReq);
        assertTrue(this.instanceOfA.getCurrentState().equals("faultHandlingState"));
    }
       
}