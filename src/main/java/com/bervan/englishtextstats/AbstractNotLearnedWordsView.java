package com.bervan.englishtextstats;

import com.bervan.common.AbstractTableView;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractNotLearnedWordsView extends AbstractTableView<Word> {
    public static final String ROUTE_NAME = "english-epub-words/not-learned-yet";


    public AbstractNotLearnedWordsView(@Autowired WordService service) {
        super(new EnglishTextLayout(ROUTE_NAME), service, "Not learned words:");

        add(new Text("Actual ebook: " + service.getActualEpub()));

        remove(addButton);
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
    protected void openEditDialog(ItemClickEvent<Word> event) {
        Dialog dialog = new Dialog();
        dialog.setWidth("80vw");

        VerticalLayout dialogLayout = new VerticalLayout();

        HorizontalLayout headerLayout = getDialogTopBarLayout(dialog);

        String clickedColumn = event.getColumn().getKey();
        TextArea field = new TextArea(clickedColumn);
        field.setWidth("100%");

        Word item = event.getItem();
        field.setValue(item.getName());

        Button saveButton = new Button("Mark as learned.");
        saveButton.addClickListener(e -> {
            data.remove(item);
            grid.getDataProvider().refreshAll();
            service.save(item);
            dialog.close();
        });

        dialogLayout.add(headerLayout, field, saveButton);
        dialog.add(dialogLayout);
        dialog.open();
    }

    @Override
    protected void openAddDialog() {
        throw new RuntimeException("Open dialog is invalid");
    }
}