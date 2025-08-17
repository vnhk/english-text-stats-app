package com.bervan.englishtextstats;

import com.bervan.common.MenuNavigationComponent;
import com.bervan.englishtextstats.view.AbstractEbooksView;
import com.bervan.englishtextstats.view.AbstractNotLearnedWordsView;
import com.vaadin.flow.component.icon.VaadinIcon;

public class EnglishTextLayout extends MenuNavigationComponent {
    public EnglishTextLayout(String routeName) {
        super(routeName);

        addButtonIfVisible(menuButtonsRow, AbstractNotLearnedWordsView.ROUTE_NAME, "Not Learned Words", VaadinIcon.HOME.create());
        addButtonIfVisible(menuButtonsRow, AbstractEbooksView.ROUTE_NAME, "Ebooks", VaadinIcon.HOME.create());

        add(menuButtonsRow);
    }
}
