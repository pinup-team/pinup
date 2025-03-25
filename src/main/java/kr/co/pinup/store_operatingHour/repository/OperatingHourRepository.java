package kr.co.pinup.store_operatingHour.repository;

import kr.co.pinup.store_operatingHour.OperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatingHourRepository extends JpaRepository<OperatingHour, Long> {
}
