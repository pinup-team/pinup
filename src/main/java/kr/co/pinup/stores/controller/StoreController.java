package kr.co.pinup.stores.controller;

import jakarta.validation.Valid;
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

@Slf4j
@Controller
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public String listStores(Model model) {
        model.addAttribute("stores", storeService.getAllStores());
        return "views/stores/list";
    }

    @GetMapping("/{id}")
    public String storeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("store", storeService.getStoreById(id));
        return "views/stores/detail";
    }


    @GetMapping("/create")
    public String createStoreForm(Model model) {
        return "views/stores/create";
    }

    @PostMapping("/create")
    public String createStore(@Valid @ModelAttribute StoreRequest storeRequest, BindingResult result) {
        if (result.hasErrors()) {
            return "views/stores/create";
        }
        storeService.createStore(storeRequest);
        return "redirect:views//stores/list";
    }

    @GetMapping("/{id}/update")
    public String editStoreForm(@PathVariable Long id, Model model) {
        model.addAttribute("store", storeService.getStoreById(id));
        return "views/stores/update";
    }

    @PostMapping("/{id}/update")
    public String updateStore(@PathVariable Long id, @Valid @ModelAttribute StoreUpdateRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return "views/stores/update";
        }
        storeService.updateStore(id, request);
        return "redirect:views/stores/list";
    }


    @PostMapping("/{id}/delete")
    public String deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return "redirect:views/stores/list";
    }
}
