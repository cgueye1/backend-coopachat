package com.example.coopachat.dtos.documentTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentTypeStatsDTO {
    private long total;
    private long required;
    private long optional;
}
