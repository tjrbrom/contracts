package org.contracts.model;

import java.time.LocalDateTime;
import lombok.*;

@AllArgsConstructor
@Builder
@Data
public class ContractMember implements IContractMember {
    private final String contractId;
    private String parentId;
    @Getter
    private final LocalDateTime createdAt;

    @Override
    public String contractId() {
        return contractId;
    }

    @Override
    public String parentId() {
        return parentId;
    }
}
