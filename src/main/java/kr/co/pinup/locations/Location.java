package kr.co.pinup.locations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import kr.co.pinup.BaseEntity;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "locations")
public class Location extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String zoneCode;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "address_detail", nullable = false)
    private String addressDetail;

}

