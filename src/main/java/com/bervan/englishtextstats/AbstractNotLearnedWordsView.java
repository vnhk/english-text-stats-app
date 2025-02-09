package com.bervan.englishtextstats;

import com.bervan.common.AbstractTableView;
import com.bervan.common.search.SearchRequest;
import com.bervan.core.model.BervanLogger;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractNotLearnedWordsView extends AbstractTableView<UUID, Word> {
    public static final String ROUTE_NAME = "english-ebook-words/not-learned-yet";
    protected HorizontalLayout dialogButtonsLayout;
    private final EbookPathLayout ebookPathLayout;
    public AbstractNotLearnedWordsView(WordService service, EbookPathLayout ebookPathLayout, BervanLogger log) {
        super(new EnglishTextLayout(ROUTE_NAME), service, log, Word.class);
        this.ebookPathLayout = ebookPathLayout;
        renderCommonComponents();
        add(ebookPathLayout);
        refreshData();

        contentLayout.remove(addButton);
    }

    @Override
    protected void refreshData() {
        ebookPathLayout.refreshServiceActualEpub();
        super.refreshData();
    }

    @Override
    protected Grid<Word> getGrid() {
        Grid<Word> grid = new Grid<>(Word.class, false);
        grid.addColumn(new ComponentRenderer<>(word -> formatTextComponent(word.getTableFilterableColumnValue())))
                .setHeader("Name").setKey("name").setResizable(true);
        grid.addColumn(new ComponentRenderer<>(word -> formatTextComponent(String.valueOf(word.getCount()))))
                .setHeader("Count").setKey("count").setResizable(true)
                .setSortable(true).setComparator(Comparator.comparing(Word::getCount));

        grid.getElement().getStyle().set("--lumo-size-m", 100 + "px");

        removeUnSortedState(grid, 1);

        return grid;
    }

    @Override
    protected void buildOnColumnClickDialogContent(Dialog dialog, VerticalLayout dialogLayout, HorizontalLayout headerLayout, String clickedColumn, Word item) {
        dialogButtonsLayout = new HorizontalLayout();

        TextArea field = new TextArea(clickedColumn);
        field.setWidth("100%");

        field.setValue(item.getTableFilterableColumnValue());

        Button saveButton = new Button("Mark as learned.");
        saveButton.addClassName("option-button");

        saveButton.addClickListener(e -> {
            data.remove(item);
            grid.getDataProvider().refreshAll();
            service.save(item);
            dialog.close();
        });

        dialogButtonsLayout.add(saveButton);

        dialogLayout.add(headerLayout, field, dialogButtonsLayout);
    }

    @Override
    protected long countAll(SearchRequest request, Collection<Word> collect) {
        return collect.size();
    }

    @Override
    protected void newItemButtonClick() {
        throw new RuntimeException("Open dialog is invalid");
    }

    @Override
    protected void buildNewItemDialogContent(Dialog dialog, VerticalLayout dialogLayout, HorizontalLayout headerLayout) {
        throw new RuntimeException("Open dialog is invalid");
    }
}