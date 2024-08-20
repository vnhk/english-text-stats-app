package com.bervan.englishtextstats;

import com.bervan.common.AbstractOneValueView;
import com.bervan.common.model.BaseOneValue;
import com.bervan.common.onevalue.OneValueService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EpubPathLayout extends AbstractOneValueView {

    public static final String actualEpubPath = "actualEpubPath";
    private final EpubNotKnownWordsService epubNotKnownWordsService;

    public EpubPathLayout(OneValueService service, EpubNotKnownWordsService epubNotKnownWordsService) {
        super(null, actualEpubPath, "Actual Epub", service);
        this.epubNotKnownWordsService = epubNotKnownWordsService;
    }

    @Override
    protected String getTextAreaHeight() {
        return "50px";
    }

    @Override
    protected void save(String value) {
        value = value.trim();
        super.save(value);
        epubNotKnownWordsService.setActualEpub(value);
    }

    @Override
    protected Optional<BaseOneValue> load(String key) {
        Optional<BaseOneValue> load = super.load(key);
        load.ifPresent(baseOneValue -> epubNotKnownWordsService.setActualEpub(baseOneValue.getContent()));
        return load;
    }

    public void refreshServiceActualEpub() {
        load(actualEpubPath);
    }
}
