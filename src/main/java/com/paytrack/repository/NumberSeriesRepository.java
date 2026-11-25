package com.paytrack.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.paytrack.model.NumberSeries;

import jakarta.persistence.LockModeType;

@Repository
public interface NumberSeriesRepository
        extends JpaRepository<NumberSeries, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select n
           from NumberSeries n
           where n.tenantId = :tenantId
           and n.financialYear = :financialYear
           """)
    Optional<NumberSeries> findForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("financialYear") String financialYear);
}