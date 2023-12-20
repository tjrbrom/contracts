# Contracts Graph

Manage relationships between contracts and graph traversal using TinkerPop graph library. 

It provides functionalities to initialize a graph with contract members, 
add new contracts, establish and modify parent-child relationships, 
and perform depth calculations within the graph.

## Features

- Initialize a graph with contract members
- Add new contracts
- Establish and modify parent-child relationships
- Calculate depth within the graph
- Print graph details
- Check contract and parent existence
- Check parent-child relationship existence
- Clear the graph

## Usage

```
// Example usage:
ContractsGraph contractsGraph = new ContractsGraph();

// Initialize the graph with contract members
contractsGraph.init(contractMembers);

// Add a new contract
contractsGraph.addContract(newContractMember);

// Join a contract
contractsGraph.joinContract(contractMemberToJoin);

// Calculate parent and child depths
long parentDepth = contractsGraph.countParentDepth(contractId);
long childDepth = contractsGraph.countChildDepth(contractId);

// Print the graph details
contractsGraph.printGraph();
```
