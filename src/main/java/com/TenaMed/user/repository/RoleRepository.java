package com.TenaMed.user.repository;

import com.TenaMed.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    @Query("select r from Role r where lower(r.name) in :normalizedNames")
    List<Role> findAllActiveByNormalizedNameIn(@Param("normalizedNames") Set<String> normalizedNames);
}
