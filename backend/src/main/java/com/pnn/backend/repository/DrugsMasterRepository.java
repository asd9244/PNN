package com.pnn.backend.repository;

import com.pnn.backend.domain.DrugsMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DrugsMasterRepository extends JpaRepository<DrugsMaster, Long>, DrugsMasterRepositoryCustom {

    Optional<DrugsMaster> findByItemSeq(String itemSeq);

    List<DrugsMaster> findByIdIn(List<Long> ids);
}
