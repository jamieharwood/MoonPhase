package org.iHarwood;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface SnapshotRepository extends MongoRepository<SnapshotDocument, String> {

    /** Returns the most recent N snapshots, newest first. */
    List<SnapshotDocument> findAllByOrderByTimestampDesc(Pageable pageable);

    /** Returns the number of snapshots within the given time range (used for deduplication). */
    long countByTimestampBetween(Instant start, Instant end);

    /** Deletes all snapshots older than the given instant and returns the count deleted (used for TTL pruning). */
    long deleteByTimestampBefore(Instant before);
}
