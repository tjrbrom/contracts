package org.contracts.graph;

import static org.apache.tinkerpop.gremlin.process.traversal.Scope.local;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.contracts.model.IContractMember;

@Slf4j
public final class ContractsGraph {
  private final TinkerGraph graph = TinkerGraph.open();
  private final GraphTraversalSource g = graph.traversal();

  public void init(Collection<? extends IContractMember> contractMembers) {
    contractMembers.forEach(
        contractMember -> {
          g.addV("contract")
              .property("data", contractMember)
              .property("contract id", contractMember.contractId())
              .next();
        });
    contractMembers.forEach(
        contractMember -> {
          if (!Strings.isNullOrEmpty(contractMember.parentId())) {
            Vertex parent =
                g.V()
                    .hasLabel("contract")
                    .has("contract id", contractMember.parentId())
                    .tryNext()
                    .orElse(null);
            Vertex contract =
                g.V()
                    .hasLabel("contract")
                    .has("contract id", contractMember.contractId())
                    .tryNext()
                    .orElse(null);
            if (parent == null) {
              log.warn("Parent contract {} doesn't exist", contractMember.parentId());
              return;
            }
            boolean parentEdgeExists =
                g.V(parent).outE("parent").where(inV().is(contract)).hasNext();
            if (!parentEdgeExists) {
              g.V(parent).addE("parent").to(contract).next();
            }
            boolean childEdgeExists = g.V(contract).outE("child").where(inV().is(parent)).hasNext();
            if (!childEdgeExists) {
              g.V(contract).addE("child").to(parent).next();
            }
          }
        });
  }

  /**
   * Creates a new contract node that will not have a parent yet.
   *
   * @param contractMember
   */
  public void createContract(IContractMember contractMember) {
    String contractId = contractMember.contractId();
    if (Strings.isNullOrEmpty(contractId)) {
      log.error("No contract ID provided");
      return;
    }
    Vertex contract =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);
    if (contract == null) {
      g.addV("contract")
          .property("data", contractMember)
          .property("contract id", contractId)
          .next();
    } else {
      log.warn("Contract already exists");
    }
  }

  /**
   * User will join another contract. They now abandon their current parent for a new one.
   *
   * @param contractMember
   */
  public void joinContract(IContractMember contractMember) {
    if (!Strings.isNullOrEmpty(contractMember.contractId())
        && !Strings.isNullOrEmpty(contractMember.parentId())) {
      // check if these contract and parent exist
      Vertex contract =
          g.V()
              .hasLabel("contract")
              .has("contract id", contractMember.contractId())
              .tryNext()
              .orElse(null);
      Vertex parent =
          g.V()
              .hasLabel("contract")
              .has("contract id", contractMember.parentId())
              .tryNext()
              .orElse(null);

      if (contract != null && parent != null) {
        // Drop existing "parent" and "child" edges
        g.V(contract).inE("parent").drop().iterate();
        g.V(contract).outE("child").drop().iterate();

        // Add new "parent" and "child" edges
        g.V(parent).addE("parent").to(contract).next();
        g.V(contract).addE("child").to(parent).next();
      } else {
        log.error("One or both vertices do not exist.");
      }
    }
  }

  public long countParentDepth(String contractId) {
    Vertex contract =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contract != null) {
      long depth = 0;
      while (g.V(contract).out("child").hasNext()) {
        contract = g.V(contract).out("child").next();
        depth++;
      }
      return depth;
    } else {
      log.error("Contract not found");
      return -1;
    }
  }

  public long countChildDepth(String contractId) {
    Vertex contract =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contract != null) {
      List<Long> depths =
          g.V(contract)
              .repeat(out("parent").simplePath())
              .emit()
              .path()
              .map(count(local)) // Count the size of the path
              .toList();

      return depths.isEmpty() ? 0 : Collections.max(depths) - 1;
    } else {
      log.error("Contract not found");
      return -1;
    }
  }

  void printGraph() {
    this.g.V().valueMap().forEachRemaining(vertice -> log.info("Vertice: " + vertice));
    log.info("Edges:");
    this.g.E()
        .forEachRemaining(
            edge ->
                log.info(
                    "Edge: "
                        + edge.label()
                        + " | "
                        + edge.outVertex().property("contract id").value()
                        + " -> "
                        + edge.inVertex().property("contract id").value()));
  }

  boolean contractExists(String contractId) {
    return this.graph
            .traversal()
            .V()
            .hasLabel("contract")
            .has("contract id", contractId)
            .tryNext()
            .orElse(null)
        != null;
  }

  boolean parentEdgeExists(IContractMember contractMember) {
    Vertex contract =
        g.V()
            .hasLabel("contract")
            .has("contract id", contractMember.contractId())
            .tryNext()
            .orElse(null);
    Vertex parent =
        g.V()
            .hasLabel("contract")
            .has("contract id", contractMember.parentId())
            .tryNext()
            .orElse(null);
    return g.V(parent).outE("parent").where(inV().is(contract)).hasNext();
  }

  boolean childEdgeExists(IContractMember contractMember) {
    Vertex contract =
        g.V()
            .hasLabel("contract")
            .has("contract id", contractMember.contractId())
            .tryNext()
            .orElse(null);
    Vertex parent =
        g.V()
            .hasLabel("contract")
            .has("contract id", contractMember.parentId())
            .tryNext()
            .orElse(null);
    return g.V(contract).outE("child").where(inV().is(parent)).hasNext();
  }

  void clear() {
    this.graph.clear();
  }

  TinkerGraph getGraph() {
    return this.graph;
  }
}
