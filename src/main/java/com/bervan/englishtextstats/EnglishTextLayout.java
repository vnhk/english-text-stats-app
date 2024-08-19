package com.bervan.englishtextstats;

import com.bervan.common.AbstractPageLayout;
import com.vaadin.flow.component.html.Hr;

public class EnglishTextLayout extends AbstractPageLayout {
    public EnglishTextLayout(String routeName) {
        super(routeName);

        add(new Hr());
    }
}
