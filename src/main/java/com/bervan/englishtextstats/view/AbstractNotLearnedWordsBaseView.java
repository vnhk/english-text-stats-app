package com.bervan.englishtextstats.view;

import com.bervan.common.AbstractTableView;
import com.bervan.common.BervanButton;
import com.bervan.common.MenuNavigationComponent;
import com.bervan.core.model.BervanLogger;
import com.bervan.englishtextstats.Word;
import com.bervan.englishtextstats.service.WordService;
import com.bervan.languageapp.service.AddAsFlashcardService;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.Comparator;
import java.util.UUID;

public abstract class AbstractNotLearnedWordsBaseView extends AbstractTableView<UUID, Word> {
    protected HorizontalLayout dialogButtonsLayout;
    protected final AddAsFlashcardService addAsFlashcardService;
    protected Button addAsFlashcard;
    protected Button markAsLearned;

    public AbstractNotLearnedWordsBaseView(WordService service, BervanLogger log, MenuNavigationComponent layout, AddAsFlashcardService addAsFlashcardService) {
        super(layout, service, log, Word.class);
        this.addAsFlashcardService = addAsFlashcardService;
    }

    @Override
    protected Grid<Word> getGrid() {
        super.checkboxesColumnsEnabled = false;
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
        dialog.addDetachListener(e -> {
            markAsLearned = null;
            addAsFlashcard = null;
        });

        dialogButtonsLayout = new HorizontalLayout();

        TextArea field = new TextArea(clickedColumn);
        field.setWidth("100%");

        field.setValue(item.getTableFilterableColumnValue());

        markAsLearned = new BervanButton("Mark as learned '['");

        markAsLearned.addClickListener(e -> {
            data.remove(item);
            grid.getDataProvider().refreshAll();
            service.save(item);
            dialog.close();
        });

        dialogLayout.add(headerLayout, field, dialogButtonsLayout);

        addAsFlashcard = new BervanButton("Add as flashcard ']'");
        addAsFlashcard.addClickListener(e -> {
            data.remove(item);
            grid.getDataProvider().refreshAll();
            service.save(item);

            addAsFlashcardService.addAsFlashcardAsync(item);
            dialog.close();
        });

        dialogButtonsLayout.add(markAsLearned);
        dialogButtonsLayout.add(addAsFlashcard);


        getElement().executeJs(
                " document.addEventListener('keydown', function(event) {" +
                        "    if (event.key === '[') {" +
                        "        $0.$server.markAsLearned() " +
                        "    } else if (event.key === ']') {" +
                        "        $0.$server.addAsFlashcard() " +
                        "    } else {return;}" +
                        " }); "
                ,
                getElement()    // $0
        );
    }

    @ClientCallable
    public void markAsLearned() {
        if (markAsLearned != null) {
            markAsLearned.click();
        }
    }

    @ClientCallable
    public void addAsFlashcard() {
        if (addAsFlashcard != null) {
            addAsFlashcard.click();
        }
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