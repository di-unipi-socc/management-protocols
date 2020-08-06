package test.UnitTest.ApplicationTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import model.*;
import model.exceptions.NodeUnknownException;
import model.exceptions.RuleNotApplicableException;

//test both randomPI and getCapableInstances

public class RandomPITest {

    public Application testApp;
    public Node nodeA;
    public Node nodeB;

    public NodeInstance instanceA;
    public NodeInstance instanceB1;
    public NodeInstance instanceB2;
    public NodeInstance instanceB3;

    public Requirement req;

    /**
        builds a simple custom application with 2 nodes, nodeA and nodeB
        nodeA requires req and nodeB offer the right cap.
        there are 4 instances, 1 instance of A and 3 instance of B.
        instanceA is created before the instances of B so it can have a resolvable fault.
    */
    @Before
    public void setUp() 
        throws 
            NullPointerException, 
            RuleNotApplicableException, 
            NodeUnknownException 
    {
        this.req = new Requirement("req", RequirementSort.REPLICA_UNAWARE);

        this.nodeA = this.createNodeA();
        this.nodeB = this.createNodeB();

        this.testApp = new Application("testApp");

        this.testApp.addNode(this.nodeA);
        this.testApp.addNode(this.nodeB);

        StaticBinding firstHalf = new StaticBinding("nodeA", "req");
        StaticBinding secondHalf = new StaticBinding("nodeB", "cap");

        this.testApp.addStaticBinding(firstHalf, secondHalf);

        this.instanceA = this.testApp.scaleOut1(this.nodeA);
        this.instanceB1 = this.testApp.scaleOut1(this.nodeB);
        this.instanceB2 = this.testApp.scaleOut1(this.nodeB);
        this.instanceB3 = this.testApp.scaleOut1(this.nodeB);
    }

    public Node createNodeA(){
        Node ret = new Node("nodeA", "state1", new ManagementProtocol());
        ManagementProtocol mp = ret.getManagementProtocol();

        ret.addState("state1");
        ret.addRequirement(this.req);

        List<Requirement> reqs = new ArrayList<>();
        reqs.add(this.req);
        mp.addRhoEntry("state1", reqs);

        mp.addGammaEntry("state1", new ArrayList<String>());
        mp.addPhiEntry("state1", new ArrayList<String>());
    
        return ret;
    }

    public Node createNodeB(){
        Node ret = new Node("nodeB", "state1", new ManagementProtocol());
        ManagementProtocol mp = ret.getManagementProtocol();

        ret.addState("state1");
        ret.addCapability("cap");

        List<String> caps = new ArrayList<>();
        caps.add("cap");

        mp.addGammaEntry("state1", caps);
        mp.addPhiEntry("state1", new ArrayList<String>());
        mp.addRhoEntry("state1", new ArrayList<>());

        return ret;
    }

    @Test(expected = NullPointerException.class)
    public void randomPINullAskingInstanceTest(){
        this.testApp.randomPI(null, this.req);
    }

    @Test(expected = NullPointerException.class)
    public void randomPINullReqTest(){
        this.testApp.randomPI(this.instanceA, null);
    }

    @Test
    public void randomPITest(){

        ArrayList<NodeInstance> capableInstances = (ArrayList<NodeInstance>) this.testApp.getGlobalState().getCapableInstances(this.instanceA, this.req);
        
        //the 3 instances of nodeB are capable
        assertTrue(capableInstances.size() == 3);
        //instanceA is not a capable instance for itself (obv)
        assertFalse(capableInstances.contains(this.instanceA));

        //really a trivial test, TODO implement a real test
        for(int i = 0; i < 50; i++)
            assertTrue(capableInstances.contains(this.testApp.randomPI(this.instanceA, this.req)));
        
    }

}