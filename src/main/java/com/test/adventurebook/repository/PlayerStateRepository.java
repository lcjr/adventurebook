package com.test.adventurebook.repository;

import com.test.adventurebook.model.PlayerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerStateRepository extends JpaRepository<PlayerState, String> {

}
