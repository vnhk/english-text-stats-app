package com.bervan.englishtextstats.service;

import com.bervan.englishtextstats.KnownWord;
import com.bervan.history.model.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KnownWordRepository extends BaseRepository<KnownWord, UUID> {
}
