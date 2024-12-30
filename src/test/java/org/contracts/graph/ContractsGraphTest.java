package org.contracts.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.contracts.graph.ContractsGraph;
import org.contracts.model.ContractMember;
import org.contracts.model.IContractMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public final class ContractsGraphTest {

  private final ContractsGraph graph = new ContractsGraph();
  private final Set<ContractMember> set = new HashSet<>();

  @BeforeEach
  public void beforeEach() {
    set.add(
        ContractMember.builder()
            .contractId("1000")
            .parentId("")
            .createdAt(LocalDateTime.now())
            .build());
    set.add(
        ContractMember.builder()
            .contractId("2000")
            .parentId("4")
            .createdAt(LocalDateTime.now().plusMinutes(1))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("3000")
            .parentId("2")
            .createdAt(LocalDateTime.now().plusMinutes(3))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("4000")
            .parentId("1")
            .createdAt(LocalDateTime.now().plusMinutes(2))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("5000")
            .parentId("3")
            .createdAt(LocalDateTime.now().plusMinutes(4))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("6000")
            .parentId("5")
            .createdAt(LocalDateTime.now().plusMinutes(5))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("7000")
            .parentId("14")
            .createdAt(LocalDateTime.now().plusMinutes(6))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("8000")
            .parentId("14")
            .createdAt(LocalDateTime.now().plusMinutes(7))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("9000")
            .parentId("6")
            .createdAt(LocalDateTime.now().plusMinutes(8))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("14000")
            .parentId("")
            .createdAt(LocalDateTime.now().plusMinutes(7))
            .build());
    set.add(
        ContractMember.builder()
            .contractId("15000")
            .parentId("4")
            .createdAt(LocalDateTime.now().plusMinutes(7))
            .build());
    graph.init(set);
  }

  @AfterEach
  public void afterEach() {
    graph.clear();
    set.clear();
  }

  @Test
  public void deleteContract() {
    assertTrue(graph.contractExists("1"));

    graph.deleteContract("1");

    assertFalse(graph.contractExists("1"));
  }

  @Test
  public void calculateChildToParentDepth() {
    assertEquals(5, graph.calculateChildToParentDepth("6", "1"));
    assertEquals(-1, graph.calculateChildToParentDepth("1", "6"));
    assertEquals(4, graph.calculateChildToParentDepth("6", "4"));
    assertEquals(-1, graph.calculateChildToParentDepth("4", "6"));
  }

  @Test
  public void leave() {
    assertEquals("4", graph.findImmediateParent("2").get().contractId());

    graph.leave("2000");

    assertTrue(graph.findImmediateParent("2").isEmpty());
  }

  @Test
  public void kick() {
    assertEquals("4", graph.findImmediateParent("2").get().contractId());

    graph.kick("4000", "2000");

    assertTrue(graph.findImmediateParent("2").isEmpty());
  }

  @Test
  public void join() {
    graph.joinContract("8000", "4");
    assertTrue(
        graph.findChildren("4").stream()
            .map(IContractMember::contractId)
            .anyMatch(id -> id.equals("8")));
  }

  @Test
  public void join_isImmediateOrDistantChild_caseInsideDepthLImit() {
    assertFalse(graph.isImmediateOrDistantChild("2", "6"));
    assertFalse(graph.joinContract("2000", "6"));
    assertFalse(graph.isImmediateOrDistantChild("2", "6"));
  }

  @Test
  public void join_isImmediateOrDistantChild_caseOutsideDepthLImit() {
    assertFalse(graph.isImmediateOrDistantChild("4", "9"));
    assertTrue(graph.joinContract("4000", "9"));
    assertTrue(graph.isImmediateOrDistantChild("4", "9"));
    assertFalse(graph.joinContract("9000", "4"));
  }

  @Test
  public void isImmediateOrDistantChildIgnoringDepth() {
    assertFalse(graph.isImmediateOrDistantChildIgnoringDepth("4", "9"));
    assertTrue(graph.isImmediateOrDistantChildIgnoringDepth("9", "4"));
    assertTrue(graph.isImmediateOrDistantChildIgnoringDepth("9", "1"));
    assertFalse(graph.isImmediateOrDistantChildIgnoringDepth("1", "9"));
  }

  @Test
  public void findContractByContractId() {
    IContractMember contract = graph.findContractByContractId("1000").get();
    assertEquals("1", contract.contractId());
  }

  @Test
  public void findImmediateParent() {
    assertEquals("2", graph.findImmediateParent("3").get().contractId());
    assertEquals("1", graph.findImmediateParent("4").get().contractId());
    assertEquals("4", graph.findImmediateParent("2").get().contractId());
    assertEquals("14", graph.findImmediateParent("7").get().contractId());
    assertTrue(graph.findImmediateParent("1").isEmpty());
  }

  @Test
  public void findMostDistantParent() {
    assertEquals("1", graph.findMostDistantParent("3").get().contractId());
    assertEquals("1", graph.findMostDistantParent("4").get().contractId());
    assertEquals("1", graph.findMostDistantParent("2").get().contractId());
    assertEquals("14", graph.findMostDistantParent("7").get().contractId());
    assertTrue(graph.findImmediateParent("1").isEmpty());
  }

  @Test
  public void findChildren() {
    assertEquals(3, graph.findChildren("3").size());
  }

  @Test
  public void findImmediateChildren() {
    assertEquals(1, graph.findImmediateChildren("3").size());
  }

  @Test
  public void getContractInfo() {
    assertEquals("4", graph.findContractByContractId("4000").get().contractId());
  }

  @Test
  public void contractExists() {
    assertTrue(graph.contractExists("14"));
    assertFalse(graph.contractExists("16"));
  }
}
