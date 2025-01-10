package kr.co.pinup.faq;

import jakarta.validation.Valid;
import kr.co.pinup.faq.request.FaqCreate;
import kr.co.pinup.faq.response.FaqResponse;
import kr.co.pinup.notice.request.FaqUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody @Valid FaqCreate request) {
        faqService.save(request);

        return ResponseEntity.status(CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<FaqResponse>> findAll() {
        return ResponseEntity.ok(faqService.findAll());
    }

    @GetMapping("/{faqId}")
    public ResponseEntity<FaqResponse> find(@PathVariable Long faqId) {
        return ResponseEntity.ok(faqService.find(faqId));
    }

    @PutMapping("/{faqId}")
    public ResponseEntity<Void> update(@PathVariable Long faqId, @RequestBody @Valid FaqUpdate request) {
        faqService.update(faqId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{faqId}")
    public ResponseEntity<Void> delete(@PathVariable Long faqId) {
        faqService.remove(faqId);

        return ResponseEntity.noContent().build();
    }
}
