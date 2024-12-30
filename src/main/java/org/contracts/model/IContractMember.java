package org.contracts.model;

public interface IContractMember {
  String contractId();

  /** contract id of the parent */
  String parentId();
}
