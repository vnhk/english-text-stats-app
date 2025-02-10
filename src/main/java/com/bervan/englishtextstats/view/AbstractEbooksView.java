package com.bervan.englishtextstats.view;

import com.bervan.common.AbstractTableView;
import com.bervan.core.model.BervanLogger;
import com.bervan.englishtextstats.EnglishTextLayout;
import com.bervan.englishtextstats.ExtractedEbookText;
import com.bervan.englishtextstats.service.ExtractedEbookTextService;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.UUID;

public abstract class AbstractEbooksView extends AbstractTableView<UUID, ExtractedEbookText> {
    public static final String ROUTE_NAME = "english-ebook-words/available-ebooks";

    public AbstractEbooksView(ExtractedEbookTextService service, BervanLogger log) {
        super(new EnglishTextLayout(ROUTE_NAME), service, log, ExtractedEbookText.class);
        renderCommonComponents();
        refreshData();
    }
}