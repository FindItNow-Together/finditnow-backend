package com.finditnow.shopservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    @GetMapping("/{shopId}/inventory")
    public void getInventory(@PathVariable Long shopId) {
    }

}
