package com.bervan.englishtextstats;

import com.bervan.common.MenuNavigationComponent;
import com.bervan.englishtextstats.view.AbstractEbooksView;
import com.bervan.englishtextstats.view.AbstractNotLearnedWordsView;

public class EnglishTextLayout extends MenuNavigationComponent {
    public EnglishTextLayout(String routeName) {
        super(routeName);

        addButtonIfVisible(menuButtonsRow, AbstractNotLearnedWordsView.ROUTE_NAME, "Not Learned Words");
        addButtonIfVisible(menuButtonsRow, AbstractEbooksView.ROUTE_NAME, "Ebooks");

        add(menuButtonsRow);
    }
}
