package com.example.coopachat.dtos.dashboard.company;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeStatusStatsDTO {
    private String label;
    private int value;
    private String color;
}
