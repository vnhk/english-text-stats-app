package com.bervan.englishtextstats.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EbookDto {
    private UUID id;
    private String ebookName;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;
}
