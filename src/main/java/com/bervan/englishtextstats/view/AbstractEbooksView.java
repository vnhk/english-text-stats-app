package com.bervan.englishtextstats.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.core.model.BervanLogger;
import com.bervan.englishtextstats.EnglishTextLayout;
import com.bervan.englishtextstats.ExtractedEbookText;
import com.bervan.englishtextstats.service.ExtractedEbookTextService;

import java.util.UUID;

public abstract class AbstractEbooksView extends AbstractBervanTableView<UUID, ExtractedEbookText> {
    public static final String ROUTE_NAME = "english-ebook-words/available-ebooks";

    public AbstractEbooksView(ExtractedEbookTextService service, BervanLogger log, BervanViewConfig bervanViewConfig) {
        super(new EnglishTextLayout(ROUTE_NAME), service, log, bervanViewConfig, ExtractedEbookText.class);
        renderCommonComponents();
        refreshData();
    }
}