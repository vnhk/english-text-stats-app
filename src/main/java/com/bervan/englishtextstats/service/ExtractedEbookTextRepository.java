package com.bervan.englishtextstats.service;

import com.bervan.englishtextstats.ExtractedEbookText;
import com.bervan.history.model.BaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractedEbookTextRepository extends BaseRepository<ExtractedEbookText, UUID> {
    Optional<ExtractedEbookText> findByEbookName(String fileName);

    @Query("SELECT e.id AS id, e.ebookName AS name FROM ExtractedEbookText e " +
            "JOIN e.owners o " +
            "WHERE (e.deleted IS FALSE OR e.deleted IS NULL) " +
            "AND o.id = :ownerId"
    )
    List<EbookSummary> findAllAvailable(UUID ownerId);

    @Modifying
    @Transactional
    @Query("UPDATE ExtractedEbookText e SET e.deleted = true WHERE e.id = :id")
    void softDeleteById(@Param("id") UUID id);

    interface EbookSummary {
        UUID getId();

        String getName();
    }
}
