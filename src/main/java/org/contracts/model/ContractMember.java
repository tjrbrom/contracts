package org.contracts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class ContractMember implements IContractMember {
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
