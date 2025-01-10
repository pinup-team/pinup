package kr.co.pinup.faq;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import kr.co.pinup.faq.exception.FaqCategoryNotFound;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class FaqCategoryConverter implements AttributeConverter<FaqCategory, String> {

    @Override
    public String convertToDatabaseColumn(FaqCategory faqCategory) {
        if (faqCategory == null) {
            throw new FaqCategoryNotFound("NULL로 저장할 수 없습니다.");
        }

        return faqCategory.getName();
    }

    @Override
    public FaqCategory convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            throw new FaqCategoryNotFound("NULL로 저장되어 있습니다.");
        }

        return FaqCategory.ofName(dbData);
    }
}
