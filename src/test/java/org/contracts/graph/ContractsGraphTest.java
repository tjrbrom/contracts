package org.contracts.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.contracts.model.ContractMember;
import org.contracts.model.IContractMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
public final class ContractsGraphTest {

    private final ContractsGraph familyGraph = new ContractsGraph();
    private final Map<String, ContractMember> map = new HashMap<>();

    @BeforeEach
    public void beforeEach() {
        map.put("1", new ContractMember("1", ""));
        map.put("2", new ContractMember("2", "4"));
        map.put("3", new ContractMember("3", "2"));
        map.put("4", new ContractMember("4", "1"));
        map.put("5", new ContractMember("5", "3"));
        map.put("6", new ContractMember("6", "4"));
        map.put("7", new ContractMember("7", "4"));
        map.put("8", new ContractMember("8", "4"));
        map.put("9", new ContractMember("9", "6"));
        map.put("10", new ContractMember("10", "6"));
        map.put("11", new ContractMember("11", "6"));
        map.put("12", new ContractMember("12", "9"));
        map.put("13", new ContractMember("13", "9"));
        map.put("14", new ContractMember("14", "9"));
    }

    @AfterEach
    public void afterEach() {
        familyGraph.clear();
        map.clear();
    }

    @Test
    public void countParentDepth() {
        familyGraph.init(map.values());
        assertEquals(0, familyGraph.countParentDepth("1"));
        assertEquals(2, familyGraph.countParentDepth("2"));
        assertEquals(3, familyGraph.countParentDepth("3"));
        assertEquals(1, familyGraph.countParentDepth("4"));
        assertEquals(4, familyGraph.countParentDepth("5"));
        assertEquals(4, familyGraph.countParentDepth("14"));
    }

    @Test
    public void countChildDepth() {
        familyGraph.init(map.values());
        assertEquals(4, familyGraph.countChildDepth("1"));
        assertEquals(2, familyGraph.countChildDepth("2"));
        assertEquals(1, familyGraph.countChildDepth("3"));
        assertEquals(3, familyGraph.countChildDepth("4"));
        assertEquals(0, familyGraph.countChildDepth("5"));
        assertEquals(0, familyGraph.countChildDepth("14"));
    }

    @Test
    public void init_add_join_rejoin() {
        ContractMember newpc =
            new ContractMember("15", "1");

        log.info("=================================");
        log.info("init families and add a family");
        familyGraph.init(map.values());
        assertEquals(14, familyGraph.getGraph().traversal().V().count().next());
        assertTrue(familyGraph.contractExists("1"));
        assertTrue(familyGraph.parentExists( "1"));
        assertFalse(familyGraph.parentEdgeExists(new FamilyMember("15", "1")));
        assertFalse(familyGraph.childEdgeExists(new FamilyMember("15", "1")));

        familyGraph.addContract(newpc);
        familyGraph.printGraph();
        assertEquals(15, familyGraph.getGraph().traversal().V().count().next());
        assertTrue(familyGraph.contractExists("15"));
        assertTrue(familyGraph.parentExists( "1"));
        assertFalse(familyGraph.parentEdgeExists(new FamilyMember("15", "1")));
        assertFalse(familyGraph.childEdgeExists(new FamilyMember("15", "1")));

        log.info("=================================");

        log.info("join another family");
        newpc.setParentId("2");
        familyGraph.joinContract(newpc);
        familyGraph.printGraph();
        assertEquals(15, familyGraph.getGraph().traversal().V().count().next());
        assertTrue(familyGraph.contractExists("15"));
        assertTrue(familyGraph.parentExists( "1"));
        assertTrue(familyGraph.parentExists( "2"));
        assertFalse(familyGraph.parentEdgeExists(new FamilyMember("15", "1")));
        assertFalse(familyGraph.childEdgeExists(new FamilyMember("15", "1")));
        assertTrue(familyGraph.parentEdgeExists(new FamilyMember("15", "2")));
        assertTrue(familyGraph.childEdgeExists(new FamilyMember("15", "2")));

        log.info("=================================");

        log.info("join the previous family");
        newpc.setParentId("1");
        familyGraph.joinContract(newpc);
        familyGraph.printGraph();
        assertEquals(15, familyGraph.getGraph().traversal().V().count().next());
        assertTrue(familyGraph.contractExists("15"));
        assertTrue(familyGraph.parentExists( "1"));
        assertTrue(familyGraph.parentExists( "2"));
        assertTrue(familyGraph.parentEdgeExists(new FamilyMember("15", "1")));
        assertTrue(familyGraph.childEdgeExists(new FamilyMember("15", "1")));
        assertFalse(familyGraph.parentEdgeExists(new FamilyMember("15", "2")));
        assertFalse(familyGraph.childEdgeExists(new FamilyMember("15", "2")));

        log.info("=================================");

        log.info("join the same family");
        familyGraph.joinContract(newpc);
        familyGraph.printGraph();
        assertEquals(15, familyGraph.getGraph().traversal().V().count().next());
        assertTrue(familyGraph.contractExists("15"));
        assertTrue(familyGraph.parentExists( "1"));
        assertTrue(familyGraph.parentExists( "2"));
        assertTrue(familyGraph.parentEdgeExists(new FamilyMember("15", "1")));
        assertTrue(familyGraph.childEdgeExists(new FamilyMember("15", "1")));
        assertFalse(familyGraph.parentEdgeExists(new FamilyMember("15", "2")));
        assertFalse(familyGraph.childEdgeExists(new FamilyMember("15", "2")));
    }

    @AllArgsConstructor
    @Data
    private static class FamilyMember implements IContractMember {
        private String contractId;
        private String parentId;

        @Override
        public String contractId() {
            return contractId;
        }

        @Override
        public String parentId() {
            return parentId;
        }
    }
}
