package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.Option;

public interface OptionRepository extends JpaRepository<Option, Long> {
}