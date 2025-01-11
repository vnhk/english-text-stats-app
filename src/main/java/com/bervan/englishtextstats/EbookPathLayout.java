package com.bervan.englishtextstats;

import com.bervan.common.AbstractOneValueView;
import com.bervan.common.model.BaseOneValue;
import com.bervan.common.onevalue.OneValueService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

public class EbookPathLayout extends AbstractOneValueView {

    public static final String actualEbookPath = "actualEbookPath";
    private final WordService wordService;

    public EbookPathLayout(OneValueService service, WordService wordService) {
        super(null, actualEbookPath, "Actual Ebook", service);
        this.wordService = wordService;
    }

    @Override
    protected String getTextAreaHeight() {
        return "50px";
    }

    @Override
    protected void save(String value) {
        value = value.trim();
        super.save(value);
        wordService.setActualEbookAndUpdatePath(value);
    }

    @Override
    protected Optional<BaseOneValue> load(String key) {
        Optional<BaseOneValue> load = super.load(key);
        load.ifPresent(baseOneValue -> wordService.setActualEbookAndUpdatePath(baseOneValue.getContent()));
        return load;
    }

    public void refreshServiceActualEpub() {
        load(actualEbookPath);
    }
}
