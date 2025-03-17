package kr.co.pinup.store_images.model.dto;

import kr.co.pinup.store_images.StoreImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreImageResponse {
    private Long id;
    private Long storeId;
    private String imageUrl;

    public static List<StoreImageResponse> fromList(List<StoreImageResponse> images) {
        return images.stream()
                .map(image -> new StoreImageResponse(image.getId(), image.getStoreId(), image.getImageUrl()))
                .collect(Collectors.toList());
    }
    public static StoreImageResponse from(StoreImage storeImage) {
        return new StoreImageResponse(storeImage.getId(), storeImage.getStore().getId(), storeImage.getImageUrl());
    }

}
