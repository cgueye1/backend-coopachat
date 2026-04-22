package com.example.coopachat.dtos.dashboard.company;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyOrderStatsDTO {
    private String month;
    private int count;
}
