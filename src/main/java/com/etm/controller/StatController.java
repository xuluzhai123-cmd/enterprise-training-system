package com.etm.controller;

import com.etm.common.Result;
import com.etm.service.StatService;
import com.etm.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.ok(statService.dashboard());
    }
}
