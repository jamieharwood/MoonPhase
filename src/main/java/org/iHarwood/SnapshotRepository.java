package org.iHarwood;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SnapshotRepository extends MongoRepository<SnapshotDocument, String> {

    /** Returns the most recent N snapshots, newest first. */
    List<SnapshotDocument> findAllByOrderByTimestampDesc(Pageable pageable);
}
