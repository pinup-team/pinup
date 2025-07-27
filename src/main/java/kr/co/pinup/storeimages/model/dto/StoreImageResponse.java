package kr.co.pinup.storeimages.model.dto;

import kr.co.pinup.storeimages.StoreImage;
import lombok.Builder;

@Builder
public record StoreImageResponse(Long id, Long storeId, String imageUrl, boolean isThumbnail) {

    public static StoreImageResponse from(StoreImage storeImage) {
        return new StoreImageResponse(
                storeImage.getId(),
                storeImage.getStore().getId(),
                storeImage.getImageUrl(),
                storeImage.isThumbnail()
        );
    }

}
