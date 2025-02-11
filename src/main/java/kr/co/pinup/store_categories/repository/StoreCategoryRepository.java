package kr.co.pinup.store_categories.repository;

import kr.co.pinup.store_categories.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, Long> {
}
