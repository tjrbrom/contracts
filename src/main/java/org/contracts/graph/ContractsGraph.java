package org.contracts.graph;

import static org.apache.tinkerpop.gremlin.process.traversal.Scope.local;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

import com.google.common.base.Strings;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.contracts.model.ContractMember;
import org.contracts.model.IContractMember;

@Slf4j
public final class ContractsGraph {
  private static final int DEPTH_LIMIT = 4;
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
    printGraph();
  }

  /**
   * Creates a new contract node that will not have a parent yet.
   *
   * @param contractMember
   */
  public boolean createContract(IContractMember contractMember) {
    String contractId = contractMember.contractId();
    if (Strings.isNullOrEmpty(contractId)) {
      log.error("No contract ID provided");
      return false;
    }
    if (contractExists(contractId)) {
      log.error("Contract already exists");
      return false;
    }
    g.addV("contract").property("data", contractMember).property("contract id", contractId).next();
    return true;
  }

  /** Member will join another contract. They now abandon their current parent for a new one. */
  public boolean joinContract(String contractId, String contractToJoinId) {
    Optional<ContractMember> pcOpt = findContractByContractId(contractId);
    if (pcOpt.isEmpty()) {
      log.error("Contract not found");
      return false;
    }
    if (isImmediateOrDistantChild(contractToJoinId, contractId)
        && calculateChildToParentDepth(contractToJoinId, contractId) <= DEPTH_LIMIT) {
      log.error("Can't turn your child into parent if inside the depth limit");
      return false;
    }
    if (!Strings.isNullOrEmpty(contractId) && !Strings.isNullOrEmpty(contractToJoinId)) {
      // check if these contract and parent exist
      Vertex contract =
          g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);
      Vertex parent =
          g.V().hasLabel("contract").has("contract id", contractToJoinId).tryNext().orElse(null);

      if (contract != null && parent != null) {
        // drop existing "parent" and "child" edges
        g.V(contract).inE("parent").drop().iterate();
        g.V(contract).outE("child").drop().iterate();

        // add new "parent" and "child" edges
        g.V(parent).addE("parent").to(contract).next();
        g.V(contract).addE("child").to(parent).next();

        // update the existing contract vertice
        ((ContractMember) getDataByContractId(contractId)).setParentId(contractToJoinId);
        return true;
      } else {
        log.error("One or both vertices do not exist.");
        return false;
      }
    } else {
      return false;
    }
  }

  /** Member abandons their parent. */
  public boolean leave(String contractId) {
    Optional<ContractMember> pcOpt = findContractByContractId(contractId);
    if (pcOpt.isEmpty()) {
      log.error("Contract not found");
      return false;
    }
    if (!Strings.isNullOrEmpty(contractId)) {
      Vertex contract =
          g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);
      if (contract == null) {
        log.warn("Contract not found");
        return false;
      }
      Optional<IContractMember> parentOpt = findImmediateParent(contractId);
      if (parentOpt.isPresent()) {
        // drop existing "parent" and "child" edges
        g.V(contract).inE("parent").drop().iterate();
        g.V(contract).outE("child").drop().iterate();
        // update the existing contract vertice
        ((ContractMember) getDataByContractId(contractId)).setParentId("");
        return true;
      } else {
        log.warn("No parent found");
      }
    }
    return false;
  }

  /**
   * Kick member (remove it's parent). The member to be kicked must be an immediate child of the
   * kicker.
   *
   * @param kickerContractId id of the kicker, must be the parent
   * @param contractId id of the member to be kicked
   */
  public boolean kick(String kickerContractId, String contractId) {
    Optional<ContractMember> contractOpt = findContractByContractId(contractId);
    Optional<ContractMember> kickerContractOpt = findContractByContractId(kickerContractId);
    if (contractOpt.isEmpty() || kickerContractOpt.isEmpty()) {
      log.error("Contract of you or the other member not found");
      return false;
    }
    ContractMember kickerContract = kickerContractOpt.get();
    if (findImmediateChildren(kickerContract.contractId()).stream()
        .noneMatch(ch -> ch.contractId().equals(contractId))) {
      log.error("Parent can kick only immediate children");
      return false;
    }

    Vertex contract =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);
    g.V(contract).inE("parent").drop().iterate();
    g.V(contract).outE("child").drop().iterate();

    ((ContractMember) getDataByContractId(contractId)).setParentId("");
    return true;
  }

  public Optional<ContractMember> findContractByContractId(String contractId) {
    List<Vertex> list = g.V().hasLabel("contract").toList();
    for (Vertex vertex : list) {
      Object data = vertex.property("data").value();
      if (data instanceof ContractMember pc) {
        if (pc.contractId().equals(contractId)) {
          return Optional.of(pc);
        }
      }
    }
    return Optional.empty();
  }

  public long countMaxParentDepth(String contractId) {
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

  public long countMaxChildDepth(String contractId) {
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

  public Integer calculateChildToParentDepth(String childId, String parentId) {
    Vertex childVertex =
        g.V().hasLabel("contract").has("contract id", childId).tryNext().orElse(null);
    Vertex parentVertex =
        g.V().hasLabel("contract").has("contract id", parentId).tryNext().orElse(null);
    if (childVertex != null && parentVertex != null) {
      if (!isImmediateOrDistantChildIgnoringDepth(childId, parentId)) {
        log.error("Not an immediate or distant child");
        return -1;
      }
      return calculateChildToParentDepth(childVertex, parentVertex, 0);
    } else {
      log.error("One or both vertices do not exist.");
      return -1;
    }
  }

  public void printGraph() {
    this.g.V().valueMap().forEachRemaining(vertice -> System.out.println("Vertice: " + vertice));
    System.out.println("Edges:");
    this.g.E()
        .forEachRemaining(
            edge ->
                System.out.println(
                    "Edge: "
                        + edge.label()
                        + " | "
                        + edge.outVertex().property("contract id").value()
                        + " -> "
                        + edge.inVertex().property("contract id").value()));
  }

  public Optional<IContractMember> findMostDistantParent(String contractId) {
    if (findImmediateParent(contractId).isEmpty()) {
      return Optional.empty();
    }
    Vertex contractVertex =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contractVertex != null) {
      Vertex parentVertex = contractVertex;
      while (true) {
        Vertex nextParentVertex = g.V(parentVertex).in("parent").tryNext().orElse(null);
        if (nextParentVertex == null) {
          return Optional.of((IContractMember) parentVertex.property("data").value());
        }
        parentVertex = nextParentVertex;
      }
    } else {
      log.error("Contract not found");
      return Optional.empty();
    }
  }

  public Optional<IContractMember> findImmediateParent(String contractId) {
    Vertex contractVertex =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contractVertex != null) {
      Vertex parent = g.V(contractVertex).in("parent").tryNext().orElse(null);
      if (parent != null) {
        return Optional.ofNullable((IContractMember) parent.property("data").value());
      } else {
        log.warn("Parent not found for contract {}", contractId);
      }
    } else {
      log.error("Contract not found");
    }
    return Optional.empty();
  }

  /**
   * Finds all children of a contract recursively.
   *
   * @param contractId The ID of the contract.
   * @return A list of all children of the contract.
   */
  public List<IContractMember> findChildren(String contractId) {
    List<IContractMember> children = new ArrayList<>();
    Vertex contractVertex =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contractVertex != null) {
      findChildrenRecursive(contractVertex, children, DEPTH_LIMIT);
    } else {
      log.error("Contract not found");
    }
    return children;
  }

  public List<IContractMember> findChildrenIgnoringDepth(String contractId) {
    List<IContractMember> children = new ArrayList<>();
    Vertex contractVertex =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contractVertex != null) {
      findChildrenRecursiveIgnoringDepth(contractVertex, children, new HashSet<>());
    } else {
      log.error("Contract not found");
    }
    return children;
  }

  public List<IContractMember> findImmediateChildren(String contractId) {
    List<IContractMember> children = new ArrayList<>();
    Vertex contractVertex =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);

    if (contractVertex != null) {
      g.V(contractVertex)
          .out("parent")
          .forEachRemaining(
              childVertex -> {
                IContractMember child = (IContractMember) childVertex.property("data").value();
                children.add(child);
              });
    } else {
      log.error("Contract not found");
    }
    return children;
  }

  public boolean contractExists(String contractId) {
    return this.graph
            .traversal()
            .V()
            .hasLabel("contract")
            .has("contract id", contractId)
            .tryNext()
            .orElse(null)
        != null;
  }

  public void deleteContract(String contractId) {
    Vertex contractVertex =
        g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null);
    if (contractVertex != null) {
      g.V(contractVertex).inE("parent").drop().iterate();
      g.V(contractVertex).outE("child").drop().iterate();
      g.V(contractVertex).drop().iterate();
      log.info("Contract with ID {} deleted successfully", contractId);
    } else {
      log.warn("Contract with ID {} not found", contractId);
    }
  }

  public boolean isImmediateOrDistantChild(String contractId, String parentId) {
    return findChildren(parentId).stream()
        .map(IContractMember::contractId)
        .anyMatch(id -> id.equals(contractId));
  }

  public boolean isImmediateOrDistantChildIgnoringDepth(String contractId, String parentId) {
    return findChildrenIgnoringDepth(parentId).stream()
        .map(IContractMember::contractId)
        .anyMatch(id -> id.equals(contractId));
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

  Object getDataByContractId(String contractId) {
    return Objects.requireNonNull(
            g.V().hasLabel("contract").has("contract id", contractId).tryNext().orElse(null))
        .property("data")
        .value();
  }

  void clear() {
    this.graph.clear();
  }

  TinkerGraph getGraph() {
    return this.graph;
  }

  private Integer calculateChildToParentDepth(Vertex childVertex, Vertex parentVertex, int depth) {
    if (childVertex.equals(parentVertex)) {
      return depth;
    }
    // Traverse all children
    while (g.V(childVertex).out("child").hasNext()) {
      Vertex child = g.V(childVertex).out("child").next();
      int childDepth = calculateChildToParentDepth(child, parentVertex, depth + 1);
      if (childDepth != -1) {
        return childDepth;
      }
    }
    return -1;
  }

  private void findChildrenRecursive(
      Vertex vertex, List<IContractMember> children, int depthLimit) {
    // Check if the depth limit is reached
    if (depthLimit == 0) {
      return;
    }
    g.V(vertex)
        .out("parent")
        .forEachRemaining(
            childVertex -> {
              IContractMember child = (IContractMember) childVertex.property("data").value();
              children.add(child);
              findChildrenRecursive(childVertex, children, depthLimit - 1);
            });
  }

  //    private void findChildrenRecursiveIgnoringDepth(Vertex vertex, List<IContractMember>
  // children) {
  //        g.V(vertex).out("parent").forEachRemaining(childVertex -> {
  //            IContractMember child = (IContractMember) childVertex.property("data").value();
  //            children.add(child);
  //            findChildrenRecursiveIgnoringDepth(childVertex, children);
  //        });
  //    }

  private void findChildrenRecursiveIgnoringDepth(
      Vertex vertex, List<IContractMember> children, Set<Vertex> visited) {
    visited.add(vertex); // Mark the current vertex as visited

    // Visit all children
    g.V(vertex)
        .out("parent")
        .forEachRemaining(
            childVertex -> {
              // Check if the child vertex has already been visited
              if (!visited.contains(childVertex)) {
                // Add the child to the list of children
                IContractMember child = (IContractMember) childVertex.property("data").value();
                children.add(child);

                // Recursively visit the child
                findChildrenRecursiveIgnoringDepth(childVertex, children, visited);
              }
            });
  }
}
