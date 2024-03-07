package org.contracts.model;

/**
 * The idea is that each contract member has its own family, and also has a parent contract (which
 * is also a family by itself).
 */
public interface IContractMember {
  String contractId();

  /** contract id of the parent */
  String parentId();
}
