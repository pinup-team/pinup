package kr.co.pinup.stores.controller;

import jakarta.validation.Valid;
import kr.co.pinup.locations.service.LocationService;
import kr.co.pinup.store_categories.service.CategoryService;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final CategoryService categoryService;
    private final LocationService locationService;

    @GetMapping
    public String listStores(Model model) {
        model.addAttribute("stores", storeService.getStoreSummaries());
        return "views/stores/list";
    }

    @GetMapping("/{id}")
    public String storeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("store", storeService.getStoreById(id));
        //todo 화면에서 파라미터 값 넘겨주는 방법 연구 (store 내부 locationId 존재)
        model.addAttribute("location", locationService.getLocationId(5L));
        return "views/stores/detail";
    }

    @GetMapping("/create")
    public String createStoreForm(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "views/stores/create";
    }

    @PostMapping("/create")
    public String createStore(@Valid @ModelAttribute StoreRequest storeRequest) {

        return "redirect:views/stores/list";
    }

    @GetMapping("/{id}/update")
    public String editStoreForm(@PathVariable Long id, Model model) {
        model.addAttribute("store", storeService.getStoreById(id));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "views/stores/update";
    }

    @PostMapping("/{id}/update")
    public String updateStore(@PathVariable Long id,
                              @Valid @ModelAttribute StoreUpdateRequest request,
                              BindingResult result,
                              @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        if (result.hasErrors()) {
            return "views/stores/update";
        }
        storeService.updateStore(id, request, imageFiles);
        return "redirect:views/stores/list";
    }


    @PostMapping("/{id}/delete")
    public String deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return "redirect:views/stores/list";
    }
}
