package kr.co.pinup.storecategories.service;

import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.exception.StoreCategoryNotFoundException;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreCategoryService {

    private final StoreCategoryRepository storeCategoryRepository;

    public List<StoreCategory> getCategories() {
        return storeCategoryRepository.findAll();
    }

    public StoreCategory getCategory(Long categoryId) {
        return storeCategoryRepository.findById(categoryId)
                .orElseThrow(StoreCategoryNotFoundException::new);
    }
}
