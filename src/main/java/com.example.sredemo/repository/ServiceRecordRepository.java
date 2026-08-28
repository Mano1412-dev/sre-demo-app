package com.example.sredemo.repository;

import com.example.sredemo.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRecordRepository
        extends JpaRepository<ServiceRecord, Long> {
}
