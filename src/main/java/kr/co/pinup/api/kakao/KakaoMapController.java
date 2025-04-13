package kr.co.pinup.api.kakao;

import com.sun.jna.WString;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/map")
public class KakaoMapController {

    private final KakaoMapService kakaoMapService;

    @GetMapping("/coord")
    public ResponseEntity<Map<String, String>> getLatLng(@RequestParam String address) {
        Map<String, String> result = kakaoMapService.searchLatLng(address);
        return ResponseEntity.ok(result);
    }
}
