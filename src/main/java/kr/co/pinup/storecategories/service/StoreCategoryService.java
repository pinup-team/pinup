package kr.co.pinup.storecategories.service;

import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.exception.StoreCategoryNotFoundException;
import kr.co.pinup.storecategories.model.dto.StoreCategoryResponse;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreCategoryService {

    private final StoreCategoryRepository storeCategoryRepository;

    public List<StoreCategoryResponse> getCategories() {
        return storeCategoryRepository.findAll().stream()
                .map(StoreCategoryResponse::from)
                .toList();
    }

    public StoreCategory findCategoryById(Long categoryId) {
        return storeCategoryRepository.findById(categoryId)
                .orElseThrow(StoreCategoryNotFoundException::new);
    }

    public StoreCategoryResponse getCategory(Long categoryId) {
        return storeCategoryRepository.findById(categoryId)
                .map(StoreCategoryResponse::from)
                .orElseThrow(StoreCategoryNotFoundException::new);
    }
}
