Playing with the TinkerPop graph library.
Functionalities include initializing a graph with contract members,
adding new contracts, establishing and modifying parent-child relationships by joining other contracts and/or breaking existing membership,
as well as perform depth calculations within the graph.

A depth limit of 4 has been enforced to prevent cyclic relationships when a contract member attempts to leave one parent and join another that is a close relative. 
Only distant relatives (more that 4 depth) are allowed to maintain such relationships.

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

// Create a new contract
contractsGraph.createContract(newContractMember);

// Join a contract
contractsGraph.joinContract(contractMemberToJoin);

// Calculate parent and child depths
long parentDepth = contractsGraph.countParentDepth(contractId);
long childDepth = contractsGraph.countChildDepth(contractId);

// Print the graph details
contractsGraph.printGraph();
```
