package org.snomed.snowstorm.core.data.repositories;

import org.snomed.snowstorm.core.data.domain.security.PermissionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

import java.util.Optional;

public interface PermissionRecordRepository extends ElasticsearchCrudRepository<PermissionRecord, String> {

	Page<PermissionRecord> findByPath(String path, Pageable pageable);

	Page<PermissionRecord> findByGlobal(boolean global, Pageable pageable);

	Optional<PermissionRecord> findByGlobalAndPathAndRole(boolean global, String branch, String role);
}
