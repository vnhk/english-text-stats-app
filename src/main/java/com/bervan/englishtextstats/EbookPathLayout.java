package com.bervan.englishtextstats;

import com.bervan.common.AbstractOneValueView;
import com.bervan.common.model.BaseOneValue;
import com.bervan.common.onevalue.OneValueService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EbookPathLayout extends AbstractOneValueView {

    public static final String actualEbookPath = "actualEbookPath";
    private final EbookNotKnownWordsService ebookNotKnownWordsService;

    public EbookPathLayout(OneValueService service, EbookNotKnownWordsService ebookNotKnownWordsService) {
        super(null, actualEbookPath, "Actual Ebook", service);
        this.ebookNotKnownWordsService = ebookNotKnownWordsService;
    }

    @Override
    protected String getTextAreaHeight() {
        return "50px";
    }

    @Override
    protected void save(String value) {
        value = value.trim();
        super.save(value);
        ebookNotKnownWordsService.setActualEbook(value);
    }

    @Override
    protected Optional<BaseOneValue> load(String key) {
        Optional<BaseOneValue> load = super.load(key);
        load.ifPresent(baseOneValue -> ebookNotKnownWordsService.setActualEbook(baseOneValue.getContent()));
        return load;
    }

    public void refreshServiceActualEpub() {
        load(actualEbookPath);
    }
}
