package com.TenaMed.antidoping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "rxcui_ingredients",
    indexes = {
        @Index(name = "idx_rxcui_ingredients_rxcui", columnList = "rxcui")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class RxcuiIngredient {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "rxcui", nullable = false, length = 50)
    private String rxcui;

    @Column(name = "ingredient_name", nullable = false, length = 255)
    private String ingredientName;
}
