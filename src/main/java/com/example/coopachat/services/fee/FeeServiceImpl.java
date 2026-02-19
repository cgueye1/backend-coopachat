package com.example.coopachat.services.fee;

import com.example.coopachat.entities.Fee;
import com.example.coopachat.repositories.FeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    private final FeeRepository feeRepository;

    @Override
    @Transactional(readOnly = true)
    //on calcule la somme des frais actifs
    public BigDecimal calculateTotalFees() {
        List<Fee> activeFees = feeRepository.findByIsActiveTrue();
        //on parcourt la liste des frais actifs et on calcule la somme des montants
        return activeFees.stream()
                .map(Fee::getAmount)//on récupère le montant de chaque frais
                .filter(amount -> amount != null)//on filtre les montants non nuls
                .reduce(BigDecimal.ZERO, BigDecimal::add);//on calcule la somme des montants
    }
}
