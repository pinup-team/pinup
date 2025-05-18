package kr.co.pinup.store_categories.service;

import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final StoreCategoryRepository storeCategoryRepository;

    public List<StoreCategory> getAllCategories() {
        return storeCategoryRepository.findAll();
    }
}
