package kr.co.pinup.locations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
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
    private String zonecode;

    @Column(nullable = false)
    private String sido;

    @Column(nullable = false)
    private String sigungu;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "address_detail")
    private String addressDetail;

    public void update(final UpdateLocationRequest request, final KakaoAddressDocument addressDocument) {
        name = addressDocument.addressName();
        zonecode = request.zonecode();
        sido = request.sido();
        sigungu = request.sigungu();
        latitude = addressDocument.latitude();
        longitude = addressDocument.longitude();
        address = request.address();
        addressDetail = request.addressDetail();
    }
}

