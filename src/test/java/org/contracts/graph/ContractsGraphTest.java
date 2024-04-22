package org.contracts.graph;

import lombok.extern.slf4j.Slf4j;
import org.contracts.model.ContractMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public final class ContractsGraphTest {

  private final ContractsGraph graph = new ContractsGraph();
  private final Set<ContractMember> set = new HashSet<>();

  @BeforeEach
  public void beforeEach() {
    set.add(ContractMember.builder()
      .contractId("1").parentId("").createdAt(LocalDateTime.now())
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("2").parentId("4").createdAt(LocalDateTime.now().plusMinutes(1))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("3").parentId("2").createdAt(LocalDateTime.now().plusMinutes(3))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("4").parentId("1").createdAt(LocalDateTime.now().plusMinutes(2))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("5").parentId("3").createdAt(LocalDateTime.now().plusMinutes(4))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("6").parentId("5").createdAt(LocalDateTime.now().plusMinutes(5))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("7").parentId("14").createdAt(LocalDateTime.now().plusMinutes(6))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("8").parentId("14").createdAt(LocalDateTime.now().plusMinutes(7))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("9").parentId("6").createdAt(LocalDateTime.now().plusMinutes(8))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("10").parentId("6").createdAt(LocalDateTime.now().plusMinutes(9))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("11").parentId("6").createdAt(LocalDateTime.now().plusMinutes(10))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("12").parentId("9").createdAt(LocalDateTime.now().plusMinutes(11))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("13").parentId("9").createdAt(LocalDateTime.now().plusMinutes(12))
      .build()
    );
    set.add(ContractMember.builder()
      .contractId("14").parentId("9").createdAt(LocalDateTime.now().plusMinutes(13))
      .build()
    );
  }

  @AfterEach
  public void afterEach() {
    graph.clear();
    set.clear();
  }

  @Test
  public void countMaxParentDepth() {
    graph.init(set);
    assertEquals(0, graph.countMaxParentDepth("1"));
    assertEquals(2, graph.countMaxParentDepth("2"));
    assertEquals(3, graph.countMaxParentDepth("3"));
    assertEquals(1, graph.countMaxParentDepth("4"));
    assertEquals(4, graph.countMaxParentDepth("5"));
    assertEquals(5, graph.countMaxParentDepth("6"));
    assertEquals(7, graph.countMaxParentDepth("14"));
  }

  @Test
  public void countMaxChildDepth() {
    graph.init(set);
    assertEquals(8, graph.countMaxChildDepth("1"));
    assertEquals(6, graph.countMaxChildDepth("2"));
    assertEquals(5, graph.countMaxChildDepth("3"));
    assertEquals(7, graph.countMaxChildDepth("4"));
    assertEquals(4, graph.countMaxChildDepth("5"));
    assertEquals(3, graph.countMaxChildDepth("6"));
    assertEquals(1, graph.countMaxChildDepth("14"));
  }

  @Test
  public void create_join_get_print() {
    graph.init(set);
    ContractMember cm = ContractMember.builder()
      .contractId("15").parentId("1").createdAt(LocalDateTime.now())
      .build();
    graph.printGraph();
    graph.createContract(cm);
    graph.printGraph();
    graph.joinContract(cm);
    graph.printGraph();
    cm.setParentId("7");
    graph.joinContract(cm);
    graph.printGraph();
    assertNotNull(graph.getDataByContractId("15"));
  }
}
