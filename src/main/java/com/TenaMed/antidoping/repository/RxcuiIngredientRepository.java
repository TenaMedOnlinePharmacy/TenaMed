package com.TenaMed.antidoping.repository;

import com.TenaMed.antidoping.entity.RxcuiIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RxcuiIngredientRepository extends JpaRepository<RxcuiIngredient, UUID> {

    List<RxcuiIngredient> findByRxcui(String rxcui);
}
