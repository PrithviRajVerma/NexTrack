package com.nextrack.issue.domain.repository;

import com.nextrack.issue.domain.entity.BoardCardView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardCardRepository extends JpaRepository<BoardCardView, UUID> {
    List<BoardCardView> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
