package kr.co.pinup.stores.controller;

import kr.co.pinup.storecategories.service.StoreCategoryService;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreThumbnailResponse;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private static final String VIEW_PATH = "views/stores";

    private final StoreService storeService;
    private final StoreCategoryService storeCategoryService;

    @GetMapping
    public String listStores(@RequestParam(required = false) String status, Model model) {
        if (status == null) {
            return "redirect:/stores?status=resolved";
        }

        log.info("StoreController listStores status={}", status);
        StoreStatus selectedStatus = StoreStatus.from(status);
        final List<StoreThumbnailResponse> stores = (selectedStatus != null)
                ? storeService.getStoresByStatus(selectedStatus)
                : storeService.getStoresSortedByStatusPriority();

        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("stores", stores);

        return VIEW_PATH + "/list";
    }

    @GetMapping("/{id}")
    public String storeDetail(@PathVariable Long id, Model model) {
        StoreResponse storeResponse = storeService.getStoreById(id);

        model.addAttribute("store", storeResponse);
        model.addAttribute("location", storeResponse.location());
        model.addAttribute("storeImages", storeResponse.storeImages());

        return VIEW_PATH + "/detail";
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @GetMapping("/create")
    public String createStoreForm(Model model) {
        model.addAttribute("categories", storeCategoryService.getCategories());

        return VIEW_PATH + "/create";
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}/update")
    public String editStoreForm(@PathVariable Long id, Model model) {
        model.addAttribute("store", storeService.getStoreById(id));
        model.addAttribute("categories", storeCategoryService.getCategories());

        return VIEW_PATH + "/update";
    }

}
