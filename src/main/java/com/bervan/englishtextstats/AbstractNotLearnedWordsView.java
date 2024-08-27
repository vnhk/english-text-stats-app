package com.bervan.englishtextstats;

import com.bervan.common.AbstractTableView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;

public abstract class AbstractNotLearnedWordsView extends AbstractTableView<Word> {
    public static final String ROUTE_NAME = "english-epub-words/not-learned-yet";
    protected HorizontalLayout dialogButtonsLayout;
    private final EpubPathLayout epubPathLayout;
    protected Dialog dialog;
    public AbstractNotLearnedWordsView(WordService service, EpubPathLayout epubPathLayout) {
        super(new EnglishTextLayout(ROUTE_NAME), service, "Not learned words:");
        this.epubPathLayout = epubPathLayout;
        renderCommonComponents();
        add(epubPathLayout);
        refreshData();

        contentLayout.remove(addButton);
    }

    @Override
    protected void refreshData() {
        epubPathLayout.refreshServiceActualEpub();
        super.refreshData();
    }

    @Override
    protected Grid<Word> getGrid() {
        Grid<Word> grid = new Grid<>(Word.class, false);
        grid.addColumn(new ComponentRenderer<>(word -> formatTextComponent(word.getName())))
                .setHeader("Name").setKey("name").setResizable(true).setSortable(true);
        grid.addColumn(new ComponentRenderer<>(word -> formatTextComponent(String.valueOf(word.getCount()))))
                .setHeader("Count").setKey("count").setResizable(true);

        grid.getElement().getStyle().set("--lumo-size-m", 100 + "px");


        return grid;
    }

    @Override
    protected void buildOnColumnClickDialogContent(Dialog dialog, VerticalLayout dialogLayout, HorizontalLayout headerLayout, String clickedColumn, Word item) {
        dialogButtonsLayout = new HorizontalLayout();

        TextArea field = new TextArea(clickedColumn);
        field.setWidth("100%");

        field.setValue(item.getName());

        Button saveButton = new Button("Mark as learned.");
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
    protected void newItemButtonClick() {
        throw new RuntimeException("Open dialog is invalid");
    }

    @Override
    protected void buildNewItemDialogContent(Dialog dialog, VerticalLayout dialogLayout, HorizontalLayout headerLayout) {
        throw new RuntimeException("Open dialog is invalid");
    }
}