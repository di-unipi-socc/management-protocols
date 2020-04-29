package test;

import model.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MyTest {
    
    @Test
    public void createFrontendTest(){
        Node frontend = ApplicationFactory.createFrontend();

        assertNotNull("frontend null", frontend);
        assertTrue("frontend wrong name", frontend.getName().equals("frontend"));

        assertNotNull("frontend reqs null", frontend.getReqs());
        assertNotNull("frontend caps null", frontend.getCaps());
        assertNotNull("frontend states null", frontend.getStates());
        assertNotNull("frontend ops null", frontend.getOps());

        assertNotNull("frontend management protocol null", frontend.getMp());
        assertNotNull("rho null", frontend.getMp().getRho());
        assertNotNull("phi null", frontend.getMp().getPhi());
        assertNotNull("gamma null", frontend.getMp().getGamma());

        //frontend do not offer caps
        assertTrue("caps not empty", frontend.getCaps().isEmpty());

        //frontend has two reqs
        assertTrue("not right number of reqs", frontend.getReqs().size() == 2);
        Requirement host = new Requirement("host", RequirementSort.CONTAINMENT);
        Requirement conn = new Requirement("conn", RequirementSort.REPLICA_UNAWARE);
        assertTrue("frontend missing requirement host", frontend.getReqs().contains(host));
        assertTrue("frontend missing requirement conn", frontend.getReqs().contains(conn));

        //frontend has 5 ops
        assertTrue("frontend should have 5 ops", frontend.getOps().size() == 5);
        assertTrue("frontend missing op install", frontend.getOps().contains("install"));
        assertTrue("frontend missing op start", frontend.getOps().contains("start"));
        assertTrue("frontend missing op config", frontend.getOps().contains("config"));
        assertTrue("frontend missing op stop", frontend.getOps().contains("stop"));
        assertTrue("frontend missing op uninstall", frontend.getOps().contains("uninstall"));

        //fronend has 5 states
        assertTrue("frontend has 5 states", frontend.getStates().size() == 5);
        assertTrue("frontend missing state not-installed", frontend.getStates().contains("not-installed"));
        assertTrue("frontend missing state installed", frontend.getStates().contains("installed"));
        assertTrue("frontend missing state configured", frontend.getStates().contains("configured"));
        assertTrue("frontend missing state working", frontend.getStates().contains("working"));
        assertTrue("frontend missing state damaged", frontend.getStates().contains("damaged"));

        //the initial state is "not-installed"
        assertTrue("frontend wrong initial state", frontend.getInitialState().equals("not-installed"));

        //frontend has 7 possible transitions
        assertTrue("frontend has 7 transitions", frontend.getMp().getTransition().size() == 7);
        assertNotNull("frontend missing transition <not-installed, install, installed>", frontend.getMp().getTransition().get("not-installedinstallinstalled"));
        assertNotNull("frontend missing transition <installed, uninstall, not-installed>", frontend.getMp().getTransition().get("installeduninstallnot-installed"));
        assertNotNull("frontend missing transition <installed, config, configured>", frontend.getMp().getTransition().get("installedconfigconfigured"));
        assertNotNull("frontend missing transition <configured, config, configured>", frontend.getMp().getTransition().get("configuredconfigconfigured"));
        assertNotNull("frontend missing transition <configured, start, working>", frontend.getMp().getTransition().get("configuredstartworking"));
        assertNotNull("frontend missing transition <working, stop, configured>", frontend.getMp().getTransition().get("workingstopconfigured"));
        assertNotNull("frontend missing transition <configured, uninstall, not-installed>", frontend.getMp().getTransition().get("configureduninstallnot-installed"));

        //rho: state/transition -> reqs
        assertTrue("frontend state not-install requires nothing", frontend.getMp().getRho().get("not-installed").isEmpty());

        assertTrue("frontend op install requires host", frontend.getMp().getRho().get("not-installedinstallinstalled").contains(host));

        assertTrue("frontend op uninstall requires host", frontend.getMp().getRho().get("installeduninstallnot-installed").contains(host));
        assertTrue("frontend op uninstall requires host", frontend.getMp().getRho().get("configureduninstallnot-installed").contains(host));

        assertTrue("frontend state installed requires nothing", frontend.getMp().getRho().get("installed").isEmpty());

        assertTrue("frontend op config requires host", frontend.getMp().getRho().get("installedconfigconfigured").contains(host));
        assertTrue("frontend op config requires conn", frontend.getMp().getRho().get("installedconfigconfigured").contains(conn));

        assertTrue("frontend op config requires host", frontend.getMp().getRho().get("configuredconfigconfigured").contains(host));
        assertTrue("frontend op config requires host", frontend.getMp().getRho().get("configuredconfigconfigured").contains(host));

        assertTrue("frontend state configured requires nothing", frontend.getMp().getRho().get("configured").isEmpty());

        assertTrue("frontend op start requires host", frontend.getMp().getRho().get("configuredstartworking").contains(host));
        assertTrue("frontend op start requires conn", frontend.getMp().getRho().get("configuredstartworking").contains(conn));

        assertTrue("frontend state working requires host", frontend.getMp().getRho().get("working").contains(host));
        assertTrue("frontend state working requires conn", frontend.getMp().getRho().get("working").contains(conn));

        assertTrue("frontend op stop requires host", frontend.getMp().getRho().get("workingstopconfigured").contains(host));

        //gamma: state -> caps
        for(String state : frontend.getStates())
            assertTrue("fronend wrong gamma entry " + state + " -> ", frontend.getMp().getGamma().get(state).isEmpty());
        
        //phi: state -> states for fault recovery
        assertTrue("frontend wrong phi entry <not-installed, install, installed> -> damaged", frontend.getMp().getPhi().get("not-installedinstallinstalled").contains("damaged"));
        assertTrue("frontend wrong phi entry <installed, uninstall, not-installed> -> damaged", frontend.getMp().getPhi().get("installeduninstallnot-installed").contains("damaged"));
        assertTrue("frontend wrong phi entry <configured, uninstall, not-installed> -> damaged", frontend.getMp().getPhi().get("configureduninstallnot-installed").contains("damaged"));
        assertTrue("frontend wrong phi entry <installed, config, configured> -> installed", frontend.getMp().getPhi().get("installedconfigconfigured").contains("installed"));
        assertTrue("frontend wrong phi entry <configured, config, configured> -> installed", frontend.getMp().getPhi().get("configuredconfigconfigured").contains("installed"));
        assertTrue("frontend wrong phi entry <configured, start, working> -> configured", frontend.getMp().getPhi().get("configuredstartworking").contains("configured"));
        assertTrue("frontend wrong phi entry <working, stop, configured> -> configured", frontend.getMp().getPhi().get("workingstopconfigured").contains("configured"));
        assertTrue("frontend wrong phi entry state working -> configured", frontend.getMp().getPhi().get("working").contains("configured"));
    }
    @Test
    public void createBackendTest(){
        Node backend = ApplicationFactory.createBackend();
        assertNotNull("backend null", backend);
        assertTrue("backend wrong name", backend.getName().equals("backend"));

        assertNotNull("backend caps null", backend.getCaps());
        assertNotNull("backend reqs null", backend.getReqs());
        assertNotNull("backend states null", backend.getStates());
        assertNotNull("backend ops null", backend.getOps());

        assertNotNull("backend mp null", backend.getMp());
        assertNotNull("backend rho null", backend.getMp().getRho());
        assertNotNull("backend phi null", backend.getMp().getPhi());
        assertNotNull("backend gamma null", backend.getMp().getGamma());

        //backend offers conn and just conn
        assertTrue("backend wrong number of caps", backend.getCaps().size() == 1);
        assertNotNull("backend missing cap conn", backend.getCaps().contains("conn"));

        //backend has 2 reqs, host and db
        Requirement host = new Requirement("host", RequirementSort.CONTAINMENT);
        Requirement db = new Requirement("db", RequirementSort.REPLICA_AWARE);
        assertTrue("backend has 2 reqs", backend.getReqs().size() == 2);
        assertTrue("backend missing req host", backend.getReqs().contains(host));
        assertTrue("backend missing req db", backend.getReqs().contains(db));

        //backend has 5 ops
        assertTrue("backend has 5 ops", backend.getOps().size() == 5);
        assertTrue("backend missing op install", backend.getOps().contains("install"));
        assertTrue("backend missing op uninstall", backend.getOps().contains("uninstall"));
        assertTrue("backend missing op start", backend.getOps().contains("start"));
        assertTrue("backend missing op stop", backend.getOps().contains("stop"));
        assertTrue("backend missing op config", backend.getOps().contains("config"));

        //backend has 4 states
        assertTrue("backend has 4 states", backend.getStates().size() == 4);
        assertTrue("backend missing state state unavailbable", backend.getStates().contains("unavailable"));
        assertTrue("backend missing state availbable", backend.getStates().contains("available"));
        assertTrue("backend missing state running", backend.getStates().contains("running"));
        assertTrue("backend missing state state damaged", backend.getStates().contains("damaged"));

        //the initial state is unavailable
        assertTrue("backend wrong initial state", backend.getInitialState().equals("unavailable"));

        //backend has 5 transitions
        assertTrue("backend has 5 transitions", backend.getMp().getTransition().size() == 5);
        assertNotNull("backend missing transsition <unavailable, install, available>", backend.getMp().getTransition().get("unavailableinstallavailable"));
        assertNotNull("backend missing transsition <available, uninstall, unavailable>", backend.getMp().getTransition().get("availableuninstallunavailable"));
        assertNotNull("backend missing transsition <available, start, running>", backend.getMp().getTransition().get("availablestartrunning"));
        assertNotNull("backend missing transsition <running, stop, available>", backend.getMp().getTransition().get("runningstopavailable"));
        assertNotNull("backend missing transsition <running, config, running>", backend.getMp().getTransition().get("runningconfigrunning"));

        //rho: state/transition -> reqs
        assertTrue("backend state unavailable requires nothing", backend.getMp().getRho().get("unavailable").size() == 0);
        assertTrue("backennd state available requires nothing", backend.getMp().getRho().get("available").size() == 0);
        assertTrue("backend state damaged requires nothing", backend.getMp().getRho().get("damaged").size() == 0);

        assertTrue("backend state running requires host", backend.getMp().getRho().get("running").contains(host));
        assertTrue("backend state running requires db", backend.getMp().getRho().get("running").contains(db));

        assertTrue("backend op install requires host", backend.getMp().getRho().get("unavailableinstallavailable").contains(host));
        assertTrue("backend op uninstall requires host", backend.getMp().getRho().get("availableuninstallunavailable").contains(host));

        assertTrue("backend op start requires host", backend.getMp().getRho().get("availablestartrunning").contains(host));
        assertTrue("backend op start requires db", backend.getMp().getRho().get("availablestartrunning").contains(db));

        assertTrue("backend op stop requires host", backend.getMp().getRho().get("runningstopavailable").contains(host));

        assertTrue("backend op config requires host", backend.getMp().getRho().get("runningconfigrunning").contains(host));
        assertTrue("backend op config requries db", backend.getMp().getRho().get("runningconfigrunning").contains(db));

        //gamma: state -> caps
        for(String state : backend.getStates()){
            if(state.equals("running") == false)
                assertTrue("backend wrong gamma entry " + state + "-> ", backend.getMp().getGamma().get(state).size() == 0);
            else
                assertTrue("backend wrong gamma entry running -> conn>", backend.getMp().getGamma().get("running").contains("conn"));
        }

        //phi: state -> states for fault recovery
        assertTrue("backend phi has 6 entries", backend.getMp().getPhi().size() == 6);
        assertTrue("backend wrong phi entry <unavailable, install, available> -> damaged", backend.getMp().getPhi().get("unavailableinstallavailable").contains("damaged"));
        assertTrue("backend wrong phi entry <available, uninstall, unavailable> -> damaged", backend.getMp().getPhi().get("availableuninstallunavailable").contains("damaged"));
        assertTrue("backend wrong phi entry <available, start, running> -> available", backend.getMp().getPhi().get("availablestartrunning").contains("available"));
        assertTrue("backend wrong phi entry <running, config, running> -> damaged", backend.getMp().getPhi().get("runningconfigrunning").contains("damaged"));
        assertTrue("backend wrong phi entry <running, stop, available> -> available", backend.getMp().getPhi().get("runningstopavailable").contains("available"));
        assertTrue("backend wrong phi entry running -> available", backend.getMp().getPhi().get("running").contains("available"));
    }

    @Test
    public void createNodeTest(){
        Node node = ApplicationFactory.createNode();
        assertNotNull("node null", node);
        assertTrue("node wrong name", node.getName().equals("node"));

        assertNotNull("node caps null", node.getCaps());
        assertNotNull("node reqs null", node.getReqs());
        assertNotNull("node states null", node.getStates());
        assertNotNull("node ops null", node.getOps());

        assertNotNull("node mp null", node.getMp());
        assertNotNull("node rho null", node.getMp().getRho());
        assertNotNull("node phi null", node.getMp().getPhi());
        assertNotNull("node gamma null", node.getMp().getGamma());
        assertNotNull("node transitions null", node.getMp().getTransition());

        //node offer host and just host
        assertTrue("node has 1 cap", node.getCaps().size() == 1);
        assertTrue("node missing cap host", node.getCaps().contains("host"));

        //node has no reqs
        assertTrue("node too many reqs", node.getReqs().size() == 0);

        //node has 2 ops
        assertTrue("node has 2 ops", node.getOps().size() == 2);
        assertTrue("node missing op start", node.getOps().contains("start"));
        assertTrue("node missing op stop", node.getOps().contains("stop"));

        //node has 2 states
        assertTrue("node has 2 states", node.getStates().size() == 2);
        assertTrue("node missing state stopped", node.getStates().contains("stopped"));
        assertTrue("node missing state running", node.getStates().contains("running"));

        //the initial state is stopped
        assertTrue("node wrong initial state", node.getInitialState().equals("stopped"));

        //node has 2 transition
        assertTrue("node has 2 transitions", node.getMp().getTransition().size() == 2);
        assertNotNull("node missing transition <stopped, start, running>", node.getMp().getTransition().get("stoppedstartrunning"));
        assertNotNull("node missing transition <running, stop, stopped>", node.getMp().getTransition().get("runningstopstopped"));

        //rho: state/transition -> requirement
        for(String state : node.getStates())
            assertTrue("node state " + state + " requires nothing", node.getMp().getRho().get(state).size() == 0);

        //gamma: state/transition -> caps
        assertTrue("node wrong gamma entry stopped -> ", node.getMp().getGamma().get("stopped").size() == 0);
        assertTrue("node wrong gamma entry running -> host", node.getMp().getGamma().get("running").contains("host"));

        //phi: state -> states for fault recovery
        for(String state : node.getStates()){
            assertTrue("node wrong phi entry "+ state + " ->", node.getMp().getPhi().get(state).size() == 0);
        }
    }

    public void creadeMongoTest(){
        Node mongo = ApplicationFactory.createMongo();
        assertNotNull("mongo null", mongo);
        assertTrue("mongo wrong name", mongo.getName().equals("mongo"));

        assertNotNull("mongo caps null", mongo.getCaps());
        assertNotNull("mongo reqs null", mongo.getReqs());
        assertNotNull("mongo states null", mongo.getStates());
        assertNotNull("mongo ops null", mongo.getOps());

        assertNotNull("mongo mp null", mongo.getMp());
        assertNotNull("mongo rho null", mongo.getMp().getRho());
        assertNotNull("mongo phi null", mongo.getMp().getPhi());
        assertNotNull("mongo gamma null", mongo.getMp().getGamma());
        assertNotNull("mongo transitions null", mongo.getMp().getTransition());

        //mongo offer db and just db
        assertTrue("mongo has 1 cap", mongo.getCaps().size() == 1);
        assertTrue("mongo missing cap db", mongo.getCaps().contains("db"));

        //mongo has no reqs
        assertTrue("mongo too many reqs", mongo.getReqs().size() == 0);

        //mongo has 2 ops
        assertTrue("node has 2 ops", mongo.getOps().size() == 2);
        assertTrue("mongo missing op start", mongo.getOps().contains("start"));
        assertTrue("mongo missing op stop", mongo.getOps().contains("stop"));

        //mongo has 2 states
        assertTrue("mongo has 2 states", mongo.getStates().size() == 2);
        assertTrue("mongo missing state stopped", mongo.getStates().contains("stopped"));
        assertTrue("mongo missing state running", mongo.getStates().contains("running"));

        //the initial state is stopped
        assertTrue("mongo wrong initial state", mongo.getInitialState().equals("stopped"));

        //mongo has 2 transition
        assertTrue("mongo has 2 transitions", mongo.getMp().getTransition().size() == 2);
        assertNotNull("mongo missing transition <stopped, start, running>", mongo.getMp().getTransition().get("stoppedstartrunning"));
        assertNotNull("mongo missing transition <running, stop, stopped>", mongo.getMp().getTransition().get("runningstopstopped"));

        //rho: state/transition -> requirement
        for(String state : mongo.getStates())
            assertTrue("mongo state " + state + " requires nothing", mongo.getMp().getRho().get(state).size() == 0);

        //gamma: state/transition -> caps
        assertTrue("mongo wrong gamma entry stopped -> ", mongo.getMp().getGamma().get("stopped").size() == 0);
        assertTrue("mongo wrong gamma entry running -> host", mongo.getMp().getGamma().get("running").contains("host"));

        //phi: state -> states for fault recovery
        for(String state : mongo.getStates()){
            assertTrue("mongo wrong phi entry "+ state + " ->", mongo.getMp().getPhi().get(state).size() == 0);
        }
    }

   @Test
   public void scaleOut1Test(){
       
   }
    
}                               