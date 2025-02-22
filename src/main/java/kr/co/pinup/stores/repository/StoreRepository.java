package kr.co.pinup.stores.repository;

import kr.co.pinup.stores.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

}
