package org.contracts.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.contracts.model.IContractMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
public final class ContractsGraphTest {

  private final ContractsGraph contractsGraph = new ContractsGraph();
  private final Set<ContractMember> set = new HashSet<>();

  @BeforeEach
  public void beforeEach() {
    set.add(new ContractMember("1", ""));
    set.add(new ContractMember("2", "4"));
    set.add(new ContractMember("3", "2"));
    set.add(new ContractMember("4", "1"));
    set.add(new ContractMember("5", "3"));
    set.add(new ContractMember("6", "4"));
    set.add(new ContractMember("7", "4"));
    set.add(new ContractMember("8", "4"));
    set.add(new ContractMember("9", "6"));
    set.add(new ContractMember("10", "6"));
    set.add(new ContractMember("11", "6"));
    set.add(new ContractMember("12", "9"));
    set.add(new ContractMember("13", "9"));
    set.add(new ContractMember("14", "9"));
  }

  @AfterEach
  public void afterEach() {
    contractsGraph.clear();
    set.clear();
  }

  @Test
  public void countParentDepth() {
    contractsGraph.init(set);
    assertEquals(0, contractsGraph.countParentDepth("1"));
    assertEquals(2, contractsGraph.countParentDepth("2"));
    assertEquals(3, contractsGraph.countParentDepth("3"));
    assertEquals(1, contractsGraph.countParentDepth("4"));
    assertEquals(4, contractsGraph.countParentDepth("5"));
    assertEquals(4, contractsGraph.countParentDepth("14"));
  }

  @Test
  public void countChildDepth() {
    contractsGraph.init(set);
    assertEquals(4, contractsGraph.countChildDepth("1"));
    assertEquals(2, contractsGraph.countChildDepth("2"));
    assertEquals(1, contractsGraph.countChildDepth("3"));
    assertEquals(3, contractsGraph.countChildDepth("4"));
    assertEquals(0, contractsGraph.countChildDepth("5"));
    assertEquals(0, contractsGraph.countChildDepth("14"));
  }

  @Test
  public void init_add_join_rejoin() {
    log.info("=================================");
    log.info("init contracts and add a contract");
    contractsGraph.init(set);
    assertEquals(14, contractsGraph.getGraph().traversal().V().count().next());
    assertTrue(contractsGraph.contractExists("1"));
    assertTrue(contractsGraph.contractExists("1"));
    assertFalse(contractsGraph.parentEdgeExists(new ContractMember("15", "1")));
    assertFalse(contractsGraph.childEdgeExists(new ContractMember("15", "1")));

    ContractMember newcm = new ContractMember("15", "1");

    contractsGraph.createContract(newcm);
    contractsGraph.printGraph();
    assertEquals(15, contractsGraph.getGraph().traversal().V().count().next());
    assertTrue(contractsGraph.contractExists("15"));
    assertTrue(contractsGraph.contractExists("1"));
    assertFalse(contractsGraph.parentEdgeExists(new ContractMember("15", "1")));
    assertFalse(contractsGraph.childEdgeExists(new ContractMember("15", "1")));

    log.info("=================================");

    log.info("join another contract");
    newcm.setParentId("2");
    contractsGraph.joinContract(newcm);
    contractsGraph.printGraph();
    assertEquals(15, contractsGraph.getGraph().traversal().V().count().next());
    assertTrue(contractsGraph.contractExists("15"));
    assertTrue(contractsGraph.contractExists("1"));
    assertTrue(contractsGraph.contractExists("2"));
    assertFalse(contractsGraph.parentEdgeExists(new ContractMember("15", "1")));
    assertFalse(contractsGraph.childEdgeExists(new ContractMember("15", "1")));
    assertTrue(contractsGraph.parentEdgeExists(new ContractMember("15", "2")));
    assertTrue(contractsGraph.childEdgeExists(new ContractMember("15", "2")));

    log.info("=================================");

    log.info("join the previous contract");
    newcm.setParentId("1");
    contractsGraph.joinContract(newcm);
    contractsGraph.printGraph();
    assertEquals(15, contractsGraph.getGraph().traversal().V().count().next());
    assertTrue(contractsGraph.contractExists("15"));
    assertTrue(contractsGraph.contractExists("1"));
    assertTrue(contractsGraph.contractExists("2"));
    assertTrue(contractsGraph.parentEdgeExists(new ContractMember("15", "1")));
    assertTrue(contractsGraph.childEdgeExists(new ContractMember("15", "1")));
    assertFalse(contractsGraph.parentEdgeExists(new ContractMember("15", "2")));
    assertFalse(contractsGraph.childEdgeExists(new ContractMember("15", "2")));

    log.info("=================================");

    log.info("join the same contract");
    contractsGraph.joinContract(newcm);
    contractsGraph.printGraph();
    assertEquals(15, contractsGraph.getGraph().traversal().V().count().next());
    assertTrue(contractsGraph.contractExists("15"));
    assertTrue(contractsGraph.contractExists("1"));
    assertTrue(contractsGraph.contractExists("2"));
    assertTrue(contractsGraph.parentEdgeExists(new ContractMember("15", "1")));
    assertTrue(contractsGraph.childEdgeExists(new ContractMember("15", "1")));
    assertFalse(contractsGraph.parentEdgeExists(new ContractMember("15", "2")));
    assertFalse(contractsGraph.childEdgeExists(new ContractMember("15", "2")));
  }

  @AllArgsConstructor
  @Data
  private static class ContractMember implements IContractMember {
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
