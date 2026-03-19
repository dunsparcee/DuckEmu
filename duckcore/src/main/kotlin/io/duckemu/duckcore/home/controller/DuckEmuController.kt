package io.duckemu.duckcore.home.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DuckEmuController {
    @GetMapping("/")
    fun index(model: Model): String {
        return "index"
    }
}